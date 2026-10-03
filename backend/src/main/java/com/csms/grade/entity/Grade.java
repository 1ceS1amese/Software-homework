package com.csms.grade.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.GradeStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grade")
public class Grade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enrollmentId;
    private Long teachingClassId;
    private Long studentId;
    private Long courseId;
    private Long termId;
    private BigDecimal regularScore;
    private BigDecimal midtermScore;
    private BigDecimal finalScore;
    private BigDecimal totalScore;
    private BigDecimal gradePoint;
    private Boolean isPass;
    private GradeStatus status;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private Integer unlockCount;
    private String lastUnlockReason;

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
