package com.csms.enroll.service;

import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.enroll.domain.EnrollmentRuleChecker;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课服务门面：对外提供选课 / 退课 / 预检 / 我的课表 / 名单查询，
 * 并负责并发冲突的有限重试（死锁重试 2 次，间隔带抖动）。
 */
@Service
public class EnrollmentService {

    private static final int MAX_RETRY = 2;

    private final EnrollmentCommandService commandService;
    private final EnrollmentRuleChecker ruleChecker;
    private final JdbcTemplate jdbc;

    public EnrollmentService(EnrollmentCommandService commandService,
                             EnrollmentRuleChecker ruleChecker,
                             JdbcTemplate jdbc) {
        this.commandService = commandService;
        this.ruleChecker = ruleChecker;
        this.jdbc = jdbc;
    }

    /** E-01 选课（带死锁重试） */
    public Map<String, Object> enroll(Long studentId, Long teachingClassId) {
        return enroll(studentId, teachingClassId, "PORTAL");
    }

    public Map<String, Object> adminEnroll(Long studentId, Long teachingClassId) {
        return enroll(studentId, teachingClassId, "ADMIN");
    }

    private Map<String, Object> enroll(Long studentId, Long teachingClassId, String source) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                return commandService.doEnroll(studentId, teachingClassId, source);
            } catch (DuplicateKeyException e) {
                // 唯一键兜底：并发下同一学生重复提交
                throw new BizException(ErrorCode.ENROLL_DUPLICATE);
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                last = e;
                sleepBackoff(attempt);
            }
        }
        throw new BizException(ErrorCode.ENROLL_SYSTEM_BUSY,
                last == null ? "选课人数众多，请稍后重试" : "选课人数众多，请稍后重试");
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(attempt == 0 ? 50L : 200L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** E-02 退课 */
    public void withdraw(Long studentId, Long teachingClassId) {
        commandService.doWithdraw(studentId, teachingClassId);
    }

    /** E-03 我的选课 */
    public List<Map<String,Object>> myEnrollments(Long studentId,Long termId) { return myEnrollments(studentId,termId,null,null,null); }
    public List<Map<String, Object>> myEnrollments(Long studentId, Long termId,String status,Long courseId,String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id                        AS id,
                       e.student_id                AS studentId,
                       e.teaching_class_id         AS teachingClassId,
                       tc.class_name               AS className,
                       e.course_id                 AS courseId,
                       c.course_code               AS courseCode,
                       c.name                      AS courseName,
                       e.term_id                   AS termId,
                       t.name                      AS termName,
                       e.credit                    AS credit,
                       e.status                    AS status,
                       DATE_FORMAT(e.enrolled_at, '%Y-%m-%d %H:%i:%s')   AS enrolledAt,
                       DATE_FORMAT(e.withdrawn_at, '%Y-%m-%d %H:%i:%s')  AS withdrawnAt,
                       e.source                    AS source
                  FROM enrollment e
                  JOIN teaching_class tc ON tc.id = e.teaching_class_id
                  JOIN course c          ON c.id = e.course_id
                  LEFT JOIN term t       ON t.id = e.term_id
                 WHERE e.student_id = ? AND e.deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(studentId);
        if (termId != null) {
            sql.append(" AND e.term_id = ?");
            args.add(termId);
        }
        if(status==null || status.isBlank()) sql.append(" AND e.status IN ('ENROLLED','COMPLETED')");
        else {
            if(!List.of("ENROLLED","COMPLETED","WITHDRAWN").contains(status)) throw new BizException(ErrorCode.PARAM_ERROR,"选课状态无效");
            sql.append(" AND e.status=?");args.add(status);
        }
        if(courseId!=null) { sql.append(" AND e.course_id=?");args.add(courseId); }
        if(keyword!=null&&!keyword.isBlank()) { sql.append(" AND (c.name LIKE ? OR c.course_code LIKE ? OR tc.class_name LIKE ?)");for(int i=0;i<3;i++) args.add("%"+keyword.trim()+"%"); }
        sql.append(" ORDER BY e.enrolled_at DESC,e.id DESC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        rows.forEach(this::attachSchedules);
        return rows;
    }

    /** E-04 我的课表（含时间段，供周视图与冲突判色） */
    public List<Map<String, Object>> mySchedule(Long studentId, Long termId) {
        StringBuilder sql = new StringBuilder("""
                SELECT tc.id              AS id,
                       tc.id                     AS teachingClassId,
                       e.id                      AS enrollmentId,
                       tc.course_id       AS courseId,
                       c.course_code      AS courseCode,
                       c.name             AS courseName,
                       tc.term_id         AS termId,
                       tc.teacher_id      AS teacherId,
                       u.real_name        AS teacherName,
                       tc.class_name      AS className,
                       tc.capacity        AS capacity,
                       tc.enrolled_count  AS enrolledCount,
                       tc.credit          AS credit,
                       tc.location        AS location,
                       tc.start_week      AS startWeek,
                       tc.end_week        AS endWeek,
                       tc.status          AS status,
                       e.status           AS enrollmentStatus
                  FROM enrollment e
                  JOIN teaching_class tc ON tc.id = e.teaching_class_id
                  JOIN course c          ON c.id = tc.course_id
                  LEFT JOIN sys_user u   ON u.id = tc.teacher_id
                 WHERE e.student_id = ? AND e.status IN ('ENROLLED','COMPLETED') AND e.deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        args.add(studentId);
        if (termId != null) {
            sql.append(" AND e.term_id = ?");
            args.add(termId);
        }
        sql.append(" ORDER BY tc.id");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        rows.forEach(this::attachSchedules);
        return rows;
    }

    /** E-05 选课预检：只读校验，不占名额 */
    public Map<String, Object> precheck(Long studentId, Long teachingClassId) {
        try {
            commandService.validateEnrollment(studentId,teachingClassId);
            return Map.of("canEnroll",true,"reasonCode",0,"reason","可以选课","conflicts",List.of(),"eligible",true,"reasons",List.of());
        } catch(BizException e) {
            return Map.of("canEnroll",false,"reasonCode",e.getErrorCode().getCode(),"reason",e.getMessage(),
                "conflicts",e.getErrorCode()==ErrorCode.ENROLL_TIME_CONFLICT?ruleChecker.conflicts(studentId,teachingClassId):List.of(),"eligible",false,"reasons",List.of(e.getMessage()));
        }
    }

    public Map<String,Object> withdrawRelation(Long studentId,Long enrollmentId) {
        var rows=jdbc.queryForList("SELECT student_id,teaching_class_id,status FROM enrollment WHERE id=? AND deleted=0",enrollmentId);
        if(rows.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND,"选课记录不存在");
        if(((Number)rows.get(0).get("student_id")).longValue()!=studentId) throw new BizException(ErrorCode.FORBIDDEN,"只能退选本人的课程");
        Long classId=((Number)rows.get(0).get("teaching_class_id")).longValue();
        withdraw(studentId,classId);
        var result=seatSnapshot(classId);result.put("alreadyWithdrawn","WITHDRAWN".equals(rows.get(0).get("status")));return result;
    }

    public Map<String,Object> seatSnapshot(Long classId) {
        return jdbc.queryForMap("SELECT id,id AS teachingClassId,enrolled_count AS enrolledCount,capacity,capacity-enrolled_count AS remaining FROM teaching_class WHERE id=?",classId);
    }
    public Map<String,Object> standardResult(Map<String,Object> enrollment) {
        return Map.of("enrollment",enrollment,"teachingClass",seatSnapshot(((Number)enrollment.get("teachingClassId")).longValue()));
    }

    /** E-06 教学班选课名单（教师 / 管理员） */
    public void assertCanReadClass(Long teachingClassId, Long userId, boolean isAdmin) {
        var rows = jdbc.queryForList("SELECT teacher_id FROM teaching_class WHERE id=? AND deleted=0", teachingClassId);
        if (rows.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND, "教学班不存在");
        if (!isAdmin && ((Number) rows.get(0).get("teacher_id")).longValue() != userId) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查看本人教学班的学生名单");
        }
    }

    public List<Map<String, Object>> classStudents(Long teachingClassId) {
        var rows = jdbc.queryForList("""
                SELECT e.id                        AS id,
                       e.student_id                AS studentId,
                       u.username                  AS studentUsername,
                       u.real_name                 AS studentName,
                       u.major_id                  AS majorId,
                       m.name                      AS majorName,
                       e.status                    AS enrollStatus,
                       e.teaching_class_id         AS teachingClassId,
                       tc.class_name               AS className,
                       e.course_id                 AS courseId,
                       c.course_code               AS courseCode,
                       c.name                      AS courseName,
                       e.term_id                   AS termId,
                       e.credit                    AS credit,
                       e.status                    AS status,
                       DATE_FORMAT(e.enrolled_at, '%Y-%m-%d %H:%i:%s')  AS enrolledAt,
                       e.source                    AS source
                  FROM enrollment e
                  JOIN sys_user u        ON u.id = e.student_id
                  LEFT JOIN major m ON m.id=u.major_id
                  JOIN teaching_class tc ON tc.id = e.teaching_class_id
                  JOIN course c          ON c.id = e.course_id
                 WHERE e.teaching_class_id = ? AND e.status IN ('ENROLLED', 'COMPLETED') AND e.deleted = 0
                 ORDER BY u.username
                """, teachingClassId);
        rows.forEach(row->row.put("hasConflict",!ruleChecker.conflicts(((Number)row.get("studentId")).longValue(),teachingClassId).isEmpty()));
        return rows;
    }

    private void attachSchedules(Map<String, Object> row) {
        Object classId = row.get("teachingClassId");
        if (classId == null) {
            classId = row.get("id");
        }
        if (classId == null) {
            return;
        }
        row.put("schedules", jdbc.queryForList("""
                SELECT id              AS id,
                       teaching_class_id AS teachingClassId,
                       day_of_week     AS dayOfWeek,
                       start_section   AS startSection,
                       end_section     AS endSection,
                       start_minute    AS startMinute,
                       end_minute      AS endMinute,
                       location        AS location,
                       week_desc       AS weekDesc,
                       start_week      AS startWeek,
                       end_week        AS endWeek
                  FROM class_schedule
                 WHERE teaching_class_id = ?
                 ORDER BY day_of_week, start_section
                """, ((Number) classId).longValue()));
    }
}
