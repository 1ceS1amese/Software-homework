package com.csms.user.controller;
import com.csms.common.api.ApiResponse;
import com.csms.user.service.UserCommandService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/users")
public class UserCommandController {
    private final UserCommandService service;
    public UserCommandController(UserCommandService service) { this.service=service; }
    @PostMapping public ApiResponse<?> create(@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(null,b)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@RequestBody Map<String,Object> b) { return ApiResponse.success(service.save(id,b)); }
    @DeleteMapping("/{id}") public ApiResponse<?> delete(@PathVariable Long id) { service.delete(id);return ApiResponse.success(); }
    @PutMapping("/{id}/password") public ApiResponse<?> reset(@PathVariable Long id) { return ApiResponse.success(service.reset(id)); }
    @PutMapping("/{id}/roles") public ApiResponse<?> roles(@PathVariable Long id,@RequestBody Map<String,Object> b) { service.roles(id,b);return ApiResponse.success(); }
}
