package com.csms.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csms.auth.service.AuthService;
import com.csms.common.context.CurrentUserHolder;
import com.csms.common.enums.UserStatus;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import com.csms.security.JwtService;
import com.csms.user.entity.User;
import com.csms.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现。
 *
 * <p>登录流程：查用户 → 校验状态与锁定 → BCrypt 校验密码 → 签发 JWT → 更新登录信息。
 * 用户不存在与密码错误返回同一错误码，避免用户名枚举（BR-15）。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 连续失败达到该次数即锁定账号 */
    private static final int MAX_FAIL_COUNT = 5;
    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 15;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public String login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BizException(ErrorCode.AUTH_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            long minutes = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new BizException(ErrorCode.AUTH_LOCKED, "账号已锁定，请 " + minutes + " 分钟后再试");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailure(user);
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getFailCount, 0)
                .set(User::getLockUntil, null)
                .set(User::getLastLoginAt, LocalDateTime.now()));

        String role = user.getUserType() == null ? null : user.getUserType().name();
        return jwtService.generateToken(user.getId(), user.getUsername(), role, user.getDeptId());
    }

    private void recordFailure(User user) {
        int failCount = (user.getFailCount() == null ? 0 : user.getFailCount()) + 1;
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getFailCount, failCount);
        if (failCount >= MAX_FAIL_COUNT) {
            update.set(User::getLockUntil, LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        userMapper.update(null, update);
    }

    @Override
    public void logout() {
        // 无状态 JWT：服务端不维护会话，登出由前端清除 Token 完成。
        // 如需强制失效，可在此接入 Token 黑名单（P2）。
    }

    @Override
    public Map<String, Object> getCurrentUser() {
        CurrentUserHolder.UserContext ctx = CurrentUserHolder.get();
        if (ctx == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        User user = userMapper.selectById(ctx.getId());
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("userType", user.getUserType() == null ? null : user.getUserType().name());
        profile.put("deptId", user.getDeptId());
        profile.put("majorId", user.getMajorId());
        profile.put("gender", user.getGender() == null ? null : user.getGender().name());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("status", user.getStatus() == null ? null : user.getStatus().name());
        profile.put("mustChangePwd", Boolean.TRUE.equals(user.getMustChangePwd()));
        profile.put("permissions", permissionsOf(user));
        return profile;
    }

    /** 按角色下发按钮级权限码（对应 docs/04 §9） */
    private List<String> permissionsOf(User user) {
        String role = user.getUserType() == null ? "" : user.getUserType().name();
        return switch (role) {
            case "ADMIN" -> List.of(
                    "user:read", "user:write", "user:reset-pwd",
                    "base:dept:write", "base:major:write", "base:term:write",
                    "base:course:write", "base:config:write",
                    "class:write", "class:publish", "class:delete",
                    "grade:read", "grade:write", "grade:publish", "grade:unlock",
                    "stat:school", "audit:read");
            case "TEACHER" -> List.of(
                    "class:write", "class:publish",
                    "grade:read", "grade:write", "grade:publish");
            default -> List.of("enroll:self");
        };
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        CurrentUserHolder.UserContext ctx = CurrentUserHolder.get();
        if (ctx == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        User user = userMapper.selectById(ctx.getId());
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_BAD_OLD_PASSWORD);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(newPassword))
                .set(User::getMustChangePwd, false));
    }
}
