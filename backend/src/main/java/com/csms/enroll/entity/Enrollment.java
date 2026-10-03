package com.csms.enroll.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.EnrollmentStatus;
import com.csms.common.enums.EnrollmentSource;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("enrollment")
public class Enrollment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long teachingClassId;
    private Long courseId;
    private Long termId;
    private BigDecimal credit;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime withdrawnAt;
    private Long gradeId;
    private EnrollmentSource source;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
