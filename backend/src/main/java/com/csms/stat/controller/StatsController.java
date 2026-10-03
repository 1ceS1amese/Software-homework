package com.csms.stat.controller;

import com.csms.common.api.ApiResponse;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.stat.service.StatQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计接口（S-01 / S-02 / S-04 / S-06）。
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatQueryService statQueryService;

    public StatsController(StatQueryService statQueryService) {
        this.statQueryService = statQueryService;
    }

    /** S-01 选课总览 */
    @GetMapping("/enrollment/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(required = false) Long termId) {
        requireLogin();
        return ApiResponse.success(statQueryService.overview(termId));
    }

    /** S-02 按课程统计 */
    @GetMapping("/enrollment/by-course")
    public ApiResponse<List<Map<String, Object>>> byCourse(@RequestParam(required = false) Long termId,
                                                           @RequestParam(required = false) Integer limit) {
        requireLogin();
        return ApiResponse.success(statQueryService.byCourse(termId, limit));
    }

    /** S-04 满员度分析 */
    @GetMapping("/capacity-analysis")
    public ApiResponse<Map<String, Object>> capacityAnalysis(@RequestParam(required = false) Long termId) {
        requireLogin();
        return ApiResponse.success(statQueryService.capacityAnalysis(termId));
    }

    /** S-06 学生学分汇总（本人或管理员） */
    @GetMapping("/students/{studentId}/summary")
    public ApiResponse<Map<String, Object>> studentSummary(@PathVariable Long studentId,
                                                           @RequestParam(required = false) Long termId) {
        CurrentUserHolder.UserContext me = requireLogin();
        boolean isAdmin = "ADMIN".equals(me.getRole());
        boolean isSelf = me.getId().equals(studentId);
        if (!isAdmin && !isSelf) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能查看本人的学分统计");
        }
        return ApiResponse.success(statQueryService.studentSummary(studentId, termId));
    }

    private CurrentUserHolder.UserContext requireLogin() {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return me;
    }
}
