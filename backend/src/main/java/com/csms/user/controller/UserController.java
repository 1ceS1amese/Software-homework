package com.csms.user.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.api.PageResult;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import lombok.Data;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（U-01 / U-03 / U-07 / U-09）。
 *
 * <p>仅管理员可访问。响应中不含 password 字段（BR-15）。
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final JdbcTemplate jdbc;

    public UserController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** U-01 用户列表 */
    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        requireAdmin();

        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);

        StringBuilder where = new StringBuilder(" WHERE u.deleted = 0 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(role)) {
            where.append(" AND u.user_type = ? ");
            args.add(role);
        }
        if (deptId != null) {
            where.append(" AND u.dept_id = ? ");
            args.add(deptId);
        }
        if (majorId != null) {
            where.append(" AND u.major_id = ? ");
            args.add(majorId);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND u.status = ? ");
            args.add(status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (u.username LIKE ? OR u.real_name LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user u " + where,
                Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((p - 1) * s);

        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT u.id AS id, u.username AS username, u.real_name AS realName,
                       u.user_type AS userType, u.dept_id AS deptId, d.name AS deptName,
                       u.major_id AS majorId, m.name AS majorName,
                       u.gender AS gender, u.phone AS phone, u.email AS email,
                       u.status AS status, u.must_change_pwd AS mustChangePwd,
                       u.fail_count AS failCount,
                       DATE_FORMAT(u.lock_until, '%Y-%m-%d %H:%i:%s')    AS lockUntil,
                       DATE_FORMAT(u.last_login_at, '%Y-%m-%d %H:%i:%s') AS lastLoginAt,
                       DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i:%s')    AS createdAt
                  FROM sys_user u
                  LEFT JOIN dept d  ON d.id = u.dept_id
                  LEFT JOIN major m ON m.id = u.major_id
                """ + where + " ORDER BY u.id LIMIT ? OFFSET ?", pageArgs.toArray());

        return ApiResponse.success(new PageResult<>(records, total == null ? 0 : total, p, s));
    }

    /** U-03 用户详情 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        requireAdmin();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.id AS id, u.username AS username, u.real_name AS realName,
                       u.user_type AS userType, u.dept_id AS deptId, d.name AS deptName,
                       u.major_id AS majorId, m.name AS majorName,
                       u.gender AS gender, u.phone AS phone, u.email AS email,
                       u.status AS status, u.must_change_pwd AS mustChangePwd
                  FROM sys_user u
                  LEFT JOIN dept d  ON d.id = u.dept_id
                  LEFT JOIN major m ON m.id = u.major_id
                 WHERE u.id = ? AND u.deleted = 0
                """, id);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return ApiResponse.success(rows.get(0));
    }

    /** U-07 启用 / 禁用 */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        requireAdmin();
        String status = request == null ? null : request.getStatus();
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态只能是 ACTIVE 或 DISABLED");
        }
        int updated = jdbc.update("""
                UPDATE sys_user SET status = ?, updated_at = NOW() WHERE id = ? AND deleted = 0
                """, status, id);
        if (updated == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return ApiResponse.success();
    }

    /** U-09 用户统计 */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        requireAdmin();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN user_type = 'STUDENT' THEN 1 ELSE 0 END), 0) AS studentCount,
                       COALESCE(SUM(CASE WHEN user_type = 'TEACHER' THEN 1 ELSE 0 END), 0) AS teacherCount,
                       COALESCE(SUM(CASE WHEN user_type = 'ADMIN'   THEN 1 ELSE 0 END), 0) AS adminCount,
                       COALESCE(SUM(CASE WHEN status = 'DISABLED'   THEN 1 ELSE 0 END), 0) AS disabledCount
                  FROM sys_user WHERE deleted = 0
                """);
        Map<String, Object> result = new HashMap<>(row);
        return ApiResponse.success(result);
    }

    private void requireAdmin() {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!"ADMIN".equals(me.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有管理员可以管理用户");
        }
    }

    @Data
    public static class StatusRequest {
        private String status;
    }
}
