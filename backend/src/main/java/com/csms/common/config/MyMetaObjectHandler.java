package com.csms.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.csms.common.context.CurrentUserHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        CurrentUserHolder.UserContext userContext = CurrentUserHolder.get();
        if (userContext != null && userContext.getId() != null) {
            this.strictInsertFill(metaObject, "createdBy", Long.class, userContext.getId());
            this.strictInsertFill(metaObject, "updatedBy", Long.class, userContext.getId());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        
        CurrentUserHolder.UserContext userContext = CurrentUserHolder.get();
        if (userContext != null && userContext.getId() != null) {
            this.strictUpdateFill(metaObject, "updatedBy", Long.class, userContext.getId());
        }
    }
}
