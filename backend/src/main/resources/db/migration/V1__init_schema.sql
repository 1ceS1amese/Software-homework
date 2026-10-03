-- V1__init_schema.sql
-- 全局配置
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. `sys_user` 用户
CREATE TABLE `sys_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '登录名（学号/工号）',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  `real_name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `user_type` VARCHAR(16) NOT NULL COMMENT 'STUDENT/TEACHER/ADMIN',
  `dept_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属院系',
  `major_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属专业（学生）',
  `gender` VARCHAR(8) DEFAULT NULL COMMENT 'MALE/FEMALE/UNKNOWN',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '加密或脱敏存储',
  `email` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  `must_change_pwd` TINYINT NOT NULL DEFAULT 0 COMMENT '是否强制改密',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
  `lock_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间',
  `last_login_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '0 正常 / 1 删除',
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_dept` (`dept_id`),
  KEY `idx_user_major` (`major_id`),
  KEY `idx_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 2. `sys_role` 角色
CREATE TABLE `sys_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT 'STUDENT/TEACHER/ADMIN',
  `name` VARCHAR(32) NOT NULL COMMENT '学生/教师/管理员',
  `description` VARCHAR(200) DEFAULT NULL,
  `builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 3. `sys_user_role` 用户角色关联
CREATE TABLE `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_uar_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 4. `sys_login_log` 登录日志
CREATE TABLE `sys_login_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '尝试登录的用户名',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '成功时写入',
  `ip` VARCHAR(45) DEFAULT NULL,
  `user_agent` VARCHAR(300) DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL COMMENT 'SUCCESS/BAD_PASSWORD/USER_NOT_FOUND/DISABLED/LOCKED',
  `login_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_username_time` (`username`, `login_at`),
  KEY `idx_login_time` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- 5. `sys_audit_log` 操作审计
CREATE TABLE `sys_audit_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED DEFAULT NULL,
  `username` VARCHAR(50) DEFAULT NULL,
  `role_code` VARCHAR(16) DEFAULT NULL,
  `module` VARCHAR(32) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(32) DEFAULT NULL,
  `target_id` BIGINT UNSIGNED DEFAULT NULL,
  `before_json` JSON DEFAULT NULL,
  `after_json` JSON DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAIL',
  `ip` VARCHAR(45) DEFAULT NULL,
  `trace_id` VARCHAR(32) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user_time` (`user_id`, `created_at`),
  KEY `idx_audit_action` (`action`, `created_at`),
  KEY `idx_audit_target` (`target_type`, `target_id`),
  KEY `idx_audit_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作审计表';

-- 6. `sys_config` 系统参数
CREATE TABLE `sys_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(64) NOT NULL,
  `config_value` VARCHAR(255) NOT NULL,
  `value_type` VARCHAR(16) NOT NULL COMMENT 'STRING/NUMBER/BOOL/JSON',
  `description` VARCHAR(200) DEFAULT NULL,
  `editable` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数表';

-- 7. `dept` 院系
CREATE TABLE `dept` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `leader` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `sort_no` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='院系表';

-- 8. `major` 专业
CREATE TABLE `major` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dept_id` BIGINT UNSIGNED NOT NULL,
  `code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `degree_years` TINYINT NOT NULL DEFAULT 4 COMMENT '学制年限',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_code` (`code`),
  KEY `idx_major_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='专业表';

-- 9. `term` 学期
CREATE TABLE `term` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL COMMENT '如 2024-2025-1',
  `name` VARCHAR(64) NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `enroll_start` DATETIME NOT NULL,
  `enroll_end` DATETIME NOT NULL,
  `withdraw_end` DATETIME NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/ENROLLING/RUNNING/CLOSED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学期表';

-- 10. `course` 课程
CREATE TABLE `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `credit` DECIMAL(3,1) NOT NULL,
  `credit_hours` INT NOT NULL,
  `course_type` VARCHAR(16) NOT NULL COMMENT 'REQUIRED/ELECTIVE/RESTRICTED',
  `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '开课单位',
  `major_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '面向专业（可空=通用）',
  `description` VARCHAR(1000) DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_course_dept` (`dept_id`),
  KEY `idx_course_name` (`name`),
  KEY `idx_course_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程表';

-- 11. `course_prereq` 课程先修关系
CREATE TABLE `course_prereq` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `prereq_course_id` BIGINT UNSIGNED NOT NULL,
  `requirement` VARCHAR(16) NOT NULL DEFAULT 'REQUIRED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_prereq` (`course_id`, `prereq_course_id`),
  KEY `idx_prereq_reverse` (`prereq_course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程先修关系表';

-- 12. `teaching_class` 教学班
CREATE TABLE `teaching_class` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `term_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `assistant_ids` VARCHAR(200) DEFAULT NULL COMMENT '预留',
  `class_name` VARCHAR(64) NOT NULL,
  `capacity` INT NOT NULL,
  `enrolled_count` INT NOT NULL DEFAULT 0,
  `credit` DECIMAL(3,1) NOT NULL COMMENT '冗余学期分',
  `location` VARCHAR(100) DEFAULT NULL,
  `start_week` TINYINT NOT NULL,
  `end_week` TINYINT NOT NULL,
  `open_class_date` DATE NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED/CANCELLED',
  `capacity_full_enabled` TINYINT NOT NULL DEFAULT 0,
  `description` VARCHAR(500) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tc_term_status` (`term_id`, `status`),
  KEY `idx_tc_course` (`course_id`),
  KEY `idx_tc_teacher` (`teacher_id`, `term_id`),
  KEY `idx_tc_term_course` (`term_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教学班表';

-- 13. `class_schedule` 上课时间段
CREATE TABLE `class_schedule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teaching_class_id` BIGINT UNSIGNED NOT NULL,
  `day_of_week` TINYINT NOT NULL COMMENT '1=周一 7=周日',
  `start_section` SMALLINT NOT NULL,
  `end_section` SMALLINT NOT NULL,
  `start_minute` INT NOT NULL,
  `end_minute` INT NOT NULL,
  `location` VARCHAR(100) DEFAULT NULL,
  `week_desc` VARCHAR(64) NOT NULL,
  `start_week` TINYINT NOT NULL DEFAULT 1,
  `end_week` TINYINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sched_class` (`teaching_class_id`),
  KEY `idx_sched_day_week` (`day_of_week`, `start_week`, `end_week`, `start_minute`, `end_minute`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='上课时间段表';

-- 14. `enrollment` 选课关系
CREATE TABLE `enrollment` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `teaching_class_id` BIGINT UNSIGNED NOT NULL,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `term_id` BIGINT UNSIGNED NOT NULL,
  `credit` DECIMAL(3,1) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ENROLLED' COMMENT 'ENROLLED/WITHDRAWN/COMPLETED',
  `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `withdrawn_at` DATETIME DEFAULT NULL,
  `grade_id` BIGINT UNSIGNED DEFAULT NULL,
  `source` VARCHAR(16) NOT NULL DEFAULT 'PORTAL' COMMENT 'PORTAL/ADMIN',
  `remark` VARCHAR(200) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enroll_student_class` (`student_id`, `teaching_class_id`),
  KEY `idx_enroll_class_status` (`teaching_class_id`, `status`),
  KEY `idx_enroll_student_status` (`student_id`, `status`),
  KEY `idx_enroll_term_term` (`term_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='选课关系表';

-- 15. `grade` 成绩
CREATE TABLE `grade` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `enrollment_id` BIGINT UNSIGNED NOT NULL,
  `teaching_class_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `term_id` BIGINT UNSIGNED NOT NULL,
  `regular_score` DECIMAL(5,2) DEFAULT NULL,
  `midterm_score` DECIMAL(5,2) DEFAULT NULL,
  `final_score` DECIMAL(5,2) DEFAULT NULL,
  `total_score` DECIMAL(5,2) DEFAULT NULL,
  `grade_point` DECIMAL(4,2) DEFAULT NULL,
  `is_pass` TINYINT DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
  `published_at` DATETIME DEFAULT NULL,
  `published_by` BIGINT UNSIGNED DEFAULT NULL,
  `unlock_count` INT NOT NULL DEFAULT 0,
  `last_unlock_reason` VARCHAR(300) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` BIGINT UNSIGNED DEFAULT NULL,
  `updated_by` BIGINT UNSIGNED DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_enrollment` (`enrollment_id`),
  KEY `idx_grade_class` (`teaching_class_id`, `status`),
  KEY `idx_grade_student` (`student_id`, `term_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成绩表';

SET FOREIGN_KEY_CHECKS = 1;
