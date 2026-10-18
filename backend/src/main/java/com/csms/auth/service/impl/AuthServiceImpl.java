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
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.csms.audit.service.AuditWriter audit;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.csms.security.UserAccessRegistry access;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService, com.csms.security.UserAccessRegistry access, org.springframework.jdbc.core.JdbcTemplate jdbc, com.csms.audit.service.AuditWriter audit) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.access = access;this.jdbc=jdbc;this.audit=audit;
    }

    @Override
    @Transactional(noRollbackFor = BizException.class)
    public String login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("FOR UPDATE"));

        if (user == null) {
            loginRecord(username,null,"USER_NOT_FOUND");
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            loginRecord(username,user,"DISABLED");
            throw new BizException(ErrorCode.AUTH_DISABLED);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            loginRecord(username,user,"LOCKED");
            long minutes = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new BizException(ErrorCode.AUTH_LOCKED, "账号已锁定，请 " + minutes + " 分钟后再试");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailure(user);
            loginRecord(username,user,"BAD_PASSWORD");
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getFailCount, 0)
                .set(User::getLockUntil, null)
                .set(User::getLastLoginAt, LocalDateTime.now()));

        loginRecord(username,user,"SUCCESS");
        String role = user.getUserType() == null ? null : user.getUserType().name();
        return jwtService.generateToken(user.getId(), user.getUsername(), role, user.getDeptId(), user.getMajorId(), user.getVersion()==null?0:user.getVersion());
    }

    private void loginRecord(String username,User user,String result) {
        var request=com.csms.audit.service.AuditWriter.request();
        jdbc.update("INSERT INTO sys_login_log(username,user_id,ip,user_agent,result) VALUES (?,?,?,?,?)",
            username,"SUCCESS".equals(result)&&user!=null?user.getId():null,
            request==null?null:com.csms.audit.service.AuditWriter.clip(request.getRemoteAddr(),45),
            request==null?null:com.csms.audit.service.AuditWriter.clip(request.getHeader("User-Agent"),300),result);
        var actor=new CurrentUserHolder.UserContext(user==null?null:user.getId(),username,
            user==null?null:user.getUserType().name(),user==null?null:user.getDeptId());
        audit.recordAs(actor,"AUTH","LOGIN","sys_user",user==null?null:user.getId(),null,
            Map.of("loginResult",result),"SUCCESS".equals(result)?"SUCCESS":"FAIL",request);
    }

    private int setting(String key,int fallback,int max) {
        var values=jdbc.queryForList("SELECT config_value FROM sys_config WHERE config_key=?",String.class,key);
        if(!values.isEmpty()) try { int n=Integer.parseInt(values.get(0));if(n>=1&&n<=max)return n; } catch(NumberFormatException ignored) { }
        return fallback;
    }
    private void recordFailure(User user) {
        int failCount = (user.getLockUntil()!=null || user.getFailCount()==null ? 0 : user.getFailCount()) + 1;
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getFailCount, failCount)
                .set(User::getLockUntil, null);
        if (failCount >= setting("auth.max_fail_count",MAX_FAIL_COUNT,20)) {
            update.set(User::getLockUntil, LocalDateTime.now().plusMinutes(setting("auth.lock_minutes",LOCK_MINUTES,1440)));
        }
        userMapper.update(null, update);
    }

    @Override
    public void logout() {
        audit.record("AUTH","LOGOUT","sys_user",CurrentUserHolder.get().getId(),null,null);
    }

    @Override
    public Map<String, Object> getCurrentUser() {
        CurrentUserHolder.UserContext ctx = CurrentUserHolder.get();
        if (ctx == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        return profile(ctx.getId());
    }

    private Map<String,Object> profile(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("userType", user.getUserType() == null ? null : user.getUserType().name());
        profile.put("role", user.getUserType()==null?null:user.getUserType().name());
        profile.put("deptName", user.getDeptId()==null?null:jdbc.queryForObject("SELECT name FROM dept WHERE id=?",String.class,user.getDeptId()));
        profile.put("majorName", user.getMajorId()==null?null:jdbc.queryForObject("SELECT name FROM major WHERE id=?",String.class,user.getMajorId()));
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

    @Override public Map<String,Object> tokenResponse(String token) {
        return Map.of("accessToken",token,"tokenType","Bearer","expiresIn",(jwtService.getClaims(token).getExpiration().getTime()-jwtService.getClaims(token).getIssuedAt().getTime())/1000,"user",profile(jwtService.getId(token)));
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
        if(newPassword==null || newPassword.length()<8 || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72
            || !newPassword.matches("(?s).*[A-Za-z].*") || !newPassword.matches("(?s).*[0-9].*"))
            throw new BizException(ErrorCode.PARAM_ERROR,"新密码须至少8位、包含字母和数字，且不超过72字节");
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
                .setSql("version=version+1")
                .set(User::getMustChangePwd, false));
        access.changed(ctx.getId());
        audit.record("AUTH","CHANGE_PASSWORD","sys_user",ctx.getId(),null,Map.of("credentialsChanged",true));
    }
}
