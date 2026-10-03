package com.csms.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.PrereqRequirement;
import lombok.Data;

@Data
@TableName("course_prereq")
public class CoursePrereq {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long prereqCourseId;
    private PrereqRequirement requirement;
}
