package com.csms.base.controller;
import com.csms.base.service.PrerequisiteService;
import com.csms.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/courses/{id}/prereqs")
public class PrerequisiteController {
    private final PrerequisiteService service;
    public PrerequisiteController(PrerequisiteService service) { this.service=service; }
    @GetMapping public ApiResponse<?> list(@PathVariable Long id) { return ApiResponse.success(service.list(id)); }
    @PostMapping public ApiResponse<?> add(@PathVariable Long id,@RequestBody Map<String,Object> body) { service.add(id,body);return ApiResponse.success(); }
    @DeleteMapping("/{prereqCourseId}") public ApiResponse<?> delete(@PathVariable Long id,@PathVariable Long prereqCourseId) { service.delete(id,prereqCourseId);return ApiResponse.success(); }
}
