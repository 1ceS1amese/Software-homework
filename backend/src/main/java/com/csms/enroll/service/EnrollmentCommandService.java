package com.csms.enroll.service;

import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.enroll.domain.CapacityGuard;
import com.csms.enroll.domain.EnrollmentRuleChecker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 选课事务核心（M3 纵向切片的主链路）。
 *
 * <p>选课顺序严格遵循 {@code docs/05 §5.1}：
 * 状态 → 选课窗口 → 账户 → 重复 → 时间冲突 → 先修课 → 学分上限 → <b>原子占位</b> → 落库。
 * 前 7 步用于给出可读的拒绝原因，第 8 步是并发安全的唯一兜底。
 */
@Service
public class EnrollmentCommandService {

    private final EnrollmentRuleChecker ruleChecker;
    private final CapacityGuard capacityGuard;
    private final JdbcTemplate jdbc;

    public EnrollmentCommandService(EnrollmentRuleChecker ruleChecker,
                                    CapacityGuard capacityGuard,
                                    JdbcTemplate jdbc) {
        this.ruleChecker = ruleChecker;
        this.capacityGuard = capacityGuard;
        this.jdbc = jdbc;
    }

    /** 执行选课，返回落库后的选课记录视图 */
    @Transactional
    public Map<String, Object> doEnroll(Long studentId, Long teachingClassId) {
        Map<String, Object> tc = loadTeachingClass(teachingClassId);

        String status = (String) tc.get("status");
        if (!"PUBLISHED".equals(status)) {
            throw new BizException(ErrorCode.ENROLL_NOT_OPEN);
        }

        Long termId = ((Number) tc.get("term_id")).longValue();
        Long courseId = ((Number) tc.get("course_id")).longValue();
        BigDecimal credit = (BigDecimal) tc.get("credit");

        checkTermWindow(termId);

        ruleChecker.checkDuplicate(studentId, teachingClassId);
        ruleChecker.checkTimeConflict(studentId, teachingClassId);
        ruleChecker.checkPrerequisite(studentId, courseId);
        ruleChecker.checkCreditMax(studentId, termId, credit);

        // —— 并发安全兜底：单条原子 UPDATE，受影响行数为 0 表示名额已被抢完 ——
        if (!capacityGuard.tryOccupy(teachingClassId, studentId)) {
            throw new BizException(ErrorCode.ENROLL_FULL);
        }

        // 退课后重选：命中 WITHDRAWN 行则复活，否则新增（BR-10）
        int revived = jdbc.update("""
                UPDATE enrollment
                   SET status = 'ENROLLED', enrolled_at = NOW(), withdrawn_at = NULL, source = 'PORTAL'
                 WHERE student_id = ? AND teaching_class_id = ? AND status = 'WITHDRAWN'
                """, studentId, teachingClassId);

        if (revived == 0) {
            jdbc.update("""
                    INSERT INTO enrollment
                        (student_id, teaching_class_id, course_id, term_id, credit, status, enrolled_at, source)
                    VALUES (?, ?, ?, ?, ?, 'ENROLLED', NOW(), 'PORTAL')
                    """, studentId, teachingClassId, courseId, termId, credit);
        }

        return loadEnrollment(studentId, teachingClassId);
    }

    /** 执行退课（幂等） */
    @Transactional
    public void doWithdraw(Long studentId, Long teachingClassId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, status, credit, term_id FROM enrollment
                 WHERE student_id = ? AND teaching_class_id = ? AND deleted = 0
                 FOR UPDATE
                """, studentId, teachingClassId);

        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "未找到对应的选课记录");
        }

        Map<String, Object> row = rows.get(0);
        String status = (String) row.get("status");

        if ("WITHDRAWN".equals(status)) {
            return; // 幂等：已退课不重复扣减名额
        }
        if ("COMPLETED".equals(status)) {
            throw new BizException(ErrorCode.GRADE_PUBLISHED, "该课程成绩已发布，不能退课");
        }

        Map<String, Object> tc = loadTeachingClass(teachingClassId);
        if ("CANCELLED".equals(tc.get("status"))) {
            throw new BizException(ErrorCode.TC_STATUS_INVALID, "该教学班已取消，无需退课");
        }

        Long termId = ((Number) row.get("term_id")).longValue();
        BigDecimal credit = (BigDecimal) row.get("credit");

        checkWithdrawWindow(termId, tc);
        ruleChecker.checkCreditMin(studentId, termId, credit);

        jdbc.update("""
                UPDATE enrollment SET status = 'WITHDRAWN', withdrawn_at = NOW() WHERE id = ?
                """, row.get("id"));

        jdbc.update("""
                UPDATE teaching_class SET enrolled_count = GREATEST(enrolled_count - 1, 0) WHERE id = ?
                """, teachingClassId);
    }

    // —————————————————— 内部辅助 ——————————————————

    private Map<String, Object> loadTeachingClass(Long teachingClassId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, course_id, term_id, teacher_id, class_name, capacity, enrolled_count,
                       credit, status, open_class_date, start_week, end_week,
                       DATE_FORMAT(open_class_date, '%Y-%m-%d') AS open_class_date_str
                  FROM teaching_class
                 WHERE id = ? AND deleted = 0
                """, teachingClassId);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "教学班不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> loadTerm(Long termId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, code, name, enroll_start, enroll_end, withdraw_end, status
                  FROM term WHERE id = ? AND deleted = 0
                """, termId);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "学期不存在");
        }
        return rows.get(0);
    }

    /** BR-02：必须在学期选课窗口内 */
    private void checkTermWindow(Long termId) {
        Map<String, Object> term = loadTerm(termId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = toLocalDateTime(term.get("enroll_start"));
        LocalDateTime end = toLocalDateTime(term.get("enroll_end"));

        if (start != null && now.isBefore(start)) {
            throw new BizException(ErrorCode.ENROLL_OUT_OF_TIME, "选课尚未开始");
        }
        if (end != null && now.isAfter(end)) {
            throw new BizException(ErrorCode.ENROLL_OUT_OF_TIME, "选课已结束");
        }
    }

    /** BR-08：退课需在截止期前且教学班未开课 */
    private void checkWithdrawWindow(Long termId, Map<String, Object> tc) {
        Map<String, Object> term = loadTerm(termId);
        LocalDate today = LocalDate.now();

        Object withdrawEnd = term.get("withdraw_end");
        if (withdrawEnd == null) {
            withdrawEnd = term.get("enroll_end");
        }
        LocalDateTime end = toLocalDateTime(withdrawEnd);
        if (end != null && LocalDateTime.now().isAfter(end)) {
            throw new BizException(ErrorCode.ENROLL_WITHDRAW_DEADLINE);
        }

        Object openDate = tc.get("open_class_date");
        if (openDate instanceof Date d && !today.isBefore(d.toLocalDate())) {
            throw new BizException(ErrorCode.ENROLL_ALREADY_STARTED);
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof Date d) {
            return d.toLocalDate().atStartOfDay();
        }
        if (value instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault());
        }
        return null;
    }

    /** 查询单条选课记录（前端 EnrollmentItem 契约） */
    public Map<String, Object> loadEnrollment(Long studentId, Long teachingClassId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
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
                       DATE_FORMAT(e.enrolled_at, '%Y-%m-%d %H:%i:%s')  AS enrolledAt,
                       DATE_FORMAT(e.withdrawn_at, '%Y-%m-%d %H:%i:%s') AS withdrawnAt,
                       e.source                    AS source
                  FROM enrollment e
                  JOIN teaching_class tc ON tc.id = e.teaching_class_id
                  JOIN course c          ON c.id = e.course_id
                  LEFT JOIN term t       ON t.id = e.term_id
                 WHERE e.student_id = ? AND e.teaching_class_id = ? AND e.deleted = 0
                """, studentId, teachingClassId);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
