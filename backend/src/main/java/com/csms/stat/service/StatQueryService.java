package com.csms.stat.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计查询（S-01 / S-02 / S-04 / S-06）。
 *
 * <p>全部数据实时从库中聚合，不含任何硬编码样例值（FR-STAT-05）。
 * 空库时返回 0 而非假数据。
 */
@Service
public class StatQueryService {

    private final JdbcTemplate jdbc;

    public StatQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** S-01 选课总览 */
    public Map<String, Object> overview(Long termId) {
        String termFilter = termId != null ? " AND term_id = " + termId : "";

        Map<String, Object> tc = jdbc.queryForMap("""
                SELECT COUNT(*)                        AS classCount,
                       COALESCE(SUM(capacity), 0)      AS totalCapacity,
                       COALESCE(SUM(enrolled_count), 0) AS totalEnrollments
                  FROM teaching_class WHERE deleted = 0
                """ + termFilter);

        long classCount = ((Number) tc.get("classCount")).longValue();
        long totalCapacity = ((Number) tc.get("totalCapacity")).longValue();
        long totalEnrollments = ((Number) tc.get("totalEnrollments")).longValue();

        Long studentCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE user_type = 'STUDENT' AND deleted = 0", Long.class);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("termId", termId);
        result.put("classCount", classCount);
        result.put("totalCapacity", totalCapacity);
        result.put("totalEnrollments", totalEnrollments);
        result.put("studentCount", studentCount == null ? 0 : studentCount);
        result.put("avgFillRate", rate(totalEnrollments, totalCapacity));
        return result;
    }

    /** S-02 按课程聚合 */
    public List<Map<String, Object>> byCourse(Long termId, Integer limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id                    AS courseId,
                       c.course_code           AS courseCode,
                       c.name                  AS courseName,
                       COALESCE(SUM(tc.capacity), 0)       AS capacity,
                       COALESCE(SUM(tc.enrolled_count), 0) AS enrolledCount
                  FROM teaching_class tc
                  JOIN course c ON c.id = tc.course_id
                 WHERE tc.deleted = 0
                """);
        if (termId != null) {
            sql.append(" AND tc.term_id = ").append(termId);
        }
        sql.append(" GROUP BY c.id, c.course_code, c.name ORDER BY enrolledCount DESC LIMIT ")
                .append(limit == null || limit < 1 ? 10 : Math.min(limit, 100));

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString());
        for (Map<String, Object> row : rows) {
            long capacity = ((Number) row.get("capacity")).longValue();
            long enrolled = ((Number) row.get("enrolledCount")).longValue();
            row.put("fillRate", rate(enrolled, capacity));
        }
        return rows;
    }

    /** S-04 满员度分析 */
    public Map<String, Object> capacityAnalysis(Long termId) {
        StringBuilder where = new StringBuilder(" WHERE tc.deleted = 0 ");
        if (termId != null) {
            where.append(" AND tc.term_id = ").append(termId);
        }
        String select = """
                SELECT tc.id AS id, tc.class_name AS className,
                       c.name AS courseName, u.real_name AS teacherName,
                       tc.capacity AS capacity, tc.enrolled_count AS enrolledCount
                  FROM teaching_class tc
                  JOIN course c        ON c.id = tc.course_id
                  LEFT JOIN sys_user u ON u.id = tc.teacher_id
                """;

        List<Map<String, Object>> full = jdbc.queryForList(
                select + where + " AND tc.enrolled_count >= tc.capacity ORDER BY tc.id");
        List<Map<String, Object>> idle = jdbc.queryForList(
                select + where + " AND tc.capacity > 0 AND tc.enrolled_count < tc.capacity * 0.3 ORDER BY tc.id");
        List<Map<String, Object>> normal = jdbc.queryForList(
                select + where + " AND tc.capacity > 0 AND tc.enrolled_count >= tc.capacity * 0.3 "
                        + " AND tc.enrolled_count < tc.capacity ORDER BY tc.id");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fullClasses", full);
        result.put("idleClasses", idle);
        result.put("normalClasses", normal);
        return result;
    }

    /** S-06 学生学分汇总 */
    public Map<String, Object> studentSummary(Long studentId, Long termId) {
        String termFilter = termId != null ? " AND term_id = " + termId : "";

        BigDecimal enrolledCredits = jdbc.queryForObject("""
                SELECT COALESCE(SUM(credit), 0) FROM enrollment
                 WHERE student_id = ? AND status = 'ENROLLED' AND deleted = 0
                """ + termFilter, BigDecimal.class, studentId);

        Map<String, Object> earned = jdbc.queryForMap("""
                SELECT COALESCE(SUM(e.credit), 0) AS earnedCredits,
                       COUNT(*)                   AS passCount
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED' AND g.is_pass = 1
                """ + (termId != null ? " AND e.term_id = " + termId : ""), studentId);

        Map<String, Object> allCompleted = jdbc.queryForMap("""
                SELECT COUNT(*) AS courseCount,
                       COALESCE(SUM(CASE WHEN g.is_pass = 0 THEN 1 ELSE 0 END), 0) AS failCount
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED'
                """ + (termId != null ? " AND e.term_id = " + termId : ""), studentId);

        BigDecimal gpa = jdbc.queryForObject("""
                SELECT COALESCE(SUM(g.grade_point * e.credit) / NULLIF(SUM(e.credit), 0), 0) AS gpa
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED'
                """ + (termId != null ? " AND e.term_id = " + termId : ""), BigDecimal.class, studentId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        result.put("termId", termId);
        result.put("totalEnrolledCredits", scale(enrolledCredits));
        result.put("earnedCredits", scale((BigDecimal) earned.get("earnedCredits")));
        result.put("passCount", ((Number) earned.get("passCount")).longValue());
        result.put("courseCount", ((Number) allCompleted.get("courseCount")).longValue());
        result.put("failCount", ((Number) allCompleted.get("failCount")).longValue());
        result.put("gpa", gpa == null ? BigDecimal.ZERO : gpa.setScale(2, RoundingMode.HALF_UP));
        return result;
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0d;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(1, RoundingMode.HALF_UP);
    }
}
