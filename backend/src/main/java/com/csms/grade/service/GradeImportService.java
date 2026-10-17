package com.csms.grade.service;

import com.csms.audit.service.AuditWriter;
import com.csms.common.error.*;
import com.csms.common.service.CommandSupport;
import com.csms.grade.domain.CsvGradeParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.csms.common.service.CommandSupport.*;

@Service
public class GradeImportService {
    public record Preview(List<Map<String,Object>> items,String token,long expiresAt) {}
    private record Plan(Long owner,Long classId,List<Map<String,Object>> items,String fingerprint,long expiresAt,AtomicBoolean busy) {}
    private final ConcurrentHashMap<String,Plan> plans=new ConcurrentHashMap<>();
    private final CommandSupport db;private final GradeService grades;private final AuditWriter audit;private final ObjectMapper mapper;private final Clock clock;
    @Autowired public GradeImportService(CommandSupport db,GradeService grades,AuditWriter audit,ObjectMapper mapper) { this(db,grades,audit,mapper,Clock.systemUTC()); }
    GradeImportService(CommandSupport db,GradeService grades,AuditWriter audit,ObjectMapper mapper,Clock clock) { this.db=db;this.grades=grades;this.audit=audit;this.mapper=mapper;this.clock=clock; }

    @Transactional(readOnly=true)
    public Preview preview(Long classId,MultipartFile file) {
        var me=require("TEACHER");grades.assertTeacherOwns(classId,me.getId(),false);
        if(file==null || file.isEmpty() || file.getSize()>1024*1024) throw new BizException(ErrorCode.GRADE_IMPORT_INVALID,"请上传不超过1MB的UTF-8 CSV文件");
        List<List<String>> rows;
        try { rows=CsvGradeParser.parse(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.getBytes())).toString()); }
        catch(java.io.IOException e) { throw new BizException(ErrorCode.GRADE_IMPORT_INVALID,"CSV必须使用UTF-8编码"); }
        if(rows.size()<2 || !rows.get(0).stream().map(String::trim).toList().equals(List.of("enrollmentId","regularScore","midtermScore","finalScore"))) throw new BizException(ErrorCode.GRADE_IMPORT_INVALID,"CSV列名必须为 enrollmentId,regularScore,midtermScore,finalScore，且至少一条记录");
        var current=grades.classGrades(classId);grades.assertEditable(classId);
        Map<Long,Map<String,Object>> byId=new HashMap<>();for(var r:current) byId.put(((Number)r.get("enrollmentId")).longValue(),r);
        Map<Long,Integer> frequencies=new HashMap<>();
        for(var r:rows.subList(1,rows.size())) { Long id=r.isEmpty()?null:grades.toLong(r.get(0).trim());if(id!=null) frequencies.merge(id,1,Integer::sum); }
        List<Map<String,Object>> differences=new ArrayList<>(),items=new ArrayList<>();boolean valid=true;
        for(int i=1;i<rows.size();i++) {
            var row=rows.get(i);Long id=row.isEmpty()?null:grades.toLong(row.get(0).trim());
            Map<String,Object> after=new LinkedHashMap<>(),difference=new LinkedHashMap<>();String message="校验通过";boolean accepted=true;
            if(row.size()!=4 || id==null || id<=0) { accepted=false;message="列数或选课关系ID无效"; }
            else if(!byId.containsKey(id)) { accepted=false;message="选课关系不属于当前教学班的有效学生"; }
            else if(frequencies.getOrDefault(id,0)>1) { accepted=false;message="同一选课关系在文件中重复"; }
            else {
                try { for(int j=1;j<4;j++) { var score=grades.toDecimal(row.get(j).trim());grades.validate(score);after.put(List.of("regularScore","midtermScore","finalScore").get(j-1),score); } }
                catch(BizException e) { accepted=false;message="成绩须为0到100且最多两位小数；空单元格表示暂不录入"; }
            }
            difference.put("rowNumber",i+1);difference.put("enrollmentId",id);difference.put("before",byId.containsKey(id)?scores(byId.get(id)):null);difference.put("after",after);difference.put("valid",accepted);difference.put("message",message);differences.add(difference);
            if(accepted) { var item=new LinkedHashMap<>(after);item.put("enrollmentId",id);items.add(item); } else valid=false;
        }
        if(!valid) return new Preview(differences,"",0);
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        long expires=clock.millis()+600_000;
        synchronized(plans) {
            plans.entrySet().removeIf(entry->entry.getValue().expiresAt()<clock.millis());
            var own=plans.entrySet().stream().filter(e->e.getValue().owner().equals(me.getId())).sorted(Comparator.comparingLong(e->e.getValue().expiresAt())).toList();
            if(own.size()>=3) plans.remove(own.get(0).getKey(),own.get(0).getValue());
            if(plans.size()>=64) throw new BizException(ErrorCode.SYSTEM_BUSY,"导入预览繁忙，请稍后重试");
            plans.put(token,new Plan(me.getId(),classId,items,fingerprint(classId,current),expires,new AtomicBoolean()));
        }
        return new Preview(differences,token,expires);
    }

    @Transactional
    public Map<String,Object> confirm(Long classId,String token) {
        var me=require("TEACHER");grades.assertTeacherOwns(classId,me.getId(),false);
        var plan=token==null?null:plans.get(token);
        if(plan==null || plan.expiresAt()<clock.millis()) { if(token!=null) plans.remove(token);throw new BizException(ErrorCode.CONFLICT,"预览已失效，请重新上传并核对"); }
        if(!plan.owner().equals(me.getId()) || !plan.classId().equals(classId)) throw new BizException(ErrorCode.FORBIDDEN,"不能确认其他教师或教学班的导入");
        grades.lockClass(classId);grades.assertEditable(classId);
        if(!plan.fingerprint().equals(fingerprint(classId,grades.classGrades(classId)))) throw new BizException(ErrorCode.CONFLICT,"成绩、学生名单或参数已改变，请重新预览");
        if(!plan.busy().compareAndSet(false,true)) throw new BizException(ErrorCode.CONFLICT,"该预览已确认或正在处理");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status==STATUS_COMMITTED) plans.remove(token,plan);else plan.busy().set(false); }
        });
        grades.saveDraft(classId,plan.items());
        audit.record("GRADE","IMPORT_GRADES","teaching_class",classId,null,Map.of("successCount",plan.items().size(),"failureCount",0));
        return Map.of("successCount",plan.items().size(),"failureCount",0);
    }

    private Map<String,Object> scores(Map<String,Object> row) { var result=new LinkedHashMap<String,Object>();for(String k:List.of("regularScore","midtermScore","finalScore","totalScore")) result.put(k,row.get(k));return result; }
    private String fingerprint(Long classId,List<Map<String,Object>> rows) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(List.of(rows,db.jdbc.queryForList("SELECT config_key,config_value FROM sys_config WHERE config_key LIKE 'grade.%' ORDER BY config_key"))))); }
        catch(java.io.IOException | NoSuchAlgorithmException e) { throw new IllegalStateException("导入快照校验失败",e); }
    }
}
