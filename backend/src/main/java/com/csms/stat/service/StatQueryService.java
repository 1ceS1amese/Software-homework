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

    private String scope(String alias) {
        var me=com.csms.common.service.CommandSupport.require("STUDENT","TEACHER","ADMIN");
        if("TEACHER".equals(me.getRole())) return " AND "+alias+".teacher_id="+me.getId();
        if("STUDENT".equals(me.getRole())) return " AND EXISTS(SELECT 1 FROM enrollment own WHERE own.teaching_class_id="+alias+".id AND own.student_id="+me.getId()+" AND own.deleted=0 AND own.status IN ('ENROLLED','COMPLETED'))";
        return "";
    }
    private String term(Long id) { return id==null?"":" AND tc.term_id="+id; }
    private String enrolledExpression() {
        var me=com.csms.common.context.CurrentUserHolder.get();
        return "STUDENT".equals(me.getRole())?"1":"tc.enrolled_count";
    }
    /** S-01：各角色只聚合本人范围。 */
    public Map<String,Object> overview(Long termId) {
        String where=" WHERE tc.deleted=0"+scope("tc")+term(termId);
        var result=new LinkedHashMap<>(jdbc.queryForMap("SELECT COUNT(*) AS classCount,COALESCE(SUM(tc.capacity),0) AS totalCapacity,COALESCE(SUM("+enrolledExpression()+"),0) AS totalEnrollments FROM teaching_class tc"+where));
        var me=com.csms.common.context.CurrentUserHolder.get();
        long students;
        if("ADMIN".equals(me.getRole())) students=jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE deleted=0 AND user_type='STUDENT'",Long.class);
        else if("STUDENT".equals(me.getRole())) students=((Number)result.get("classCount")).longValue()>0?1:0;
        else students=jdbc.queryForObject("SELECT COUNT(DISTINCT e.student_id) FROM enrollment e JOIN teaching_class tc ON tc.id=e.teaching_class_id"+where+" AND e.deleted=0 AND e.status IN ('ENROLLED','COMPLETED')",Long.class);
        result.put("termId",termId);result.put("studentCount",students);
        result.put("avgFillRate",rate(((Number)result.get("totalEnrollments")).longValue(),((Number)result.get("totalCapacity")).longValue()));return result;
    }
    /** S-02：受角色范围、院系与学期约束。 */
    public List<Map<String,Object>> byCourse(Long termId,Long deptId,Integer limit) {
        String where=" WHERE tc.deleted=0"+scope("tc")+term(termId)+(deptId==null?"":" AND c.dept_id="+deptId);
        var rows=jdbc.queryForList("SELECT c.id AS courseId,c.course_code AS courseCode,c.name AS courseName,SUM(tc.capacity) AS capacity,SUM("+enrolledExpression()+") AS enrolledCount FROM teaching_class tc JOIN course c ON c.id=tc.course_id"+where+" GROUP BY c.id,c.course_code,c.name ORDER BY enrolledCount DESC,c.id LIMIT "+(limit==null?10:Math.max(1,Math.min(100,limit))));
        rows.forEach(r->r.put("fillRate",rate(((Number)r.get("enrolledCount")).longValue(),((Number)r.get("capacity")).longValue())));return rows;
    }
    public List<Map<String,Object>> byTeacher(Long termId,Long deptId) {
        com.csms.common.service.CommandSupport.require("ADMIN");
        var rows=jdbc.queryForList("SELECT u.id AS teacherId,u.real_name AS teacherName,COUNT(*) AS classCount,SUM(tc.capacity) AS capacity,SUM(tc.enrolled_count) AS enrolledCount FROM teaching_class tc JOIN sys_user u ON u.id=tc.teacher_id JOIN course c ON c.id=tc.course_id WHERE tc.deleted=0"+term(termId)+(deptId==null?"":" AND c.dept_id="+deptId)+" GROUP BY u.id,u.real_name ORDER BY enrolledCount DESC,u.id");
        rows.forEach(r->r.put("fillRate",rate(((Number)r.get("enrolledCount")).longValue(),((Number)r.get("capacity")).longValue())));return rows;
    }
    public List<Map<String,Object>> gradeDistribution(Long termId,Long classId) {
        var me=com.csms.common.service.CommandSupport.require("TEACHER","ADMIN");
        String filter=term(termId)+("TEACHER".equals(me.getRole())?" AND tc.teacher_id="+me.getId():"")+(classId==null?"":" AND tc.id="+classId);
        var values=jdbc.queryForList("SELECT g.total_score FROM grade g JOIN teaching_class tc ON tc.id=g.teaching_class_id JOIN enrollment e ON e.id=g.enrollment_id WHERE g.deleted=0 AND g.status='PUBLISHED' AND tc.deleted=0 AND e.deleted=0 AND e.status='COMPLETED'"+filter,BigDecimal.class);
        String[] labels={"0-59","60-69","70-79","80-89","90-100"};long[] counts=new long[5];
        for(BigDecimal value:values) if(value!=null) counts[value.compareTo(BigDecimal.valueOf(60))<0?0:value.compareTo(BigDecimal.valueOf(70))<0?1:value.compareTo(BigDecimal.valueOf(80))<0?2:value.compareTo(BigDecimal.valueOf(90))<0?3:4]++;
        var rows=new java.util.ArrayList<Map<String,Object>>();for(int i=0;i<5;i++) rows.add(Map.of("bucket",labels[i],"count",counts[i],"n",values.size()));return rows;
    }

    /** S-04 满员度分析 */
    public Map<String, Object> capacityAnalysis(Long termId) {
        com.csms.common.service.CommandSupport.require("ADMIN");
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
                        + " AND tc.enrolled_count <= tc.capacity*0.9 AND tc.enrolled_count < tc.capacity ORDER BY tc.id");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fullClasses", full);
        result.put("idleClasses", idle);
        result.put("normalClasses", normal);
        result.put("nearFullClasses",jdbc.queryForList(select+where+" AND tc.capacity>0 AND tc.enrolled_count>tc.capacity*0.9 AND tc.enrolled_count<tc.capacity ORDER BY tc.id"));
        return result;
    }

    /** S-06 学生学分汇总 */
    public Map<String, Object> studentSummary(Long studentId, Long termId) {
        String termFilter = termId != null ? " AND term_id = " + termId : "";

        BigDecimal enrolledCredits = jdbc.queryForObject("""
                SELECT COALESCE(SUM(credit), 0) FROM enrollment
                 WHERE student_id = ? AND status IN ('ENROLLED','COMPLETED') AND deleted = 0
                """ + termFilter, BigDecimal.class, studentId);

        Map<String, Object> earned = jdbc.queryForMap("""
                SELECT COALESCE(SUM(e.credit), 0) AS earnedCredits,
                       COUNT(*)                   AS passCount
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED' AND g.deleted=0 AND g.is_pass = 1
                """ + (termId != null ? " AND e.term_id = " + termId : ""), studentId);

        Map<String, Object> allCompleted = jdbc.queryForMap("""
                SELECT COUNT(*) AS courseCount,
                       COALESCE(SUM(CASE WHEN g.is_pass = 0 THEN 1 ELSE 0 END), 0) AS failCount
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED' AND g.deleted=0
                """ + (termId != null ? " AND e.term_id = " + termId : ""), studentId);

        BigDecimal gpa = jdbc.queryForObject("""
                SELECT COALESCE(SUM(g.grade_point * e.credit) / NULLIF(SUM(e.credit), 0), 0) AS gpa
                  FROM enrollment e
                  JOIN grade g ON g.enrollment_id = e.id
                 WHERE e.student_id = ? AND e.status = 'COMPLETED' AND e.deleted = 0
                   AND g.status = 'PUBLISHED' AND g.deleted=0
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
        result.put("courses",jdbc.queryForList("SELECT e.id AS enrollmentId,e.course_id AS courseId,c.name AS courseName,e.credit,e.status,g.total_score AS totalScore,g.grade_point AS gradePoint FROM enrollment e JOIN course c ON c.id=e.course_id LEFT JOIN grade g ON g.enrollment_id=e.id AND g.deleted=0 AND g.status='PUBLISHED' WHERE e.student_id=? AND e.deleted=0 AND e.status IN ('ENROLLED','COMPLETED')"+(termId==null?"":" AND e.term_id="+termId)+" ORDER BY e.id",studentId));
        BigDecimal limit=jdbc.queryForObject("SELECT config_value FROM sys_config WHERE config_key='enroll.max_credit_per_term'",BigDecimal.class);
        result.put("creditLimit",limit);result.put("remainingCredits",limit==null||limit.signum()<=0?null:limit.subtract(scale(enrolledCredits)).max(BigDecimal.ZERO));
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
