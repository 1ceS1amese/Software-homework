package com.csms.audit.service;

import com.csms.common.context.CurrentUserHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.MDC;
import java.util.*;

/** 业务变更在原事务内审计；只在提交后标记完成，回滚后由请求审计记录失败。 */
@Component
public class AuditWriter {
    public static final String RECORDED = AuditWriter.class.getName() + ".recorded";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public AuditWriter(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc;this.mapper=mapper; }

    public void record(String module,String action,String targetType,Long targetId,Object before,Object after) {
        recordAs(CurrentUserHolder.get(),module,action,targetType,targetId,before,after,"SUCCESS",request());
    }
    public void recordAs(CurrentUserHolder.UserContext user,String module,String action,String targetType,Long targetId,
                         Object before,Object after,String result,HttpServletRequest request) {
        try {
            jdbc.update("""
                INSERT INTO sys_audit_log
                (user_id,username,role_code,module,action,target_type,target_id,before_json,after_json,result,ip,trace_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """,user==null?null:user.getId(),user==null?null:user.getUsername(),user==null?null:user.getRole(),
                module,action,targetType,targetId,json(before),json(after),result,
                request==null?null:clip(request.getRemoteAddr(),45),MDC.get("traceId"));
            if(request!=null) {
                if(TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() { request.setAttribute(RECORDED,Boolean.TRUE); }
                    });
                } else request.setAttribute(RECORDED,Boolean.TRUE);
            }
        } catch(JsonProcessingException e) { throw new IllegalStateException("审计数据序列化失败",e); }
    }
    private String json(Object value) throws JsonProcessingException {
        return value==null?null:mapper.writeValueAsString(redact(mapper.convertValue(value,Object.class)));
    }
    static Object redact(Object value) {
        if(value instanceof Map<?,?> map) {
            Map<String,Object> clean=new LinkedHashMap<>();
            map.forEach((k,v)-> { String key=String.valueOf(k); if(!key.toLowerCase(Locale.ROOT).matches(".*(password|passwd|secret|token|authorization|cookie).*")) clean.put(key,redact(v)); });
            return clean;
        }
        if(value instanceof List<?> list) return list.stream().map(AuditWriter::redact).toList();
        return value;
    }
    public static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes s?s.getRequest():null;
    }
    public static String clip(String text,int max) { return text==null?null:text.substring(0,Math.min(text.length(),max)); }
}
