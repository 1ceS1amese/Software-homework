package com.csms.base.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.csms.common.service.CommandSupport.*;

@Service
public class PrerequisiteService {
    private final CommandSupport db; private final AuditWriter audit;
    public PrerequisiteService(CommandSupport db,AuditWriter audit) { this.db=db;this.audit=audit; }
    public List<Map<String,Object>> list(Long id) {
        db.row("course",id,false);
        return db.jdbc.queryForList("SELECT cp.prereq_course_id AS prereqCourseId,c.course_code AS courseCode,c.name,cp.requirement FROM course_prereq cp JOIN course c ON c.id=cp.prereq_course_id WHERE cp.course_id=? AND c.deleted=0 ORDER BY c.course_code",id);
    }
    @Transactional
    public void add(Long id,Map<String,Object> body) {
        require("ADMIN");
        // 同一有向图统一锁序，防止并发添加 A→B 与 B→A 同时绕过环检测。
        db.jdbc.queryForList("SELECT id FROM course WHERE deleted=0 ORDER BY id FOR UPDATE");
        db.row("course",id,false);
        long prerequisite=number(body,"prereqCourseId",1,Long.MAX_VALUE,null);
        db.row("course",prerequisite,false);
        choice(body,"requirement","REQUIRED","REQUIRED");
        Map<Long,List<Long>> graph=new HashMap<>();
        for(var row:db.jdbc.queryForList("SELECT course_id,prereq_course_id FROM course_prereq"))
            graph.computeIfAbsent(((Number)row.get("course_id")).longValue(),ignored->new ArrayList<>()).add(((Number)row.get("prereq_course_id")).longValue());
        Deque<Long> pending=new ArrayDeque<>();Set<Long> visited=new HashSet<>();pending.add(prerequisite);
        while(!pending.isEmpty()) {
            long current=pending.removeFirst();
            if(current==id) throw new BizException(ErrorCode.COURSE_PREREQ_CYCLE);
            if(visited.add(current)) pending.addAll(graph.getOrDefault(current,List.of()));
        }
        var before=list(id);
        try { db.jdbc.update("INSERT INTO course_prereq(course_id,prereq_course_id,requirement) VALUES(?,?,'REQUIRED')",id,prerequisite); }
        catch(DuplicateKeyException e) { throw new BizException(ErrorCode.CONFLICT,"先修关系已存在"); }
        audit.record("BASE","ADD_PREREQUISITE","course",id,before,list(id));
    }
    @Transactional
    public void delete(Long id,Long prerequisite) {
        require("ADMIN");db.row("course",id,true);var before=list(id);
        if(db.jdbc.update("DELETE FROM course_prereq WHERE course_id=? AND prereq_course_id=?",id,prerequisite)==0)
            throw new BizException(ErrorCode.NOT_FOUND,"先修关系不存在");
        audit.record("BASE","DELETE_PREREQUISITE","course",id,before,list(id));
    }
}
