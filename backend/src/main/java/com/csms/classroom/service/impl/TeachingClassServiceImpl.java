package com.csms.classroom.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.csms.classroom.entity.TeachingClass;
import com.csms.classroom.mapper.TeachingClassMapper;
import com.csms.classroom.service.TeachingClassService;
import org.springframework.stereotype.Service;

@Service
public class TeachingClassServiceImpl extends ServiceImpl<TeachingClassMapper, TeachingClass> implements TeachingClassService {
}
