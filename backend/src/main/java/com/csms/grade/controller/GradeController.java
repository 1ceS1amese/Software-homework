package com.csms.grade.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.grade.service.GradeService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成绩接口（G-01 ~ G-06）。
 */
@RestController
@RequestMapping("/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    /** G-01 教学班成绩表 */
    @GetMapping("/class/{teachingClassId}")
    public ApiResponse<List<Map<String, Object>>> classGrades(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = requireTeacherOrAdmin();
        gradeService.assertTeacherOwns(teachingClassId, me.getId(), "ADMIN".equals(me.getRole()));
        return ApiResponse.success(gradeService.classGrades(teachingClassId));
    }

    /** G-02 批量暂存成绩 */
    @PutMapping("/class/{teachingClassId}")
    public ApiResponse<Void> saveGrades(@PathVariable Long teachingClassId,
                                        @RequestBody List<Map<String, Object>> items) {
        CurrentUserHolder.UserContext me = requireTeacherOrAdmin();
        gradeService.assertTeacherOwns(teachingClassId, me.getId(), "ADMIN".equals(me.getRole()));
        gradeService.saveDraft(teachingClassId, items);
        return ApiResponse.success();
    }

    /** G-03 发布成绩 */
    @PostMapping("/class/{teachingClassId}/publish")
    public ApiResponse<Void> publish(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = requireTeacherOrAdmin();
        gradeService.assertTeacherOwns(teachingClassId, me.getId(), "ADMIN".equals(me.getRole()));
        gradeService.publish(teachingClassId, me.getId());
        return ApiResponse.success();
    }

    /** G-04 管理员解锁成绩 */
    @PostMapping("/class/{teachingClassId}/unlock")
    public ApiResponse<Void> unlock(@PathVariable Long teachingClassId, @RequestBody UnlockRequest request) {
        CurrentUserHolder.UserContext me = requireLogin();
        if (!"ADMIN".equals(me.getRole())) {
            throw new BizException(ErrorCode.GRADE_UNLOCK_FORBIDDEN);
        }
        gradeService.unlock(teachingClassId, request == null ? null : request.getReason());
        return ApiResponse.success();
    }

    /** G-05 学生本人成绩 */
    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myGrades(@RequestParam(required = false) Long termId) {
        CurrentUserHolder.UserContext me = requireLogin();
        return ApiResponse.success(gradeService.myGrades(me.getId(), termId));
    }

    /** G-06 分数段分布 */
    @GetMapping("/class/{teachingClassId}/distribution")
    public ApiResponse<List<Map<String, Object>>> distribution(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = requireTeacherOrAdmin();
        gradeService.assertTeacherOwns(teachingClassId, me.getId(), "ADMIN".equals(me.getRole()));
        return ApiResponse.success(gradeService.distribution(teachingClassId));
    }

    // —————————————————— 辅助 ——————————————————

    private CurrentUserHolder.UserContext requireLogin() {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return me;
    }

    private CurrentUserHolder.UserContext requireTeacherOrAdmin() {
        CurrentUserHolder.UserContext me = requireLogin();
        String role = me.getRole();
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有教师或管理员可以操作成绩");
        }
        return me;
    }

    @Data
    public static class UnlockRequest {
        private String reason;
    }
}
