package com.csms.audit.filter;

import com.csms.audit.service.AuditWriter;
import com.csms.common.context.CurrentUserHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import java.io.IOException;
import java.util.Map;

/** 覆盖业务读写及拒绝请求，不读取请求体、查询参数、认证头或响应体。 */
@Component
public class RequestAuditFilter extends OncePerRequestFilter {
    private final AuditWriter audit;
    public RequestAuditFilter(AuditWriter audit) { this.audit=audit; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()) || "/actuator/health".equals(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        boolean thrown=true;
        try { chain.doFilter(request,response);thrown=false; }
        finally {
            if(!Boolean.TRUE.equals(request.getAttribute(AuditWriter.RECORDED))) {
                Object pattern=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                String path=pattern==null?"unmapped":pattern.toString().replaceFirst("^/api/v1","");
                String module=path.contains("auth")?"AUTH":path.contains("enroll")?"ENROLL":path.contains("grade")?"GRADE":path.contains("teaching-class")?"CLASS":path.contains("user")?"USER":"BASE";
                try {
                    audit.recordAs(CurrentUserHolder.get(),module,"HTTP_"+request.getMethod(),"http_request",null,null,
                        Map.of("route",path,"status",thrown?500:response.getStatus()),
                        thrown||response.getStatus()>=400?"FAIL":"SUCCESS",request);
                } catch(RuntimeException e) {
                    // Business writes fail closed in their own transaction. Unavailable DB cannot store request audits.
                    LoggerFactory.getLogger(getClass()).error("Request audit unavailable, traceId={}",MDC.get("traceId"));
                }
            }
        }
    }
}
