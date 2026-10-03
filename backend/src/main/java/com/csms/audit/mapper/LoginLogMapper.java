package com.csms.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csms.audit.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
