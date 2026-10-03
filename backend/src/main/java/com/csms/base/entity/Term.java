package com.csms.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.TermStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("term")
public class Term {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime enrollStart;
    private LocalDateTime enrollEnd;
    private LocalDateTime withdrawEnd;
    private TermStatus status;

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
