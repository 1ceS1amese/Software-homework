package com.csms.base.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import static com.csms.common.service.CommandSupport.*;

@Service
public class ConfigCommandService {
    private final CommandSupport db;private final AuditWriter audit;private final ObjectMapper mapper;
    public ConfigCommandService(CommandSupport db,AuditWriter audit,ObjectMapper mapper) { this.db=db;this.audit=audit;this.mapper=mapper; }
    @Transactional
    public void update(String key,String value,Map<String,String> related) {
        require("ADMIN");
        var configs=db.jdbc.queryForList("SELECT * FROM sys_config ORDER BY id FOR UPDATE");
        var before=configs.stream().filter(c->key.equals(c.get("config_key")) || key.equals(String.valueOf(c.get("id")))).findFirst().orElseThrow(()->new BizException(ErrorCode.NOT_FOUND));
        if(((Number)before.get("editable")).intValue()!=1) throw new BizException(ErrorCode.SYS_CONFIG_READONLY);
        if(value==null || value.isBlank() || value.length()>255) throw invalid("configValue");
        value=value.trim();String name=String.valueOf(before.get("config_key"));
        if(name.startsWith("grade.weight.")) {
            List<String> keys=List.of("grade.weight.regular","grade.weight.midterm","grade.weight.final");
            Map<String,String> proposed=new LinkedHashMap<>();
            for(String k:keys) proposed.put(k,String.valueOf(configs.stream().filter(c->k.equals(c.get("config_key"))).findFirst().orElseThrow().get("config_value")));
            proposed.put(name,value);
            if(related!=null) for(var entry:related.entrySet()) {
                if(!keys.contains(entry.getKey()) || name.equals(entry.getKey())) throw invalid("relatedValues");
                proposed.put(entry.getKey(),entry.getValue());
            }
            try {
                BigDecimal sum=BigDecimal.ZERO;
                for(String v:proposed.values()) { var n=new BigDecimal(v);if(n.signum()<0 || n.compareTo(BigDecimal.ONE)>0 || n.scale()>4) throw invalid("成绩权重");sum=sum.add(n); }
                if(sum.compareTo(BigDecimal.ONE)!=0) throw invalid("成绩权重合计必须为1");
            } catch(NumberFormatException | NullPointerException e) { throw invalid("成绩权重"); }
            for(var entry:proposed.entrySet()) db.jdbc.update("UPDATE sys_config SET config_value=?,updated_by=? WHERE config_key=? AND editable=1",entry.getValue(),require("ADMIN").getId(),entry.getKey());
            audit.record("BASE","UPDATE_GRADE_WEIGHTS","sys_config",((Number)before.get("id")).longValue(),configs.stream().filter(c->keys.contains(c.get("config_key"))).toList(),proposed);
            return;
        }
        if(related!=null && !related.isEmpty()) throw invalid("relatedValues");
        try {
            switch(String.valueOf(before.get("value_type"))) {
                case "NUMBER" -> {
                    var decimal=new BigDecimal(value);
                    if(decimal.signum()<0 || decimal.scale()>4 || decimal.compareTo(new BigDecimal("10000"))>0) throw invalid("configValue");
                    if(name.startsWith("grade.weight.") && decimal.compareTo(BigDecimal.ONE)>0) throw invalid("成绩权重");
                    if(name.equals("grade.pass_score") && decimal.compareTo(new BigDecimal("100"))>0) throw invalid("及格线");
                    String counterpart=name.equals("enroll.min_credit_per_term")?"enroll.max_credit_per_term":name.equals("enroll.max_credit_per_term")?"enroll.min_credit_per_term":null;
                    if(counterpart!=null) {
                        var other=new BigDecimal(String.valueOf(configs.stream().filter(c->counterpart.equals(c.get("config_key"))).findFirst().orElseThrow().get("config_value")));
                        if(name.contains(".min_")?decimal.compareTo(other)>0:decimal.compareTo(other)<0) throw invalid("学分上下限");
                    }
                    value=decimal.stripTrailingZeros().toPlainString();
                }
                case "BOOL" -> { if(!Set.of("true","false").contains(value)) throw invalid("布尔参数"); }
                case "JSON" -> { if(mapper.readTree(value)==null) throw invalid("JSON参数"); }
                case "STRING" -> { }
                default -> throw invalid("参数类型");
            }
        } catch(java.io.IOException | NumberFormatException e) { throw invalid("configValue"); }
        if(name.equals("grade.point.mapping_mode") && !value.equals("LINEAR")) throw invalid("绩点映射（当前实现仅支持 LINEAR）");
        if(name.equals("enroll.withdraw_deadline_mode") && !Set.of("TERM_ENROLL_END","CUSTOM").contains(value)) throw invalid("退课截止模式");
        db.jdbc.update("UPDATE sys_config SET config_value=?,updated_by=? WHERE id=?",value,require("ADMIN").getId(),before.get("id"));
        audit.record("BASE","UPDATE_CONFIG","sys_config",((Number)before.get("id")).longValue(),before,Map.of("configKey",name,"configValue",value));
    }
}
