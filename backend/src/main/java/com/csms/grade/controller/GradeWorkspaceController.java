package com.csms.grade.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.error.*;
import com.csms.grade.service.GradeService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.csms.common.service.CommandSupport.require;

@RestController
public class GradeWorkspaceController {
    private final GradeService grades;
    public GradeWorkspaceController(GradeService grades) { this.grades=grades; }
    @GetMapping("/teaching-classes/{id}/grades")
    public ApiResponse<?> list(@PathVariable Long id) {
        var me=require("TEACHER","ADMIN");grades.assertTeacherOwns(id,me.getId(),"ADMIN".equals(me.getRole()));return ApiResponse.success(grades.classGrades(id));
    }
    @PostMapping("/teaching-classes/{id}/grades")
    public ApiResponse<?> save(@PathVariable Long id,@RequestBody ItemsRequest body) {
        var me=require("TEACHER");grades.assertTeacherOwns(id,me.getId(),false);grades.saveDraft(id,body.items());return ApiResponse.success();
    }
    @PutMapping("/grades/{id}")
    public ApiResponse<?> update(@PathVariable Long id,@RequestBody Map<String,Object> item) { grades.updateOne(id,item,require("TEACHER").getId());return ApiResponse.success(); }
    @PostMapping("/teaching-classes/{id}/grades/publish")
    public ApiResponse<?> publish(@PathVariable Long id,@RequestBody Map<String,Object> body) {
        if(!Boolean.TRUE.equals(body.get("confirm"))) throw new BizException(ErrorCode.PARAM_ERROR,"请确认发布成绩");
        var me=require("TEACHER");grades.assertTeacherOwns(id,me.getId(),false);boolean changed=grades.publish(id,me.getId());return new ApiResponse<>(0,changed?"成绩发布成功":"成绩已发布",null);
    }
    @PostMapping("/teaching-classes/{id}/grades/unpublish")
    public ApiResponse<?> unlock(@PathVariable Long id,@RequestBody Map<String,Object> body) {
        require("ADMIN");grades.unlock(id,body.get("reason") instanceof String r?r:null);return ApiResponse.success();
    }
    public record ItemsRequest(List<Map<String,Object>> items) {}
    @GetMapping("/my/grades")
    public ApiResponse<?> mine(@RequestParam(required=false) Long termId) { return ApiResponse.success(grades.myGrades(require("STUDENT").getId(),termId)); }
}
