package com.csms.classroom.controller;

import com.csms.classroom.service.TeachingClassQueryService;
import com.csms.common.api.ApiResponse;
import com.csms.common.api.PageResult;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教学班接口（C-01 / C-02 / C-12）。
 *
 * <p>列表与详情对所有已登录用户开放（学生需要浏览课程），
 * 教师工作台接口按 teacherId 过滤，并强制校验归属。
 */
@RestController
@RequestMapping("/teaching-classes")
public class TeachingClassController {

    private final TeachingClassQueryService queryService;

    public TeachingClassController(TeachingClassQueryService queryService) {
        this.queryService = queryService;
    }

    /** C-01 教学班列表 */
    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean onlyAvailable,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(queryService.query(
                termId, courseId, teacherId, keyword, status, onlyAvailable, page, size));
    }

    /** C-02 教学班详情 */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(queryService.detail(id));
    }

    /** C-12 我的教学班（教师） */
    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> myClasses(@RequestParam(required = false) Long termId) {
        CurrentUserHolder.UserContext me = CurrentUserHolder.get();
        if (me == null || me.getId() == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!"TEACHER".equals(me.getRole()) && !"ADMIN".equals(me.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有教师可以查看我的教学班");
        }
        return ApiResponse.success(queryService.byTeacher(me.getId(), termId));
    }
}
