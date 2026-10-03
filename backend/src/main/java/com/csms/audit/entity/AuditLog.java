package com.csms.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.RoleCode;
import com.csms.common.enums.AuditModule;
import com.csms.common.enums.AuditResult;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private RoleCode roleCode;
    private AuditModule module;
    private String action;
    private String targetType;
    private Long targetId;
    private String beforeJson;
    private String afterJson;
    private AuditResult result;
    private String ip;
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
