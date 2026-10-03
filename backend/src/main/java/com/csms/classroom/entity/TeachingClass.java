package com.csms.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.TeachingClassStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("teaching_class")
public class TeachingClass {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long termId;
    private Long teacherId;
    private String assistantIds;
    private String className;
    private Integer capacity;
    private Integer enrolledCount;
    private BigDecimal credit;
    private String location;
    private Integer startWeek;
    private Integer endWeek;
    private LocalDate openClassDate;
    private TeachingClassStatus status;
    private Boolean capacityFullEnabled;
    private String description;

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
