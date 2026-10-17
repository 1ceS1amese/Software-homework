package com.csms.base.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Map;
import static com.csms.common.service.CommandSupport.*;

@Service
public class TermCommandService {
    private final CommandSupport db;
    private final AuditWriter audit;
    public TermCommandService(CommandSupport db,AuditWriter audit) { this.db=db;this.audit=audit; }

    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> body) {
        require("ADMIN");
        var before=id==null?null:db.row("term",id,true);
        String code=text(body,"code",32,true),name=text(body,"name",64,true);
        try {
            var start=LocalDate.parse(text(body,"startDate",10,true));
            var end=LocalDate.parse(text(body,"endDate",10,true));
            var enrollStart=dateTime(body,"enrollStart");
            var enrollEnd=dateTime(body,"enrollEnd");
            var withdrawEnd=dateTime(body,"withdrawEnd");
            if(!start.isBefore(end) || start.getYear()<1970 || end.getYear()>2200 || enrollStart.isAfter(enrollEnd)
                    || enrollEnd.isAfter(withdrawEnd) || withdrawEnd.toLocalDate().isAfter(end))
                throw new BizException(ErrorCode.PARAM_ERROR,"学期起止及选退课时间顺序不正确");
            if(body.containsKey("status") && !String.valueOf(before==null?"PLANNED":before.get("status")).equals(body.get("status")))
                throw new BizException(ErrorCode.TC_STATUS_INVALID,"请使用学期状态操作，禁止跳过状态流程");
            try {
                if(id==null) id=db.insert("INSERT INTO term(code,name,start_date,end_date,enroll_start,enroll_end,withdraw_end,status) VALUES(?,?,?,?,?,?,?,'PLANNED')",code,name,start,end,enrollStart,enrollEnd,withdrawEnd);
                else db.jdbc.update("UPDATE term SET code=?,name=?,start_date=?,end_date=?,enroll_start=?,enroll_end=?,withdraw_end=?,version=version+1 WHERE id=?",code,name,start,end,enrollStart,enrollEnd,withdrawEnd,id);
            } catch(DuplicateKeyException e) { throw new BizException(ErrorCode.TERM_CODE_EXISTS); }
        } catch(DateTimeParseException e) { throw new BizException(ErrorCode.PARAM_ERROR,"日期或时间格式无效"); }
        var after=db.row("term",id,false);
        audit.record("BASE",before==null?"CREATE_TERM":"UPDATE_TERM","term",id,before,after);
        return after;
    }

    @Transactional
    public void status(Long id,String next) {
        require("ADMIN");
        var before=db.row("term",id,true);
        String expected=Map.of("PLANNED","ENROLLING","ENROLLING","RUNNING","RUNNING","CLOSED").get(before.get("status"));
        if(expected==null || !expected.equals(next)) throw new BizException(ErrorCode.TC_STATUS_INVALID,"学期只能按计划、选课、授课、结束的顺序流转");
        db.jdbc.update("UPDATE term SET status=?,version=version+1 WHERE id=?",next,id);
        audit.record("BASE","TERM_STATUS","term",id,before,db.row("term",id,false));
    }

    private LocalDateTime dateTime(Map<String,Object> body,String key) {
        return LocalDateTime.parse(text(body,key,19,true).replace(' ','T'));
    }
}
