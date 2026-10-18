package com.csms.auth.dto;

import lombok.Data;

@Data
public class PasswordChangeRequest {
    @jakarta.validation.constraints.NotBlank(message="密码不能为空")
    @jakarta.validation.constraints.Size(max=72,message="密码长度无效")
    private String oldPassword;
    @jakarta.validation.constraints.NotBlank(message="密码不能为空")
    @jakarta.validation.constraints.Size(max=72,message="密码长度无效")
    private String newPassword;
}
