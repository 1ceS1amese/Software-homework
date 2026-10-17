package com.csms.classroom.service;

import com.csms.audit.service.AuditWriter;
import com.csms.classroom.domain.TeachingClassStateMachine;
import com.csms.classroom.entity.*;
import com.csms.common.enums.TeachingClassStatus;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import static com.csms.common.service.CommandSupport.*;

@Service
public class TeachingClassCommandService {
    private final CommandSupport db;private final AuditWriter audit;private final TeachingClassQueryService query;private final TeachingClassStateMachine machine;
    public TeachingClassCommandService(CommandSupport db,AuditWriter audit,TeachingClassQueryService query,TeachingClassStateMachine machine) { this.db=db;this.audit=audit;this.query=query;this.machine=machine; }

    private Map<String,Object> writable(Long id) {
        var me=require("TEACHER","ADMIN");var row=db.row("teaching_class",id,true);
        if(!"ADMIN".equals(me.getRole()) && ((Number)row.get("teacher_id")).longValue()!=me.getId()) throw new BizException(ErrorCode.FORBIDDEN,"只能维护本人教学班");
        return row;
    }
    private void editable(Map<String,Object> row) {
        if(List.of("CLOSED","CANCELLED").contains(row.get("status"))) throw new BizException(ErrorCode.TC_STATUS_INVALID,"已关闭或取消的教学班不可编辑");
    }
    private int records(Long id) { return db.jdbc.queryForObject("SELECT COUNT(*) FROM enrollment WHERE teaching_class_id=? AND deleted=0",Integer.class,id); }

    @Transactional
    public Map<String,Object> save(Long id,Map<String,Object> input) {
        var me=require("TEACHER","ADMIN");var before=id==null?null:writable(id);
        if(before!=null) editable(before);
        Map<String,Object> body=new HashMap<>();
        if(id!=null) body.putAll(query.detail(id));body.putAll(input);
        if(input.containsKey("status") && !String.valueOf(before==null?"DRAFT":before.get("status")).equals(input.get("status"))) throw new BizException(ErrorCode.TC_STATUS_INVALID,"请使用状态流转操作");
        long courseId=number(body,"courseId",1,Long.MAX_VALUE,null),termId=number(body,"termId",1,Long.MAX_VALUE,null);
        var term=db.row("term",termId,true);var course=db.row("course",courseId,true);
        if("CLOSED".equals(term.get("status")) || !"ACTIVE".equals(course.get("status"))) throw new BizException(ErrorCode.TC_STATUS_INVALID,"学期已结束或课程已停用");
        long teacher="TEACHER".equals(me.getRole())?me.getId():number(body,"teacherId",1,Long.MAX_VALUE,null);
        var user=db.row("sys_user",teacher,true);
        if(!"TEACHER".equals(user.get("user_type")) || !"ACTIVE".equals(user.get("status"))) throw invalid("teacherId");
        String name=text(body,"className",64,true),location=text(body,"location",100,false),description=text(body,"description",500,false);
        long capacity=number(body,"capacity",1,100000,null),start=number(body,"startWeek",1,30,1L),end=number(body,"endWeek",start,30,16L);
        LocalDate open;
        try { open=LocalDate.parse(text(body,"openClassDate",10,true)); } catch(DateTimeParseException e) { throw invalid("openClassDate"); }
        if(before!=null && capacity<((Number)before.get("enrolled_count")).longValue()) throw new BizException(ErrorCode.TC_CAPACITY_INVALID);
        if(before!=null && records(id)>0 && (courseId!=((Number)before.get("course_id")).longValue() || termId!=((Number)before.get("term_id")).longValue()
                || teacher!=((Number)before.get("teacher_id")).longValue() || start!=((Number)before.get("start_week")).longValue()
                || end!=((Number)before.get("end_week")).longValue() || !open.toString().equals(before.get("open_class_date").toString())))
            throw new BizException(ErrorCode.TC_HAS_ENROLLMENTS,"已有选课记录，不可变更课程、学期、教师、周次或开课日期");
        if(before!=null && db.jdbc.queryForObject("SELECT COUNT(*) FROM class_schedule WHERE teaching_class_id=? AND (start_week<? OR end_week>?)",Integer.class,id,start,end)>0) throw invalid("周次（已有排课超出新范围）");
        if(!db.jdbc.queryForList("SELECT id FROM teaching_class WHERE course_id=? AND term_id=? AND class_name=? AND deleted=0 AND id<>? FOR UPDATE",courseId,termId,name,id==null?-1L:id).isEmpty()) throw new BizException(ErrorCode.TC_EXISTS);
        Object credit=before!=null && courseId==((Number)before.get("course_id")).longValue()?before.get("credit"):course.get("credit");
        if(id==null) id=db.insert("INSERT INTO teaching_class(course_id,term_id,teacher_id,class_name,capacity,credit,location,start_week,end_week,open_class_date,description,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,'DRAFT')",courseId,termId,teacher,name,capacity,credit,location,start,end,open,description);
        else db.jdbc.update("UPDATE teaching_class SET course_id=?,term_id=?,teacher_id=?,class_name=?,capacity=?,credit=?,location=?,start_week=?,end_week=?,open_class_date=?,description=?,version=version+1 WHERE id=?",courseId,termId,teacher,name,capacity,credit,location,start,end,open,description,id);
        var after=query.detail(id);audit.record("CLASS",before==null?"CREATE_CLASS":"UPDATE_CLASS","teaching_class",id,before,after);return after;
    }

    @Transactional
    public void delete(Long id) {
        var before=writable(id);
        if(!"ADMIN".equals(require("TEACHER","ADMIN").getRole()) && !"DRAFT".equals(before.get("status"))) throw new BizException(ErrorCode.TC_STATUS_INVALID);
        if(records(id)>0) throw new BizException(ErrorCode.TC_HAS_ENROLLMENTS);
        var snapshot=query.detail(id);db.jdbc.update("UPDATE teaching_class SET deleted=1,version=version+1 WHERE id=?",id);
        db.jdbc.update("DELETE FROM class_schedule WHERE teaching_class_id=?",id);
        audit.record("CLASS","DELETE_CLASS","teaching_class",id,snapshot,Map.of("deleted",1));
    }

    @Transactional
    public void status(Long id,Map<String,Object> body) {
        var before=writable(id);TeachingClassStateMachine.Action action;
        try { action=TeachingClassStateMachine.Action.valueOf(text(body,"action",32,true)); } catch(IllegalArgumentException e) { throw invalid("action"); }
        var entity=new TeachingClass();entity.setStatus(TeachingClassStatus.valueOf(String.valueOf(before.get("status"))));entity.setCapacity(((Number)before.get("capacity")).intValue());
        var times=schedules(id);machine.assertTransit(entity,times.stream().map(ignored->new ClassSchedule()).toList(),action);
        String reason=text(body,"reason",300,false);
        if(action==TeachingClassStateMachine.Action.ROLLBACK_DRAFT) {
            require("ADMIN");if(records(id)>0) throw new BizException(ErrorCode.TC_HAS_ENROLLMENTS);
        }
        if(action==TeachingClassStateMachine.Action.PUBLISH) {
            if("CLOSED".equals(db.row("term",((Number)before.get("term_id")).longValue(),true).get("status"))) throw new BizException(ErrorCode.TC_STATUS_INVALID,"学期已结束");
        }
        if(action==TeachingClassStateMachine.Action.CANCEL) {
            if(records(id)>0 && reason.length()<5) throw new BizException(ErrorCode.PARAM_ERROR,"取消有选课的教学班必须填写至少5字原因");
            if(db.jdbc.queryForObject("SELECT COUNT(*) FROM grade WHERE teaching_class_id=? AND status='PUBLISHED' AND deleted=0",Integer.class,id)>0) throw new BizException(ErrorCode.GRADE_PUBLISHED,"已有已发布成绩，请先由管理员解锁后处理");
            db.jdbc.update("UPDATE enrollment SET status='WITHDRAWN',withdrawn_at=NOW() WHERE teaching_class_id=? AND status='ENROLLED' AND deleted=0",id);
            db.jdbc.update("UPDATE grade SET deleted=1 WHERE teaching_class_id=? AND status='DRAFT' AND deleted=0",id);
            db.jdbc.update("UPDATE teaching_class SET enrolled_count=0 WHERE id=?",id);
        }
        db.jdbc.update("UPDATE teaching_class SET status=?,version=version+1 WHERE id=?",machine.getTargetStatus(action).name(),id);
        audit.record("CLASS","CLASS_"+action.name(),"teaching_class",id,before,Map.of("class",query.detail(id),"reason",reason));
    }

    public List<Map<String,Object>> schedules(Long id) {
        db.row("teaching_class",id,false);
        return db.jdbc.queryForList("SELECT id,teaching_class_id AS teachingClassId,day_of_week AS dayOfWeek,start_section AS startSection,end_section AS endSection,start_minute AS startMinute,end_minute AS endMinute,location,week_desc AS weekDesc,start_week AS startWeek,end_week AS endWeek FROM class_schedule WHERE teaching_class_id=? ORDER BY day_of_week,start_minute",id);
    }

    @Transactional
    public Map<String,Object> schedule(Long id,Long sid,Map<String,Object> body) {
        var tc=writable(id);editable(tc);
        var before=sid==null?null:db.jdbc.queryForList("SELECT * FROM class_schedule WHERE id=? AND teaching_class_id=? FOR UPDATE",sid,id).stream().findFirst().orElseThrow(()->new BizException(ErrorCode.NOT_FOUND));
        long day=number(body,"dayOfWeek",1,7,null),section=number(body,"startSection",1,12,null),last=number(body,"endSection",section,12,null);
        long start=number(body,"startMinute",0,1439,null),end=number(body,"endMinute",start+1,1440,null);
        long week=number(body,"startWeek",((Number)tc.get("start_week")).longValue(),((Number)tc.get("end_week")).longValue(),((Number)tc.get("start_week")).longValue());
        long finalWeek=number(body,"endWeek",week,((Number)tc.get("end_week")).longValue(),((Number)tc.get("end_week")).longValue());
        String location=text(body,"location",100,false),desc=week+"-"+finalWeek+"周";
        db.jdbc.queryForList("SELECT u.id FROM sys_user u JOIN enrollment e ON e.student_id=u.id WHERE e.teaching_class_id=? AND e.status IN ('ENROLLED','COMPLETED') AND e.deleted=0 ORDER BY u.id FOR UPDATE",id);
        if(db.jdbc.queryForObject("SELECT COUNT(*) FROM class_schedule WHERE teaching_class_id=? AND id<>? AND day_of_week=? AND start_week<=? AND end_week>=? AND start_minute<? AND end_minute>?",Integer.class,id,sid==null?-1L:sid,day,finalWeek,week,end,start)>0) throw new BizException(ErrorCode.TC_SCHEDULE_CONFLICT);
        if(db.jdbc.queryForObject("SELECT COUNT(*) FROM enrollment a JOIN enrollment b ON a.student_id=b.student_id AND a.term_id=b.term_id AND a.teaching_class_id<>b.teaching_class_id JOIN class_schedule s ON s.teaching_class_id=b.teaching_class_id WHERE a.teaching_class_id=? AND a.deleted=0 AND b.deleted=0 AND a.status IN ('ENROLLED','COMPLETED') AND b.status IN ('ENROLLED','COMPLETED') AND s.day_of_week=? AND s.start_week<=? AND s.end_week>=? AND s.start_minute<? AND s.end_minute>?",Integer.class,id,day,finalWeek,week,end,start)>0) throw new BizException(ErrorCode.TC_SCHEDULE_CONFLICT,"排课与已选学生的其他课程冲突");
        if(sid==null) sid=db.insert("INSERT INTO class_schedule(teaching_class_id,day_of_week,start_section,end_section,start_minute,end_minute,location,week_desc,start_week,end_week) VALUES(?,?,?,?,?,?,?,?,?,?)",id,day,section,last,start,end,location,desc,week,finalWeek);
        else db.jdbc.update("UPDATE class_schedule SET day_of_week=?,start_section=?,end_section=?,start_minute=?,end_minute=?,location=?,week_desc=?,start_week=?,end_week=? WHERE id=? AND teaching_class_id=?",day,section,last,start,end,location,desc,week,finalWeek,sid,id);
        var after=db.jdbc.queryForMap("SELECT * FROM class_schedule WHERE id=?",sid);
        audit.record("CLASS",before==null?"CREATE_SCHEDULE":"UPDATE_SCHEDULE","class_schedule",sid,before,after);return after;
    }

    @Transactional
    public void deleteSchedule(Long id,Long sid,boolean confirm) {
        var tc=writable(id);editable(tc);
        var rows=db.jdbc.queryForList("SELECT * FROM class_schedule WHERE id=? AND teaching_class_id=?",sid,id);
        if(rows.isEmpty()) throw new BizException(ErrorCode.NOT_FOUND);
        if(records(id)>0 && !confirm) throw new BizException(ErrorCode.TC_STATUS_INVALID,"有选课记录，删除排课需二次确认");
        if("PUBLISHED".equals(tc.get("status")) && schedules(id).size()==1) throw new BizException(ErrorCode.TC_STATUS_INVALID,"已发布教学班必须保留至少一个时段");
        db.jdbc.update("DELETE FROM class_schedule WHERE id=? AND teaching_class_id=?",sid,id);
        audit.record("CLASS","DELETE_SCHEDULE","class_schedule",sid,rows.get(0),Map.of("deleted",true));
    }
}
