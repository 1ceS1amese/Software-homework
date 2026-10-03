package com.csms.base.controller;

import com.csms.base.service.BaseDataQueryService;
import com.csms.common.api.ApiResponse;
import com.csms.common.api.PageResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 基础数据接口（B-01 ~ B-21 的只读部分）。
 *
 * <p>写操作（新建/编辑/删除）在 M5 里程碑补齐；当前前端对应按钮本身也尚未接线，
 * 因此本期只保证查询可用，避免页面因 404 而空白。
 */
@RestController
public class BaseDataController {

    private final BaseDataQueryService queryService;

    public BaseDataController(BaseDataQueryService queryService) {
        this.queryService = queryService;
    }

    // —— B-01 院系 ——
    @GetMapping("/depts")
    public ApiResponse<List<Map<String, Object>>> depts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(queryService.depts(keyword));
    }

    // —— B-05 专业 ——
    @GetMapping("/majors")
    public ApiResponse<List<Map<String, Object>>> majors(@RequestParam(required = false) Long deptId,
                                                         @RequestParam(required = false) String keyword) {
        return ApiResponse.success(queryService.majors(deptId, keyword));
    }

    // —— B-09 学期 ——
    @GetMapping("/terms")
    public ApiResponse<List<Map<String, Object>>> terms() {
        return ApiResponse.success(queryService.terms());
    }

    @GetMapping("/terms/current")
    public ApiResponse<Map<String, Object>> currentTerm() {
        return ApiResponse.success(queryService.currentTerm());
    }

    // —— B-13 课程 ——
    @GetMapping("/courses")
    public ApiResponse<PageResult<Map<String, Object>>> courses(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String courseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 200);
        List<Map<String, Object>> records = queryService.courses(deptId, majorId, courseType, status, keyword, p, s);
        long total = queryService.courseCount(deptId, majorId, courseType, status, keyword);
        return ApiResponse.success(new PageResult<>(records, total, p, s));
    }

    // —— B-21 系统参数 ——
    @GetMapping("/configs")
    public ApiResponse<List<Map<String, Object>>> configs() {
        return ApiResponse.success(queryService.configs());
    }

    // —— B-22 修改系统参数（仅管理员，且仅 editable=1 的项） ——
    @PutMapping("/configs/{id}")
    public ApiResponse<Void> updateConfig(@PathVariable Long id, @RequestBody ConfigUpdateRequest request) {
        com.csms.common.context.CurrentUserHolder.UserContext me =
                com.csms.common.context.CurrentUserHolder.get();
        if (me == null || !"ADMIN".equals(me.getRole())) {
            throw new com.csms.common.error.BizException(
                    com.csms.common.error.ErrorCode.FORBIDDEN, "只有管理员可以修改系统参数");
        }
        queryService.updateConfig(id, request == null ? null : request.getConfigValue());
        return ApiResponse.success();
    }

    @lombok.Data
    public static class ConfigUpdateRequest {
        private String configValue;
    }
}
