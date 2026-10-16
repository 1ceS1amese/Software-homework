package com.csms.base.controller;

import com.csms.base.service.CourseCommandService;
import com.csms.base.service.CourseCommandService.CourseRequest;
import com.csms.common.api.ApiResponse;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseCommandController {
    private final CourseCommandService service;

    public CourseCommandController(CourseCommandService service) { this.service = service; }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CourseRequest request) {
        requireAdmin();
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        requireAdmin();
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        requireAdmin();
        service.delete(id);
        return ApiResponse.success();
    }

    private void requireAdmin() {
        var user = CurrentUserHolder.get();
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException(ErrorCode.FORBIDDEN);
    }
}
