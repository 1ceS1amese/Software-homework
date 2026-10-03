package com.csms.enroll.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.enroll.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 选课接口（E-01 ~ E-06）。
 *
 * <p>当前用户一律从 {@link CurrentUserHolder} 取（由 JwtFilter 填充），不再硬编码。
 */
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /** E-01 选课 */
    @PostMapping
    public ApiResponse<Map<String, Object>> enroll(@Valid @RequestBody EnrollRequest request) {
        CurrentUserHolder.UserContext me = currentUser();
        return ApiResponse.success(enrollmentService.enroll(me.getId(), request.getTeachingClassId()));
    }

    /** E-02 退课（按教学班 ID，幂等） */
    @DeleteMapping("/{teachingClassId}")
    public ApiResponse<Void> withdraw(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = currentUser();
        enrollmentService.withdraw(me.getId(), teachingClassId);
        return ApiResponse.success();
    }

    /** E-03 我的选课 */
    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myEnrollments(@RequestParam(required = false) Long termId) {
        CurrentUserHolder.UserContext me = currentUser();
        return ApiResponse.success(enrollmentService.myEnrollments(me.getId(), termId));
    }

    /** E-04 我的课表 */
    @GetMapping("/my/schedule")
    public ApiResponse<List<Map<String, Object>>> mySchedule(@RequestParam(required = false) Long termId) {
        CurrentUserHolder.UserContext me = currentUser();
        return ApiResponse.success(enrollmentService.mySchedule(me.getId(), termId));
    }

    /** E-05 选课预检（不占名额） */
    @GetMapping("/precheck/{teachingClassId}")
    public ApiResponse<Map<String, Object>> precheck(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = currentUser();
        return ApiResponse.success(enrollmentService.precheck(me.getId(), teachingClassId));
    }

    /** E-06 教学班选课名单（教师本人 / 管理员） */
    @GetMapping("/class/{teachingClassId}/students")
    public ApiResponse<List<Map<String, Object>>> classStudents(@PathVariable Long teachingClassId) {
        CurrentUserHolder.UserContext me = currentUser();
        requireTeacherOrAdmin(me);
        return ApiResponse.success(enrollmentService.classStudents(teachingClassId));
    }

    /** E-08 管理员代选课 */
    @PostMapping("/admin-enroll")
    public ApiResponse<Map<String, Object>> adminEnroll(@Valid @RequestBody AdminEnrollRequest request) {
        CurrentUserHolder.UserContext me = currentUser();
        if (!"ADMIN".equals(me.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有管理员可以代选课");
        }
        return ApiResponse.success(enrollmentService.enroll(request.getStudentId(), request.getTeachingClassId()));
    }

    // —————————————————— 辅助 ——————————————————

    private CurrentUserHolder.UserContext currentUser() {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return me;
    }

    private void requireTeacherOrAdmin(CurrentUserHolder.UserContext me) {
        String role = me.getRole();
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有教师或管理员可以查看选课名单");
        }
    }

    @Data
    public static class EnrollRequest {
        @NotNull(message = "教学班 ID 不能为空")
        private Long teachingClassId;
    }

    @Data
    public static class AdminEnrollRequest {
        @NotNull(message = "学生 ID 不能为空")
        private Long studentId;
        @NotNull(message = "教学班 ID 不能为空")
        private Long teachingClassId;
    }
}
