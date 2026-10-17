package com.csms.base.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

/** B-14/B-16/B-17；逻辑删除与关联校验，复用既有 course 契约。 */
@Service
public class CourseCommandService {
    private final JdbcTemplate jdbc;
    private final AuditWriter audit;

    public CourseCommandService(JdbcTemplate jdbc, AuditWriter audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public Map<String, Object> detail(Long id) {
        var rows = jdbc.queryForList("""
                SELECT id, course_code AS courseCode, name, credit, credit_hours AS creditHours,
                       course_type AS courseType, dept_id AS deptId, major_id AS majorId, description, status
                  FROM course WHERE id = ? AND deleted = 0
                """, id);
        if (rows.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        var result=rows.get(0);
        result.put("prereqs",jdbc.queryForList("SELECT prereq_course_id AS prereqCourseId,requirement FROM course_prereq WHERE course_id=?",id));
        return result;
    }

    @Transactional
    public Map<String, Object> create(CourseRequest request) {
        validateReferences(request);
        var key = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                var statement = connection.prepareStatement("""
                        INSERT INTO course (course_code, name, credit, credit_hours, course_type,
                                            dept_id, major_id, description, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                Object[] values = values(request);
                for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                return statement;
            }, key);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.COURSE_CODE_EXISTS);
        }
        Long id = key.getKey().longValue();
        var created = detail(id);
        audit.record("BASE", "CREATE_COURSE", "course", id, null, created);
        return created;
    }

    @Transactional
    public Map<String, Object> update(Long id, CourseRequest request) {
        lock(id);
        var before = detail(id);
        validateReferences(request);
        try {
            Object[] values = values(request);
            Object[] args = java.util.Arrays.copyOf(values, values.length + 1);
            args[values.length] = id;
            jdbc.update("""
                    UPDATE course SET course_code=?, name=?, credit=?, credit_hours=?, course_type=?,
                                      dept_id=?, major_id=?, description=?, status=?, version=version+1
                     WHERE id=? AND deleted=0
                    """, args);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ErrorCode.COURSE_CODE_EXISTS);
        }
        var after = detail(id);
        audit.record("BASE", "UPDATE_COURSE", "course", id, before, after);
        return after;
    }

    @Transactional
    public void delete(Long id) {
        lock(id);
        var before = detail(id);
        Integer references = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM teaching_class WHERE course_id=? AND deleted=0)
                     + (SELECT COUNT(*) FROM course_prereq WHERE prereq_course_id=?)
                     + (SELECT COUNT(*) FROM enrollment WHERE course_id=? AND deleted=0)
                """, Integer.class, id, id, id);
        if (references != null && references > 0) {
            throw new BizException(ErrorCode.CONFLICT, "课程已有教学班、选课或先修引用，不能删除");
        }
        jdbc.update("DELETE FROM course_prereq WHERE course_id=?", id);
        jdbc.update("UPDATE course SET deleted=1, version=version+1 WHERE id=?", id);
        audit.record("BASE", "DELETE_COURSE", "course", id, before, Map.of("deleted", 1));
    }

    private void lock(Long id) {
        if (jdbc.queryForList("SELECT id FROM course WHERE id=? AND deleted=0 FOR UPDATE", id).isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
    }

    private void validateReferences(CourseRequest request) {
        if (!Set.of("REQUIRED", "ELECTIVE", "RESTRICTED").contains(request.getCourseType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "课程类型无效");
        }
        if (!Set.of("ACTIVE", "DISABLED").contains(request.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "课程状态无效");
        }
        if (jdbc.queryForList("SELECT id FROM dept WHERE id=? AND deleted=0 AND status='ACTIVE' FOR UPDATE",
                request.getDeptId()).isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "开课院系不存在或已停用");
        }
        if (request.getMajorId() != null && jdbc.queryForList("""
                SELECT id FROM major WHERE id=? AND dept_id=? AND deleted=0 AND status='ACTIVE' FOR UPDATE
                """, request.getMajorId(), request.getDeptId()).isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "专业不存在、已停用或不属于开课院系");
        }
    }

    private Object[] values(CourseRequest request) {
        return new Object[]{request.getCourseCode().trim(), request.getName().trim(), request.getCredit(),
                request.getCreditHours(), request.getCourseType(), request.getDeptId(), request.getMajorId(),
                request.getDescription(), request.getStatus()};
    }

    @Data
    public static class CourseRequest {
        @NotBlank(message="课程代码不能为空") @Size(max=32, message="课程代码不能超过32字") private String courseCode;
        @NotBlank(message="课程名称不能为空") @Size(max=100, message="课程名称不能超过100字") private String name;
        @NotNull(message="学分不能为空") @DecimalMin(value="0.1", message="学分不能低于0.1")
        @DecimalMax(value="99.9", message="学分不能超过99.9")
        @Digits(integer=2, fraction=1, message="学分最多保留一位小数") private BigDecimal credit;
        @NotNull(message="学时不能为空") @Min(value=1, message="学时必须为正数")
        @Max(value=10000, message="学时不能超过10000") private Integer creditHours;
        @NotBlank(message="课程类型不能为空") private String courseType;
        @NotNull(message="开课院系不能为空") @Positive(message="院系ID必须为正数") private Long deptId;
        @Positive(message="专业ID必须为正数") private Long majorId;
        @Size(max=1000, message="课程说明不能超过1000字") private String description;
        @NotNull(message="课程状态不能为空") private String status = "ACTIVE";
    }
}
