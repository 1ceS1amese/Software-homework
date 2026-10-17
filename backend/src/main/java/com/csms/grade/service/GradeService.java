package com.csms.grade.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 成绩服务（G-01 ~ G-06）。
 *
 * <p>总评与绩点一律由服务端按 {@code sys_config} 计算，不信任前端传值（BR-11）。
 * 发布是单向锁：发布后教师不可改，需管理员解锁并填写原因（BR-12）。
 */
@Service
public class GradeService {

    private final JdbcTemplate jdbc;
    private final AuditWriter audit;

    public GradeService(JdbcTemplate jdbc, AuditWriter audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    /** G-01 教学班成绩表（含未录入的学生） */
    public List<Map<String, Object>> classGrades(Long teachingClassId) {
        return jdbc.queryForList("""
                SELECT g.id                        AS id,
                       e.id                        AS enrollmentId,
                       e.teaching_class_id         AS teachingClassId,
                       e.student_id                AS studentId,
                       u.username                  AS studentNumber,
                       u.real_name                 AS studentName,
                       g.regular_score             AS regularScore,
                       g.midterm_score             AS midtermScore,
                       g.final_score               AS finalScore,
                       g.total_score               AS totalScore,
                       g.grade_point               AS gradePoint,
                       g.is_pass                   AS isPass,
                       COALESCE(g.status, 'DRAFT') AS status,
                       DATE_FORMAT(g.published_at, '%Y-%m-%d %H:%i:%s') AS publishedAt,
                       COALESCE(g.unlock_count, 0) AS unlockCount,
                       g.last_unlock_reason        AS lastUnlockReason
                  FROM enrollment e
                  JOIN teaching_class tc ON tc.id = e.teaching_class_id
                  JOIN sys_user u        ON u.id = e.student_id
                  LEFT JOIN grade g      ON g.enrollment_id = e.id AND g.deleted=0
                 WHERE e.teaching_class_id = ?
                   AND e.status IN ('ENROLLED', 'COMPLETED')
                   AND e.deleted = 0
                 ORDER BY u.username
                """, teachingClassId);
    }

    /** G-02 批量暂存成绩（整批成功或整批失败） */
    @Transactional
    public void saveDraft(Long teachingClassId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        lockClass(teachingClassId);
        assertEditable(teachingClassId);

        BigDecimal[] weights = weights();
        BigDecimal passScore = passScore();

        // 先校验整批归属及分数，再写入，禁止跨班 ID、重复 ID 或缺失 ID 被悄悄跳过。
        var seen = new HashSet<Long>();
        for (var item : items) {
            if (item == null) throw new BizException(ErrorCode.PARAM_ERROR, "成绩记录不能为空");
            Long enrollmentId = toLong(item.get("enrollmentId"));
            if (enrollmentId == null || enrollmentId <= 0 || !seen.add(enrollmentId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "选课记录ID必须为唯一的正整数");
            }
            var enrolled = jdbc.queryForList("""
                    SELECT teaching_class_id, status FROM enrollment WHERE id=? AND deleted=0 FOR UPDATE
                    """, enrollmentId);
            if (enrolled.isEmpty() || ((Number) enrolled.get(0).get("teaching_class_id")).longValue() != teachingClassId) {
                throw new BizException(ErrorCode.FORBIDDEN, "成绩记录不属于当前教学班");
            }
            if (!List.of("ENROLLED", "COMPLETED").contains(enrolled.get(0).get("status"))) {
                throw new BizException(ErrorCode.CONFLICT, "已退课的学生不能录入成绩");
            }
            validate(toDecimal(item.get("regularScore")));
            validate(toDecimal(item.get("midtermScore")));
            validate(toDecimal(item.get("finalScore")));
        }
        var before = classGrades(teachingClassId);
        for (Map<String, Object> item : items) {
            Long enrollmentId = toLong(item.get("enrollmentId"));
            BigDecimal regular = toDecimal(item.get("regularScore"));
            BigDecimal midterm = toDecimal(item.get("midtermScore"));
            BigDecimal finalScore = toDecimal(item.get("finalScore"));

            validate(regular);
            validate(midterm);
            validate(finalScore);

            BigDecimal total = computeTotal(regular, midterm, finalScore, weights);
            BigDecimal gradePoint = total == null ? null : gradePoint(total, passScore);
            Integer pass = total == null ? null : (total.compareTo(passScore) >= 0 ? 1 : 0);

            // 该选课关系是否已有成绩行
            List<Long> existing = jdbc.query("""
                    SELECT id FROM grade WHERE enrollment_id = ?
                    """, (rs, i) -> rs.getLong(1), enrollmentId);

            if (existing.isEmpty()) {
                jdbc.update("""
                        INSERT INTO grade (enrollment_id, teaching_class_id, student_id, course_id, term_id,
                                           regular_score, midterm_score, final_score, total_score,
                                           grade_point, is_pass, status)
                        SELECT e.id, e.teaching_class_id, e.student_id, e.course_id, e.term_id,
                               ?, ?, ?, ?, ?, ?, 'DRAFT'
                          FROM enrollment e WHERE e.id = ? AND e.teaching_class_id=? AND e.deleted=0
                        """, regular, midterm, finalScore, total, gradePoint, pass, enrollmentId, teachingClassId);
            } else {
                jdbc.update("""
                        UPDATE grade
                           SET regular_score = ?, midterm_score = ?, final_score = ?,
                               total_score = ?, grade_point = ?, is_pass = ?, updated_at = NOW()
                         WHERE enrollment_id = ? AND teaching_class_id=? AND status='DRAFT'
                        """, regular, midterm, finalScore, total, gradePoint, pass, enrollmentId, teachingClassId);
            }
        }
        audit.record("GRADE", "SAVE_GRADES", "teaching_class", teachingClassId, before, classGrades(teachingClassId));
    }

    /** G-03 发布成绩（BR-13：成绩与选课状态在同一事务内变更） */
    @Transactional
    public void publish(Long teachingClassId, Long publisherId) {
        lockClass(teachingClassId);
        List<Map<String, Object>> rows = classGrades(teachingClassId);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.BIZ_ERROR, "该教学班没有学生，无法发布");
        }
        long incomplete = rows.stream()
                .filter(r -> r.get("totalScore") == null)
                .count();
        if (incomplete > 0) {
            throw new BizException(ErrorCode.GRADE_INCOMPLETE,
                    "存在 " + incomplete + " 名学生未录入完整成绩，无法发布");
        }

        boolean allPublished = rows.stream().allMatch(r -> "PUBLISHED".equals(r.get("status")));
        if (allPublished) {
            return; // 幂等：重复发布不产生副作用
        }

        jdbc.update("""
                UPDATE grade
                   SET status = 'PUBLISHED', published_at = NOW(), published_by = ?, updated_at = NOW()
                 WHERE teaching_class_id = ? AND status = 'DRAFT'
                   AND enrollment_id IN (SELECT id FROM enrollment WHERE teaching_class_id=? AND status='ENROLLED' AND deleted=0)
                """, publisherId, teachingClassId, teachingClassId);

        jdbc.update("""
                UPDATE enrollment
                   SET status = 'COMPLETED'
                 WHERE teaching_class_id = ? AND status = 'ENROLLED' AND deleted = 0
                """, teachingClassId);
        audit.record("GRADE", "PUBLISH_GRADES", "teaching_class", teachingClassId, rows, classGrades(teachingClassId));
    }

    /** G-04 管理员解锁成绩 */
    @Transactional
    public void unlock(Long teachingClassId, String reason) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException(ErrorCode.GRADE_UNLOCK_REASON_REQUIRED, "解锁原因至少 5 个字");
        }
        if (reason.trim().length() > 300) {
            throw new BizException(ErrorCode.PARAM_ERROR, "解锁原因不能超过300字");
        }
        lockClass(teachingClassId);
        var before = classGrades(teachingClassId);
        int updated = jdbc.update("""
                UPDATE grade
                   SET status = 'DRAFT',
                       unlock_count = COALESCE(unlock_count, 0) + 1,
                       last_unlock_reason = ?,
                       updated_at = NOW()
                 WHERE teaching_class_id = ? AND status = 'PUBLISHED'
                """, reason.trim(), teachingClassId);
        if (updated == 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "该教学班没有已发布的成绩");
        }
        jdbc.update("""
                UPDATE enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                   SET e.status = 'ENROLLED'
                 WHERE e.teaching_class_id = ? AND e.status = 'COMPLETED'
                """, teachingClassId);
        audit.record("GRADE", "UNLOCK_GRADES", "teaching_class", teachingClassId,
                before, Map.of("reason", reason.trim(), "grades", classGrades(teachingClassId)));
    }

    /** G-05 学生本人成绩（只返回已发布） */
    public List<Map<String, Object>> myGrades(Long studentId, Long termId) {
        StringBuilder sql = new StringBuilder("""
                SELECT g.id                    AS id,
                       g.enrollment_id         AS enrollmentId,
                       g.teaching_class_id     AS teachingClassId,
                       g.student_id            AS studentId,
                       g.course_id             AS courseId,
                       c.course_code           AS courseCode,
                       c.name                  AS courseName,
                       e.credit                AS credit,
                       g.term_id               AS termId,
                       g.regular_score         AS regularScore,
                       g.midterm_score         AS midtermScore,
                       g.final_score           AS finalScore,
                       g.total_score           AS totalScore,
                       g.grade_point           AS gradePoint,
                       g.is_pass               AS isPass,
                       g.status                AS status,
                       DATE_FORMAT(g.published_at, '%Y-%m-%d %H:%i:%s') AS publishedAt
                  FROM grade g
                  JOIN enrollment e ON e.id = g.enrollment_id
                  JOIN course c     ON c.id = g.course_id
                 WHERE g.student_id = ? AND g.status = 'PUBLISHED' AND g.deleted=0
                   AND e.deleted=0 AND e.status='COMPLETED'
                """);
        List<Object> args = new ArrayList<>();
        args.add(studentId);
        if (termId != null) {
            sql.append(" AND g.term_id = ?");
            args.add(termId);
        }
        sql.append(" ORDER BY g.term_id DESC, c.course_code");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    /** G-06 分数段分布 */
    public List<Map<String, Object>> distribution(Long teachingClassId) {
        return jdbc.queryForList("""
                SELECT bucket, COUNT(*) AS count FROM (
                    SELECT CASE
                             WHEN total_score IS NULL      THEN '未录入'
                             WHEN total_score >= 90        THEN '90-100'
                             WHEN total_score >= 80        THEN '80-89'
                             WHEN total_score >= 70        THEN '70-79'
                             WHEN total_score >= 60        THEN '60-69'
                             ELSE '0-59'
                           END AS bucket
                      FROM grade
                     WHERE teaching_class_id = ? AND status = 'PUBLISHED' AND deleted=0
                ) t GROUP BY bucket ORDER BY bucket
                """, teachingClassId);
    }

    // —————————————————— 内部 ——————————————————

    void lockClass(Long teachingClassId) {
        if (jdbc.queryForList("SELECT id FROM teaching_class WHERE id=? AND deleted=0 FOR UPDATE", teachingClassId).isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "教学班不存在");
        }
    }

    /** 教学班成绩必须处于可编辑状态 */
    void assertEditable(Long teachingClassId) {
        Integer published = jdbc.queryForObject("""
                SELECT COUNT(*) FROM grade
                 WHERE teaching_class_id = ? AND status = 'PUBLISHED' AND deleted=0
                """, Integer.class, teachingClassId);
        if (published != null && published > 0) {
            throw new BizException(ErrorCode.GRADE_PUBLISHED);
        }
    }

    void validate(BigDecimal score) {
        if (score == null) {
            return;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new BizException(ErrorCode.GRADE_INVALID,
                    "成绩必须在 0 ~ 100 之间，当前值：" + score.toPlainString());
        }
        if (score.stripTrailingZeros().scale() > 2) {
            throw new BizException(ErrorCode.GRADE_INVALID, "成绩最多保留两位小数");
        }
    }

    private BigDecimal computeTotal(BigDecimal regular, BigDecimal midterm, BigDecimal finalScore,
                                    BigDecimal[] weights) {
        if (regular == null || midterm == null || finalScore == null) {
            return null;
        }
        return regular.multiply(weights[0])
                .add(midterm.multiply(weights[1]))
                .add(finalScore.multiply(weights[2]))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 绩点：百分制线性映射（AS-13 默认方案），< 及格线记 0 */
    private BigDecimal gradePoint(BigDecimal total, BigDecimal passScore) {
        if (total.compareTo(passScore) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE
                .add(total.subtract(new BigDecimal("60")).multiply(new BigDecimal("0.075")))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal[] weights() {
        var result = new BigDecimal[]{
                configDecimal("grade.weight.regular", new BigDecimal("0.3")),
                configDecimal("grade.weight.midterm", new BigDecimal("0.3")),
                configDecimal("grade.weight.final", new BigDecimal("0.4"))
        };
        if (java.util.Arrays.stream(result).anyMatch(weight -> weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0)
                || result[0].add(result[1]).add(result[2]).compareTo(BigDecimal.ONE) != 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "成绩权重须在0到1之间且合计为1，请联系管理员");
        }
        return result;
    }

    private BigDecimal passScore() {
        return configDecimal("grade.pass_score", new BigDecimal("60"));
    }

    private BigDecimal configDecimal(String key, BigDecimal defaultValue) {
        List<BigDecimal> values = jdbc.query("SELECT config_value FROM sys_config WHERE config_key = ?",
                (rs, rowNum) -> {
                    try {
                        return new BigDecimal(rs.getString(1));
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }, key);
        return values.isEmpty() || values.get(0) == null ? defaultValue : values.get(0);
    }

    Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    BigDecimal toDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCode.GRADE_INVALID, "成绩格式非法：" + text);
        }
    }

    /** 供控制器复用的所属校验 */
    public void assertTeacherOwns(Long teachingClassId, Long teacherId, boolean isAdmin) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM teaching_class
                 WHERE id = ? AND (? OR teacher_id = ?) AND deleted = 0
                """, Integer.class, teachingClassId, isAdmin, teacherId);
        if (count == null || count == 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能操作本人名下的教学班");
        }
    }
}
