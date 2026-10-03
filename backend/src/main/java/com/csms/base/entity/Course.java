package com.csms.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.CourseType;
import com.csms.common.enums.BaseStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String courseCode;
    private String name;
    private BigDecimal credit;
    private Integer creditHours;
    private CourseType courseType;
    private Long deptId;
    private Long majorId;
    private String description;
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
