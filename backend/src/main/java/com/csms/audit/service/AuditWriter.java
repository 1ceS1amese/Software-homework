package com.csms.audit.service;

import com.csms.common.context.CurrentUserHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 必须在业务事务内调用；不记录密码、令牌或完整请求体。 */
@Component
public class AuditWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AuditWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void record(String module, String action, String targetType, Long targetId,
                       Object before, Object after) {
        var user = CurrentUserHolder.get();
        try {
            jdbc.update("""
                    INSERT INTO sys_audit_log
                      (user_id, username, role_code, module, action, target_type, target_id,
                       before_json, after_json, result)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESS')
                    """, user == null ? null : user.getId(), user == null ? null : user.getUsername(),
                    user == null ? null : user.getRole(), module, action, targetType, targetId,
                    before == null ? null : mapper.writeValueAsString(before),
                    after == null ? null : mapper.writeValueAsString(after));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("审计数据序列化失败", exception);
        }
    }
}
