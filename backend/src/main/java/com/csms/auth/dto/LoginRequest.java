package com.csms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    @jakarta.validation.constraints.Size(max=50,message="用户名不能超过50字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @jakarta.validation.constraints.Size(max=72,message="密码长度无效")
    private String password;
}
