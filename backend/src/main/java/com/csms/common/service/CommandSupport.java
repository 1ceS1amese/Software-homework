package com.csms.common.service;

import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

/** 管理写操作共用的严格入参、身份和生成键处理；表名只允许源码常量。 */
@Component
public class CommandSupport {
    public final JdbcTemplate jdbc;
    public CommandSupport(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public static CurrentUserHolder.UserContext require(String... roles) {
        var me = CurrentUserHolder.get();
        if (me == null) throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        if (!Set.of(roles).contains(me.getRole())) throw new BizException(ErrorCode.FORBIDDEN);
        return me;
    }

    public Map<String,Object> row(String table, Long id, boolean lock) {
        if (!Set.of("dept", "major", "term", "course", "teaching_class", "sys_user").contains(table))
            throw new IllegalArgumentException("不允许的业务表");
        var rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE id=? AND deleted=0" + (lock ? " FOR UPDATE" : ""), id);
        if (rows.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND);
        return rows.get(0);
    }

    public long insert(String sql, Object... values) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i=0; i<values.length; i++) statement.setObject(i+1, values[i]);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    public static String text(Map<String,Object> body, String key, int max, boolean required) {
        Object value = body.get(key);
        if (value != null && !(value instanceof String)) throw invalid(key);
        String result = value == null ? "" : value.toString().trim();
        if ((required && result.isBlank()) || result.length()>max) throw invalid(key);
        return result;
    }

    public static long number(Map<String,Object> body, String key, long min, long max, Long fallback) {
        try {
            Object raw=body.get(key)==null?fallback:body.get(key);
            long value = new BigDecimal(raw.toString()).longValueExact();
            if (value<min || value>max) throw invalid(key);
            return value;
        } catch (NullPointerException | NumberFormatException | ArithmeticException e) { throw invalid(key); }
    }

    public static Long optionalId(Map<String,Object> body, String key) {
        return body.get(key)==null || "".equals(body.get(key)) ? null : number(body,key,1,Long.MAX_VALUE,null);
    }

    public static String choice(Map<String,Object> body, String key, String fallback, String... options) {
        String value = body.get(key)==null ? fallback : text(body,key,64,true);
        if (value==null || !Set.of(options).contains(value)) throw invalid(key);
        return value;
    }

    public static BizException invalid(String key) { return new BizException(ErrorCode.PARAM_ERROR, key+" 参数无效，请检查格式与范围"); }
}
