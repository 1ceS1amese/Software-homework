package com.csms.audit.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.LoginResult;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Long userId;
    private String ip;
    private String userAgent;
    private LoginResult result;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginAt;
}
