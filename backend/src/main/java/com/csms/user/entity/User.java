package com.csms.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.UserType;
import com.csms.common.enums.Gender;
import com.csms.common.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private UserType userType;
    private Long deptId;
    private Long majorId;
    private Gender gender;
    private String phone;
    private String email;
    private UserStatus status;
    private Boolean mustChangePwd;
    private Integer failCount;
    private LocalDateTime lockUntil;
    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
    @Version
    private Integer version;
}
