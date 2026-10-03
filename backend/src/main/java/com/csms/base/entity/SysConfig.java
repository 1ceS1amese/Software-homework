package com.csms.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.csms.common.enums.ConfigValueType;
import lombok.Data;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private ConfigValueType valueType;
    private String description;
    private Boolean editable;
}
