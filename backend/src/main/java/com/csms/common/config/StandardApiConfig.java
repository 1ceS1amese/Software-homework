package com.csms.common.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;

/** 在同一控制器实现上登记标准入口及历史入口，不复制业务逻辑。 */
@Configuration
public class StandardApiConfig implements WebMvcRegistrations {
    @Override public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override protected RequestMappingInfo getMappingForMethod(Method method,Class<?> type) {
                var mapping=super.getMappingForMethod(method,type);
                if(mapping==null || !type.getPackageName().startsWith("com.csms")) return mapping;
                var paths=new LinkedHashSet<>(mapping.getPatternValues());
                for(String path:mapping.getPatternValues()) if(!path.startsWith("/api/v1/")) paths.add("/api/v1"+path);
                return mapping.mutate().paths(paths.toArray(String[]::new)).build();
            }
        };
    }
}
