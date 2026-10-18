package com.csms.enroll.domain;

import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 选课规则校验器（BR-04 / BR-05 / BR-06 / 学分上下限）。
 *
 * <p>设计为无状态组件，仅依赖只读查询，便于单独做单元测试。
 * 所有校验只负责"给出可读的拒绝原因"，并发安全的兜底在 {@link CapacityGuard}。
 */
@Component
public class EnrollmentRuleChecker {

    private static final String[] WEEK_NAMES = {"一", "二", "三", "四", "五", "六", "日"};

    private final JdbcTemplate jdbc;

    public EnrollmentRuleChecker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** BR-04：同一学生同一教学班只能有一条有效选课关系 */
    public void checkDuplicate(Long studentId, Long teachingClassId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM enrollment
                 WHERE student_id = ? AND teaching_class_id = ?
                   AND status IN ('ENROLLED', 'COMPLETED') AND deleted = 0
                """, Integer.class, studentId, teachingClassId);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.ENROLL_DUPLICATE);
        }
    }

    /** BR-05：同一学生不可同时选上课时间重叠的两个教学班（同时考虑周次区间） */
    public void checkTimeConflict(Long studentId, Long teachingClassId) {
        List<Map<String, Object>> conflicts = conflicts(studentId,teachingClassId);
        if (!conflicts.isEmpty()) {
            Map<String,Object> c=conflicts.get(0);
            int day=((Number)c.get("dayOfWeek")).intValue();
            throw new BizException(ErrorCode.ENROLL_TIME_CONFLICT,"与《"+c.get("courseName")+"》上课时间冲突（周"+WEEK_NAMES[day-1]+" 第"+c.get("startSection")+"-"+c.get("endSection")+"节）");
        }
    }

    public List<Map<String,Object>> conflicts(Long studentId,Long teachingClassId) {
        return jdbc.queryForList("""
                SELECT DISTINCT e.teaching_class_id AS teachingClassId,c.name AS courseName, s2.day_of_week AS dayOfWeek, s2.start_section AS startSection, s2.end_section AS endSection
                  FROM class_schedule s1
                  JOIN class_schedule s2
                    ON s1.day_of_week = s2.day_of_week
                   AND s2.teaching_class_id <> s1.teaching_class_id
                   AND s1.start_minute < s2.end_minute
                   AND s2.start_minute < s1.end_minute
                   AND s1.start_week <= s2.end_week
                   AND s2.start_week <= s1.end_week
                  JOIN enrollment e
                    ON e.teaching_class_id = s2.teaching_class_id
                   AND e.student_id = ?
                   AND e.status IN ('ENROLLED','COMPLETED')
                   AND e.deleted = 0
                  JOIN course c ON c.id = e.course_id
                 WHERE s1.teaching_class_id = ? AND e.term_id=(SELECT term_id FROM teaching_class WHERE id=s1.teaching_class_id)
                """, studentId, teachingClassId);

    }

    /** BR-06：课程若配置了先修课，学生必须已 COMPLETED 且成绩及格 */
    public void checkPrerequisite(Long studentId, Long courseId) {
        List<Map<String, Object>> missing = jdbc.queryForList("""
                SELECT c.name AS course_name
                  FROM course_prereq p
                  JOIN course c ON c.id = p.prereq_course_id
                 WHERE p.course_id = ?
                   AND NOT EXISTS (
                       SELECT 1 FROM enrollment e
                         JOIN grade g ON g.enrollment_id = e.id
                        WHERE e.student_id = ?
                          AND e.course_id = p.prereq_course_id
                          AND e.status = 'COMPLETED'
                          AND e.deleted = 0
                          AND g.status = 'PUBLISHED'
                          AND g.is_pass = 1)
                """, courseId, studentId);

        if (!missing.isEmpty()) {
            String names = missing.stream()
                    .map(m -> "《" + m.get("course_name") + "》")
                    .reduce((a, b) -> a + "、" + b)
                    .orElse("");
            throw new BizException(ErrorCode.ENROLL_PREREQ_FAILED, "需先修读并通过 " + names);
        }
    }

    /** 学分上限：本学期已选学分 + 本次学分 不得超过配置上限 */
    public void checkCreditMax(Long studentId, Long termId, BigDecimal addCredit) {
        BigDecimal max = configDecimal("enroll.max_credit_per_term", new BigDecimal("30"));
        if (max.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal current = currentCredit(studentId, termId);
        BigDecimal after = current.add(addCredit == null ? BigDecimal.ZERO : addCredit);
        if (after.compareTo(max) > 0) {
            throw new BizException(ErrorCode.ENROLL_CREDIT_MAX,
                    "本学期选课学分（" + after.stripTrailingZeros().toPlainString()
                            + "）将超过上限（" + max.stripTrailingZeros().toPlainString() + "）");
        }
    }

    /** 学分下限：退课后剩余学分不得低于配置下限 */
    public void checkCreditMin(Long studentId, Long termId, BigDecimal removeCredit) {
        BigDecimal min = configDecimal("enroll.min_credit_per_term", BigDecimal.ZERO);
        if (min.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal after = currentCredit(studentId, termId)
                .subtract(removeCredit == null ? BigDecimal.ZERO : removeCredit);
        if (after.compareTo(min) < 0) {
            throw new BizException(ErrorCode.ENROLL_CREDIT_MIN,
                    "退课后本学期学分（" + after.stripTrailingZeros().toPlainString()
                            + "）将低于下限（" + min.stripTrailingZeros().toPlainString() + "）");
        }
    }

    /** 本学期已选学分 */
    public BigDecimal currentCredit(Long studentId, Long termId) {
        BigDecimal sum = jdbc.queryForObject("""
                SELECT COALESCE(SUM(credit), 0) FROM enrollment
                 WHERE student_id = ? AND term_id = ? AND status IN ('ENROLLED','COMPLETED') AND deleted = 0
                """, BigDecimal.class, studentId, termId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private BigDecimal configDecimal(String key, BigDecimal defaultValue) {
        List<BigDecimal> values = jdbc.query("""
                SELECT config_value FROM sys_config WHERE config_key = ?
                """, (rs, rowNum) -> {
            try {
                return new BigDecimal(rs.getString(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }, key);
        return values.isEmpty() || values.get(0) == null ? defaultValue : values.get(0);
    }
}
