-- V2__init_data.sql
-- 全局配置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 内置角色
INSERT INTO `sys_role` (`id`, `code`, `name`, `description`, `builtin`) VALUES
(1, 'STUDENT', '学生', '学生角色', 1),
(2, 'TEACHER', '教师', '教师角色', 1),
(3, 'ADMIN', '管理员', '系统管理员', 1);

-- 默认用户 (密码皆为 123456, BCrypt: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2)
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `user_type`, `status`, `must_change_pwd`, `deleted`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'ADMIN', 'ACTIVE', 0, 0),
(2, 'teacher1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张教师', 'TEACHER', 'ACTIVE', 0, 0),
(3, 'student1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李学生', 'STUDENT', 'ACTIVE', 0, 0);

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 3), -- admin -> ADMIN
(2, 2), -- teacher1 -> TEACHER
(3, 1); -- student1 -> STUDENT

-- 院系与专业示例
INSERT INTO `dept` (`id`, `code`, `name`, `status`, `sort_no`) VALUES
(1, 'CS', '计算机科学与技术学院', 'ACTIVE', 1),
(2, 'MATH', '数学学院', 'ACTIVE', 2);

INSERT INTO `major` (`id`, `dept_id`, `code`, `name`, `degree_years`, `status`) VALUES
(1, 1, 'CS01', '计算机科学与技术', 4, 'ACTIVE'),
(2, 1, 'SE01', '软件工程', 4, 'ACTIVE'),
(3, 2, 'MATH01', '数学与应用数学', 4, 'ACTIVE');

-- 更新用户所属院系/专业
UPDATE `sys_user` SET `dept_id` = 1, `major_id` = 1 WHERE `id` = 3;
UPDATE `sys_user` SET `dept_id` = 1 WHERE `id` = 2;

-- 系统参数配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `value_type`, `description`, `editable`) VALUES
('grade.weight.regular', '0.3', 'NUMBER', '平时成绩权重', 1),
('grade.weight.midterm', '0.3', 'NUMBER', '期中成绩权重', 1),
('grade.weight.final', '0.4', 'NUMBER', '期末成绩权重', 1),
('grade.pass_score', '60', 'NUMBER', '及格分数线', 1),
('grade.point.mapping_mode', 'LINEAR', 'STRING', '绩点映射模式(LINEAR/SECTION)', 1),
('enroll.max_credit_per_term', '30', 'NUMBER', '单学期最大选课学分', 1),
('enroll.min_credit_per_term', '0', 'NUMBER', '单学期最低选课学分', 1),
('enroll.withdraw_deadline_mode', 'TERM_ENROLL_END', 'STRING', '退课截止期模式', 1);

SET FOREIGN_KEY_CHECKS = 1;
