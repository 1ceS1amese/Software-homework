package com.csms.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.BaseStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("major")
public class Major {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private String code;
    private String name;
    private Integer degreeYears;
    private BaseStatus status;

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
