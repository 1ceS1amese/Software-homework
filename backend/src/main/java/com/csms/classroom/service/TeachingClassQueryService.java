package com.csms.classroom.service;

import com.csms.common.api.PageResult;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 教学班只读查询（C-01 / C-02）。
 *
 * <p>列表需要联表取课程名、教师名与学期名，故直接使用 JdbcTemplate 编写显式 SQL
 * （对应 {@code docs/01} ADR-03：聚合与联表场景走手写 SQL）。
 */
@Service
public class TeachingClassQueryService {

    private final JdbcTemplate jdbc;
    private static final String CONFLICT_SQL="""
        SELECT 1 FROM class_schedule a JOIN class_schedule b ON a.day_of_week=b.day_of_week
        AND a.start_minute<b.end_minute AND b.start_minute<a.end_minute AND a.start_week<=b.end_week AND b.start_week<=a.end_week
        JOIN enrollment e ON e.teaching_class_id=b.teaching_class_id AND e.student_id=? AND e.deleted=0 AND e.status IN ('ENROLLED','COMPLETED')
        WHERE a.teaching_class_id=tc.id AND b.teaching_class_id<>tc.id AND e.term_id=tc.term_id
        """;

    public TeachingClassQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String BASE_SELECT = """
            SELECT tc.id                     AS id,
                   tc.course_id              AS courseId,
                   c.course_code             AS courseCode,
                   c.name                    AS courseName,
                   tc.term_id                AS termId,
                   t.name                    AS termName,
                   tc.teacher_id             AS teacherId,
                   u.real_name               AS teacherName,
                   tc.class_name             AS className,
                   tc.capacity               AS capacity,
                   tc.enrolled_count         AS enrolledCount,
                   tc.credit                 AS credit,
                   tc.location               AS location,
                   tc.start_week             AS startWeek,
                   tc.end_week               AS endWeek,
                   DATE_FORMAT(tc.open_class_date, '%Y-%m-%d') AS openClassDate,
                   tc.status                 AS status,
                   tc.description            AS description,
                   (SELECT COUNT(*) FROM grade g WHERE g.teaching_class_id=tc.id AND g.deleted=0 AND g.status='PUBLISHED') AS publishedGradeCount,
                   CASE WHEN EXISTS(SELECT 1 FROM grade g WHERE g.teaching_class_id=tc.id AND g.deleted=0 AND g.status='PUBLISHED') THEN 'PUBLISHED'
                        WHEN EXISTS(SELECT 1 FROM grade g WHERE g.teaching_class_id=tc.id AND g.deleted=0) THEN 'DRAFT' ELSE 'EMPTY' END AS gradeStatus
              FROM teaching_class tc
              JOIN course c        ON c.id = tc.course_id
              LEFT JOIN term t     ON t.id = tc.term_id
              LEFT JOIN sys_user u ON u.id = tc.teacher_id
            """;

    /** C-01 教学班列表（分页 + 筛选） */
    public PageResult<Map<String, Object>> query(Long termId, Long courseId, Long teacherId,
                                                 String keyword, String status, Boolean onlyAvailable, Long deptId, Boolean hasConflict, String sortBy, String sortOrder,
                                                 Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);

        StringBuilder where = new StringBuilder(" WHERE tc.deleted = 0 ");
        List<Object> args = new ArrayList<>();

        Long effectiveTermId = termId != null ? termId : activeTermId();
        if (effectiveTermId != null) {
            where.append(" AND tc.term_id = ? ");
            args.add(effectiveTermId);
        }
        if (courseId != null) {
            where.append(" AND tc.course_id = ? ");
            args.add(courseId);
        }
        if (teacherId != null) {
            where.append(" AND tc.teacher_id = ? ");
            args.add(teacherId);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND tc.status = ? ");
            args.add(status);
        }
        if (Boolean.TRUE.equals(onlyAvailable)) {
            where.append(" AND tc.enrolled_count < tc.capacity ");
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (c.name LIKE ? OR c.course_code LIKE ? OR tc.class_name LIKE ? OR u.real_name LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        if(deptId!=null) { where.append(" AND c.dept_id=?");args.add(deptId); }
        if(hasConflict!=null) {
            var me=com.csms.common.service.CommandSupport.require("STUDENT");
            where.append(hasConflict?" AND EXISTS ":" AND NOT EXISTS ").append("("+CONFLICT_SQL+")");args.add(me.getId());
        }
        String order=com.csms.common.api.QuerySort.order(sortBy,sortOrder,Map.of("id","tc.id","enrolled_count","tc.enrolled_count","enrolledCount","tc.enrolled_count","start_time","(SELECT MIN(s.start_minute) FROM class_schedule s WHERE s.teaching_class_id=tc.id)","className","tc.class_name"),"tc.id");
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM teaching_class tc "
                        + "JOIN course c ON c.id = tc.course_id "
                        + "LEFT JOIN sys_user u ON u.id = tc.teacher_id "
                        + where,
                Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((p - 1L) * s);

        List<Map<String, Object>> rows = jdbc.queryForList(
                BASE_SELECT + where + " ORDER BY " + order + ",tc.id LIMIT ? OFFSET ?",
                pageArgs.toArray());
        rows.forEach(this::attachSchedules);

        return new PageResult<>(rows, total == null ? 0 : total, p, s);
    }

    /** C-02 教学班详情 */
    public Map<String, Object> detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(BASE_SELECT + " WHERE tc.id = ? AND tc.deleted = 0", id);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "教学班不存在");
        }
        Map<String, Object> row = rows.get(0);
        attachSchedules(row);
        return row;
    }

    /** 教师本人的教学班（C-12） */
    public List<Map<String, Object>> byTeacher(Long teacherId, Long termId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE tc.deleted = 0 AND tc.teacher_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(teacherId);
        if (termId != null) {
            sql.append(" AND tc.term_id = ? ");
            args.add(termId);
        }
        sql.append(" ORDER BY tc.id");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        rows.forEach(this::attachSchedules);
        return rows;
    }

    /** 当前选课中的学期；没有则取最新学期 */
    public Long activeTermId() {
        List<Long> ids = jdbc.query("""
                SELECT id FROM term WHERE deleted = 0
                 ORDER BY (status = 'ENROLLING') DESC, start_date DESC
                 LIMIT 1
                """, (rs, rowNum) -> rs.getLong(1));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void attachSchedules(Map<String, Object> row) {
        Object id = row.get("id");
        if (id == null) {
            return;
        }
        row.put("schedules", jdbc.queryForList("""
                SELECT id               AS id,
                       teaching_class_id AS teachingClassId,
                       day_of_week      AS dayOfWeek,
                       start_section    AS startSection,
                       end_section      AS endSection,
                       start_minute     AS startMinute,
                       end_minute       AS endMinute,
                       location         AS location,
                       week_desc        AS weekDesc,
                       start_week       AS startWeek,
                       end_week         AS endWeek
                  FROM class_schedule
                 WHERE teaching_class_id = ?
                 ORDER BY day_of_week, start_section
                """, ((Number) id).longValue()));
    }
}
