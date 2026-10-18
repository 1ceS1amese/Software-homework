package com.csms.auth.service;

import java.util.Map;

public interface AuthService {
    String login(String username, String password);
    Map<String,Object> tokenResponse(String token);
    void logout();
    Map<String, Object> getCurrentUser();
    void changePassword(String oldPassword, String newPassword);
}
