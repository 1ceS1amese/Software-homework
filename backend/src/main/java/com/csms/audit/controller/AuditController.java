package com.csms.audit.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.api.PageResult;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审计与登录日志接口（L-01 ~ L-03）。
 *
 * <p>仅管理员可查。审计数据由各业务的写操作产生，本期先提供检索能力。
 */
@RestController
public class AuditController {

    private final JdbcTemplate jdbc;

    public AuditController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** L-01 操作审计日志 */
    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<Map<String, Object>>> auditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        requireAdmin();

        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (userId != null) {
            where.append(" AND user_id = ? ");
            args.add(userId);
        }
        if (StringUtils.hasText(username)) {
            where.append(" AND username LIKE ? ");
            args.add("%" + username + "%");
        }
        if (StringUtils.hasText(module)) {
            where.append(" AND module = ? ");
            args.add(module);
        }
        if (StringUtils.hasText(action)) {
            where.append(" AND action = ? ");
            args.add(action);
        }
        if (StringUtils.hasText(result)) {
            where.append(" AND result = ? ");
            args.add(result);
        }
        if (StringUtils.hasText(startTime)) {
            where.append(" AND created_at >= ? ");
            args.add(startTime);
        }
        if (StringUtils.hasText(endTime)) {
            where.append(" AND created_at <= ? ");
            args.add(endTime);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_audit_log " + where,
                Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((p - 1) * s);

        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT id AS id, user_id AS userId, username AS username, role_code AS roleCode,
                       module AS module, action AS action,
                       target_type AS targetType, target_id AS targetId,
                       before_json AS beforeJson, after_json AS afterJson,
                       result AS result, ip AS ip, trace_id AS traceId,
                       DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                  FROM sys_audit_log
                """ + where + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());

        return ApiResponse.success(new PageResult<>(records, total == null ? 0 : total, p, s));
    }

    /** L-03 审计详情 */
    @GetMapping("/audit-logs/{id}")
    public ApiResponse<Map<String, Object>> auditDetail(@PathVariable Long id) {
        requireAdmin();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id AS id, user_id AS userId, username AS username, role_code AS roleCode,
                       module AS module, action AS action,
                       target_type AS targetType, target_id AS targetId,
                       before_json AS beforeJson, after_json AS afterJson,
                       result AS result, ip AS ip, trace_id AS traceId,
                       DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                  FROM sys_audit_log WHERE id = ?
                """, id);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "审计记录不存在");
        }
        return ApiResponse.success(rows.get(0));
    }

    /** L-02 登录日志 */
    @GetMapping("/login-logs")
    public ApiResponse<PageResult<Map<String, Object>>> loginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        requireAdmin();

        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 100);

        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(username)) {
            where.append(" AND username LIKE ? ");
            args.add("%" + username + "%");
        }
        if (StringUtils.hasText(result)) {
            where.append(" AND result = ? ");
            args.add(result);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_login_log " + where,
                Long.class, args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((p - 1) * s);

        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT id AS id, username AS username, user_id AS userId, ip AS ip,
                       user_agent AS userAgent, result AS result,
                       DATE_FORMAT(login_at, '%Y-%m-%d %H:%i:%s') AS loginAt
                  FROM sys_login_log
                """ + where + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());

        return ApiResponse.success(new PageResult<>(records, total == null ? 0 : total, p, s));
    }

    private void requireAdmin() {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!"ADMIN".equals(me.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有管理员可以查看审计日志");
        }
    }
}
