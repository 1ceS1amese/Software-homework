package com.csms.user.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import com.csms.security.UserAccessRegistry;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;
import static com.csms.common.service.CommandSupport.*;

@Service
public class UserCommandService {
    private final CommandSupport db;
    private final AuditWriter audit;
    private final PasswordEncoder encoder;
    private final UserAccessRegistry access;
    public UserCommandService(CommandSupport db,AuditWriter audit,PasswordEncoder encoder,UserAccessRegistry access) { this.db=db;this.audit=audit;this.encoder=encoder;this.access=access; }
    private Map<String,Object> safe(Map<String,Object> row) { var copy=new LinkedHashMap<>(row);copy.remove("password");return copy; }
    private String initialPassword() { byte[] bytes=new byte[18];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }

    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> body) {
        require("ADMIN");
        var before=id==null?null:db.row("sys_user",id,true);
        if(body.containsKey("password")) throw invalid("password（请使用专用改密操作）");
        String username=text(body,"username",50,true),name=text(body,"realName",50,true);
        if(!username.matches("[\\p{L}\\p{N}_.@-]{3,50}")) throw invalid("username");
        String role=choice(body,"userType",before==null?"STUDENT":String.valueOf(before.get("user_type")),"STUDENT","TEACHER","ADMIN");
        if(before!=null && !role.equals(before.get("user_type"))) throw invalid("userType（请使用角色分配）");
        String gender=choice(body,"gender","UNKNOWN","MALE","FEMALE","UNKNOWN");
        String phone=text(body,"phone",20,false),email=text(body,"email",100,false);
        if(!phone.matches("[+0-9 ()-]*") || (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) throw invalid("联系方式");
        Long dept=optionalId(body,"deptId"),major=optionalId(body,"majorId");
        if(dept!=null && !"ACTIVE".equals(db.row("dept",dept,true).get("status"))) throw invalid("deptId");
        if(major!=null) {
            var m=db.row("major",major,true);
            if(dept==null || ((Number)m.get("dept_id")).longValue()!=dept || !"ACTIVE".equals(m.get("status"))) throw invalid("majorId");
        }
        String password=id==null?initialPassword():null;
        try {
            if(id==null) {
                id=db.insert("INSERT INTO sys_user(username,password,real_name,user_type,dept_id,major_id,gender,phone,email,status,must_change_pwd) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVE',1)",username,encoder.encode(password),name,role,dept,major,gender,phone,email);
                assign(id,role);
            } else {
                if(id==1L && !username.equals(before.get("username"))) throw new BizException(ErrorCode.USER_BUILTIN,"内置管理员登录名不可修改");
                db.jdbc.update("UPDATE sys_user SET username=?,real_name=?,dept_id=?,major_id=?,gender=?,phone=?,email=?,version=version+1 WHERE id=?",username,name,dept,major,gender,phone,email,id);
            }
        } catch(DuplicateKeyException e) {
            int deleted=db.jdbc.queryForObject("SELECT COALESCE(MAX(deleted),0) FROM sys_user WHERE username=?",Integer.class,username);
            throw new BizException(deleted==1?ErrorCode.USER_EXISTS_DELETED:ErrorCode.USER_EXISTS);
        }
        var after=safe(db.row("sys_user",id,false));
        audit.record("USER",before==null?"CREATE_USER":"UPDATE_USER","sys_user",id,before==null?null:safe(before),after);
        access.changed(id);
        var result=new LinkedHashMap<>(after);
        if(password!=null) result.put("initialPassword",password);
        return result;
    }

    @Transactional
    public void delete(Long id) {
        protect(id);
        var before=db.row("sys_user",id,true);
        if(references(id)>0) throw new BizException(ErrorCode.TC_HAS_ENROLLMENTS,"用户已有选课、成绩或授课记录，不能删除");
        db.jdbc.update("UPDATE sys_user SET deleted=1,version=version+1 WHERE id=?",id);
        db.jdbc.update("DELETE FROM sys_user_role WHERE user_id=?",id);
        audit.record("USER","DELETE_USER","sys_user",id,safe(before),Map.of("deleted",1));access.changed(id);
    }

    @Transactional
    public Map<String,String> reset(Long id) {
        require("ADMIN");db.row("sys_user",id,true);
        if(id.equals(require("ADMIN").getId())) throw new BizException(ErrorCode.USER_BUILTIN,"请在个人中心修改自己的密码");
        String password=initialPassword();
        db.jdbc.update("UPDATE sys_user SET password=?,must_change_pwd=1,fail_count=0,lock_until=NULL,version=version+1 WHERE id=?",encoder.encode(password),id);
        audit.record("USER","RESET_PASSWORD","sys_user",id,null,Map.of("mustChangePwd",true));access.changed(id);
        return Map.of("initialPassword",password);
    }

    @Transactional
    public void status(Long id,String status) {
        require("ADMIN");
        if(!Set.of("ACTIVE","DISABLED").contains(status)) throw invalid("status");
        var before=db.row("sys_user",id,true);
        if("DISABLED".equals(status)) protect(id);
        db.jdbc.update("UPDATE sys_user SET status=?,version=version+1 WHERE id=?",status,id);
        audit.record("USER","USER_STATUS","sys_user",id,safe(before),Map.of("status",status));access.changed(id);
    }

    @Transactional
    public void roles(Long id,Map<String,Object> body) {
        require("ADMIN");
        if(!(body.get("roleCodes") instanceof List<?> codes) || codes.size()!=1 || !(codes.get(0) instanceof String)) throw invalid("roleCodes（必须且仅能选择一个角色）");
        String role=(String)((List<?>)body.get("roleCodes")).get(0);
        if(!Set.of("ADMIN","TEACHER","STUDENT").contains(role)) throw invalid("roleCodes");
        var before=db.row("sys_user",id,true);
        if(!role.equals(before.get("user_type"))) {
            protect(id);
            if(references(id)>0) throw new BizException(ErrorCode.CONFLICT,"用户已有业务记录，不能改变身份");
        }
        assign(id,role);
        db.jdbc.update("UPDATE sys_user SET user_type=?,version=version+1 WHERE id=?",role,id);
        audit.record("USER","USER_ROLES","sys_user",id,safe(before),Map.of("role",role));access.changed(id);
    }

    private void protect(Long id) {
        var me=require("ADMIN");
        if(id==1L || id.equals(me.getId())) throw new BizException(ErrorCode.USER_BUILTIN,"内置管理员或当前账号不允许此操作");
        db.jdbc.queryForList("SELECT id FROM sys_user WHERE user_type='ADMIN' AND status='ACTIVE' AND deleted=0 FOR UPDATE");
        var user=db.row("sys_user",id,true);
        if("ADMIN".equals(user.get("user_type")) && db.jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE user_type='ADMIN' AND status='ACTIVE' AND deleted=0",Integer.class)<=1)
            throw new BizException(ErrorCode.USER_BUILTIN,"必须保留至少一名有效管理员");
    }
    private int references(Long id) { return db.jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM enrollment WHERE student_id=? AND deleted=0)+(SELECT COUNT(*) FROM grade WHERE student_id=? AND deleted=0)+(SELECT COUNT(*) FROM teaching_class WHERE teacher_id=? AND deleted=0)",Integer.class,id,id,id); }
    private void assign(Long id,String role) {
        db.jdbc.update("DELETE FROM sys_user_role WHERE user_id=?",id);
        int count=db.jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE code=? AND deleted=0",id,role);
        if(count!=1) throw new BizException(ErrorCode.PARAM_ERROR,"角色配置不可用");
    }
}
