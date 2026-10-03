-- V4__fix_demo_passwords.sql
-- 修正 V2 中演示账号的 BCrypt 哈希。
--
-- 背景：V2__init_data.sql 中写入的哈希
--   $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- 经 BCryptPasswordEncoder(10).matches("123456", hash) 实测返回 false，
-- 与注释声称的明文 "123456" 不符，导致三个演示账号全部无法登录。
--
-- 本脚本把密码统一重置为 "123456" 的正确哈希（同样由 BCryptPasswordEncoder(10) 生成）。
-- 保留 V2 原文件不动，避免已部署环境出现 Flyway checksum 校验失败。

UPDATE `sys_user`
   SET `password` = '$2a$10$tAg0anWUC6ZLmGVhqJ8e8.gDmfYcYxqOQAN5YuoZ4q/48Fh4leNly',
       `fail_count` = 0,
       `lock_until` = NULL,
       `updated_at` = NOW()
 WHERE `username` IN ('admin', 'teacher1', 'student1');
