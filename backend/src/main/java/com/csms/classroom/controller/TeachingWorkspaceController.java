package com.csms.classroom.controller;

import com.csms.classroom.service.TeachingClassQueryService;
import com.csms.common.api.*;
import com.csms.enroll.service.EnrollmentService;
import com.csms.grade.service.GradeService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.csms.common.service.CommandSupport.require;

@RestController
public class TeachingWorkspaceController {
    private final TeachingClassQueryService classes;private final EnrollmentService enrollments;private final GradeService grades;
    public TeachingWorkspaceController(TeachingClassQueryService classes,EnrollmentService enrollments,GradeService grades) { this.classes=classes;this.enrollments=enrollments;this.grades=grades; }
    @GetMapping("/my/teaching-classes")
    public ApiResponse<?> mine(@RequestParam(required=false) Long termId) {
        return ApiResponse.success(classes.byTeacher(require("TEACHER").getId(),termId));
    }
    @GetMapping({"/teaching-classes/{id}/students","/my/teaching-classes/{id}/students"})
    public ApiResponse<?> students(@PathVariable Long id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,jakarta.servlet.http.HttpServletRequest request) {
        var me=request.getRequestURI().contains("/my/")?require("TEACHER"):require("TEACHER","ADMIN");
        enrollments.assertCanReadClass(id,me.getId(),"ADMIN".equals(me.getRole()));
        var rows=enrollments.classStudents(id).stream().filter(row->status==null||status.equals(row.get("enrollStatus")))
            .filter(row->keyword==null||keyword.isBlank()||(row.get("studentName")+" "+row.get("studentUsername")).toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))).toList();
        int p=Math.max(1,page),s=Math.min(100,Math.max(1,size));long offset=(p-1L)*s;
        return ApiResponse.success(new PageResult<>(rows.subList((int)Math.min(offset,rows.size()),(int)Math.min(offset+s,rows.size())),rows.size(),p,s));
    }
    @GetMapping("/my/teaching-classes/{id}/roster")
    public ApiResponse<?> roster(@PathVariable Long id) {
        var me=require("TEACHER");enrollments.assertCanReadClass(id,me.getId(),false);
        return ApiResponse.success(grades.classGrades(id));
    }
}
