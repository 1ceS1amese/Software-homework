package com.csms.base.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static com.csms.common.service.CommandSupport.*;

@Service
public class OrganizationService {
    private final CommandSupport db;
    private final AuditWriter audit;
    public OrganizationService(CommandSupport db, AuditWriter audit) { this.db=db; this.audit=audit; }

    @Transactional
    public Map<String,Object> save(String type, Long id, Map<String,Object> body) {
        require("ADMIN");
        var before=id==null ? null : db.row(type,id,true);
        String code=text(body,"code",32,true), name=text(body,"name",64,true);
        String status=choice(body,"status","ACTIVE","ACTIVE","DISABLED");
        Object[] fields;
        if ("dept".equals(type)) {
            fields=new Object[]{code,name,text(body,"leader",50,false),status,number(body,"sortNo",0,100000,0L)};
        } else {
            long dept=number(body,"deptId",1,Long.MAX_VALUE,null);
            if (!"ACTIVE".equals(db.row("dept",dept,true).get("status"))) throw invalid("deptId");
            if (before!=null && ((Number)before.get("dept_id")).longValue()!=dept &&
                    db.jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM sys_user WHERE major_id=? AND deleted=0)+(SELECT COUNT(*) FROM course WHERE major_id=? AND deleted=0)",Integer.class,id,id)>0)
                throw new BizException(ErrorCode.CONFLICT,"专业已有用户或课程，不可变更所属院系");
            fields=new Object[]{code,name,dept,number(body,"degreeYears",1,8,4L),status};
        }
        try {
            if (id==null) {
                id="dept".equals(type) ? db.insert("INSERT INTO dept(code,name,leader,status,sort_no) VALUES(?,?,?,?,?)",fields)
                        : db.insert("INSERT INTO major(code,name,dept_id,degree_years,status) VALUES(?,?,?,?,?)",fields);
            } else {
                var args=java.util.Arrays.copyOf(fields,fields.length+1); args[fields.length]=id;
                db.jdbc.update("dept".equals(type) ? "UPDATE dept SET code=?,name=?,leader=?,status=?,sort_no=?,version=version+1 WHERE id=?"
                        : "UPDATE major SET code=?,name=?,dept_id=?,degree_years=?,status=?,version=version+1 WHERE id=?",args);
            }
        } catch (DuplicateKeyException e) { throw new BizException("dept".equals(type)?ErrorCode.DEPT_CODE_EXISTS:ErrorCode.MAJOR_CODE_EXISTS); }
        var after=db.row(type,id,false);
        audit.record("BASE",(before==null?"CREATE_":"UPDATE_")+type.toUpperCase(),type,id,before,after);
        return after;
    }

    @Transactional
    public void delete(String type, Long id) {
        require("ADMIN");
        var before=db.row(type,id,true);
        int count="dept".equals(type) ? db.jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM major WHERE dept_id=? AND deleted=0)+(SELECT COUNT(*) FROM sys_user WHERE dept_id=? AND deleted=0)+(SELECT COUNT(*) FROM course WHERE dept_id=? AND deleted=0)",Integer.class,id,id,id)
                : db.jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM sys_user WHERE major_id=? AND deleted=0)+(SELECT COUNT(*) FROM course WHERE major_id=? AND deleted=0)",Integer.class,id,id);
        if(count>0) throw new BizException("dept".equals(type)?ErrorCode.DEPT_HAS_RECORDS:ErrorCode.CONFLICT,"存在关联专业、用户或课程，不能删除");
        db.jdbc.update("UPDATE "+type+" SET deleted=1,version=version+1 WHERE id=?",id);
        audit.record("BASE","DELETE_"+type.toUpperCase(),type,id,before,Map.of("deleted",1));
    }
}
