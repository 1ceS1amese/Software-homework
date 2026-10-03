package com.csms.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.RoleCode;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private RoleCode code;
    private String name;
    private String description;
    private Boolean builtin;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
