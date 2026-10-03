package com.csms.auth.controller;

import com.csms.auth.dto.LoginRequest;
import com.csms.auth.dto.PasswordChangeRequest;
import com.csms.auth.service.AuthService;
import com.csms.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证接口（A-01 ~ A-05）。
 *
 * <p>契约：`/auth/login` 的 `data` 直接返回 JWT 字符串，前端 `stores/auth.ts`
 * 取出后写入 localStorage 并作为后续请求的 Bearer Token。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** A-01 登录 */
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    /** A-02 登出（无状态，前端清除 Token） */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    /** A-03 当前登录人信息 */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    /** A-04 修改密码 */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success();
    }

    /** A-05 当前用户权限码 */
    @GetMapping("/permissions")
    public ApiResponse<List<String>> permissions() {
        Map<String, Object> profile = authService.getCurrentUser();
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) profile.getOrDefault("permissions", List.of());
        return ApiResponse.success(permissions);
    }
}
