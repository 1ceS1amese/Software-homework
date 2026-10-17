package com.csms.classroom.controller;
import com.csms.classroom.service.TeachingClassCommandService;
import com.csms.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/teaching-classes")
public class TeachingClassCommandController {
    private final TeachingClassCommandService service;
    public TeachingClassCommandController(TeachingClassCommandService service) { this.service=service; }
    @PostMapping public ApiResponse<?> create(@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(null,b)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(id,b)); }
    @DeleteMapping("/{id}") public ApiResponse<?> delete(@PathVariable Long id) { service.delete(id);return ApiResponse.success(); }
    @PutMapping("/{id}/status") public ApiResponse<?> status(@PathVariable Long id,@RequestBody Map<String,Object> b) { service.status(id,b);return ApiResponse.success(); }
    @GetMapping("/{id}/schedules") public ApiResponse<?> schedules(@PathVariable Long id) { return ApiResponse.success(service.schedules(id)); }
    @PostMapping("/{id}/schedules") public ApiResponse<?> add(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.schedule(id,null,b)); }
    @PutMapping("/{id}/schedules/{sid}") public ApiResponse<?> edit(@PathVariable Long id,@PathVariable Long sid,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.schedule(id,sid,b)); }
    @DeleteMapping("/{id}/schedules/{sid}") public ApiResponse<?> remove(@PathVariable Long id,@PathVariable Long sid,@RequestParam(defaultValue="false") boolean confirm) { service.deleteSchedule(id,sid,confirm);return ApiResponse.success(); }
}
