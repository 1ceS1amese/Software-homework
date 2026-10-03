package com.csms.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.DayOfWeek;
import lombok.Data;

@Data
@TableName("class_schedule")
public class ClassSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teachingClassId;
    private DayOfWeek dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    private Integer startMinute;
    private Integer endMinute;
    private String location;
    private String weekDesc;
    private Integer startWeek;
    private Integer endWeek;
}
