-- 仅新增配置，不改变表结构，不覆盖已有同名配置；旧程序可忽略这些项。
INSERT IGNORE INTO sys_config (config_key, config_value, value_type, description, editable)
VALUES ('auth.max_fail_count', '5', 'NUMBER', '连续登录失败锁定阈值，整数1至20', 1),
       ('auth.lock_minutes', '15', 'NUMBER', '登录锁定时长，整数1至1440分钟', 1),
       ('auth.jwt_expire_seconds', '7200', 'NUMBER', '新登录令牌有效期，整数60至86400秒', 1);
