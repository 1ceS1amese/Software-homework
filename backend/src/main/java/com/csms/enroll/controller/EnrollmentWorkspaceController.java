package com.csms.enroll.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.error.*;
import com.csms.enroll.service.EnrollmentService;
import com.csms.enroll.domain.TimetableBuilder;
import com.csms.stat.service.StatQueryService;
import com.csms.classroom.service.TeachingClassQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.csms.common.service.CommandSupport.*;

@RestController
public class EnrollmentWorkspaceController {
    private final EnrollmentService enrollments;private final StatQueryService stats;private final TeachingClassQueryService classes;private final com.csms.base.service.BaseDataQueryService base;private final com.csms.audit.service.AuditWriter audit;
    public EnrollmentWorkspaceController(EnrollmentService enrollments,StatQueryService stats,TeachingClassQueryService classes,com.csms.base.service.BaseDataQueryService base,com.csms.audit.service.AuditWriter audit) { this.enrollments=enrollments;this.stats=stats;this.classes=classes;this.base=base;this.audit=audit; }
    @GetMapping("/my/enrollments")
    public ApiResponse<?> mine(@RequestParam(required=false) Long termId,@RequestParam(required=false) String status,@RequestParam(required=false) Long courseId,@RequestParam(required=false) String keyword) {
        return ApiResponse.success(enrollments.myEnrollments(require("STUDENT").getId(),termId,status,courseId,keyword));
    }
    @GetMapping("/my/timetable")
    public ApiResponse<?> timetable(@RequestParam(required=false) Long termId,@RequestParam(required=false) Integer week) {
        var me=require("STUDENT");
        if(week!=null&&(week<1||week>30)) throw new BizException(ErrorCode.PARAM_ERROR,"周次须在1至30之间");
        Long term=termId==null?classes.activeTermId():termId;
        var result=TimetableBuilder.build(enrollments.mySchedule(me.getId(),term),week);
        result.put("term",base.terms().stream().filter(t->java.util.Objects.equals(((Number)t.get("id")).longValue(),term)).findFirst().orElse(null));
        return ApiResponse.success(result);
    }
    @GetMapping("/my/enrollments/statistics")
    public ApiResponse<?> statistics(@RequestParam(required=false) Long termId) { return ApiResponse.success(stats.studentSummary(require("STUDENT").getId(),termId)); }
    @PostMapping("/enrollments/check")
    public ApiResponse<?> check(@RequestBody Map<String,Object> body) { return ApiResponse.success(enrollments.precheck(require("STUDENT").getId(),number(body,"teachingClassId",1,Long.MAX_VALUE,null))); }
    @PostMapping("/enrollments/batch")
    public ApiResponse<?> batch(@RequestBody Map<String,Object> body) {
        var me=require("STUDENT");
        if(!(body.get("teachingClassIds") instanceof List<?> ids)||ids.isEmpty()||ids.size()>50) throw new BizException(ErrorCode.PARAM_ERROR,"须提交1至50个教学班ID");
        List<Long> parsed=new ArrayList<>();
        for(Object id:ids) { if(id==null) throw new BizException(ErrorCode.PARAM_ERROR,"教学班ID不能为空");parsed.add(number(Map.of("id",id),"id",1,Long.MAX_VALUE,null)); }
        List<Object> successes=new ArrayList<>(),failures=new ArrayList<>();
        // 门面每次调用独立的事务 Bean；某条失败不回滚其他成功记录。
        for(Long id:parsed) try { successes.add(enrollments.enroll(me.getId(),id)); }
            catch(BizException e) {
                audit.recordAs(me,"ENROLL","ENROLL_BATCH_FAILED","teaching_class",id,null,Map.of("reasonCode",e.getErrorCode().getCode()),"FAIL",com.csms.audit.service.AuditWriter.request());
                failures.add(Map.of("teachingClassId",id,"code",e.getErrorCode().getCode(),"message",e.getMessage())); }
        return ApiResponse.success(Map.of("successes",successes,"failures",failures,"successCount",successes.size(),"failureCount",failures.size()));
    }
}
