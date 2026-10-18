package com.csms.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.*;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
public class StandardResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;
    public StandardResponseAdvice(ObjectMapper mapper) { this.mapper=mapper; }
    @Override public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c) { return true; }
    @Override public Object beforeBodyWrite(Object body,MethodParameter p,MediaType t,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest request,ServerHttpResponse response) {
        if(!request.getURI().getPath().startsWith("/api/v1/")) return body;
        if(body instanceof ApiResponse<?> envelope) {
            var result=new ApiResponse<>(envelope.getCode(),envelope.getMessage(),normalize(envelope.getData(),""));
            result.setTraceId(envelope.getTraceId());return result;
        }
        return body;
    }
    public Object normalize(Object value,String key) {
        if(value==null) return null;
        if(value instanceof Map<?,?> source) {
            Map<String,Object> result=new LinkedHashMap<>();
            source.forEach((k,v)-> { String name=camel(String.valueOf(k));result.put(name,normalize(v,name)); });
            return result;
        }
        if(value instanceof Collection<?> list) return list.stream().map(v->normalize(v,key.endsWith("Ids")?"id":"")).toList();
        if(value instanceof PageResult<?> page) return Map.of("records",normalize(page.getRecords(),"records"),"total",page.getTotal(),"page",page.getPage(),"size",page.getSize(),"pages",page.getPages());
        if(value instanceof Number && (key.equals("id")||key.endsWith("Id"))) return value.toString();
        if(value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if(value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if(value instanceof java.time.LocalDateTime date) return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if(value instanceof java.time.LocalDate date) return date.toString();
        if(value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        return normalize(mapper.convertValue(value,Map.class),key);
    }
    private String camel(String value) {
        var out=new StringBuilder();boolean upper=false;
        for(char c:value.toCharArray()) { if(c=='_') upper=true;else { out.append(upper?Character.toUpperCase(c):c);upper=false; } }
        return out.toString();
    }
}
