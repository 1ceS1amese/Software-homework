-- V3__init_teaching_data.sql
-- 教学演示数据：学期 / 课程 / 先修关系 / 教学班 / 上课时间段
-- 说明：仅用于开发与演示环境；生产环境执行迁移时应排除本脚本。
SET NAMES utf8mb4;

-- —————————— 学期 ——————————
-- 当前学期处于选课开放期（enroll_start <= NOW() <= enroll_end），用于演示选课主链路
INSERT INTO `term` (`id`, `code`, `name`, `start_date`, `end_date`, `enroll_start`, `enroll_end`, `withdraw_end`, `status`) VALUES
(1, '2023-2024-1', '2023-2024学年第1学期', '2023-09-04', '2024-01-19', '2023-08-20 08:00:00', '2023-09-10 23:59:59', '2023-09-20 23:59:59', 'CLOSED'),
(2, '2024-2025-1', '2024-2025学年第1学期', '2024-09-02', '2025-01-17', '2024-08-19 08:00:00', '2024-09-15 23:59:59', '2024-09-30 23:59:59', 'CLOSED'),
(3, '2025-2026-1', '2025-2026学年第1学期', '2025-09-01', '2026-01-16', '2020-01-01 00:00:00', '2099-12-31 23:59:59', '2099-12-31 23:59:59', 'ENROLLING');

-- —————————— 课程 ——————————
INSERT INTO `course` (`id`, `course_code`, `name`, `credit`, `credit_hours`, `course_type`, `dept_id`, `major_id`, `description`, `status`) VALUES
(1, 'CS101', '高等数学(上)',   5.0, 80, 'REQUIRED',   2, NULL, '工科基础课，一元微积分与级数', 'ACTIVE'),
(2, 'CS102', 'C语言程序设计',  4.0, 64, 'REQUIRED',   1, 1,    'C 语言语法、指针、结构体与文件操作', 'ACTIVE'),
(3, 'CS201', '数据结构与算法', 4.0, 64, 'REQUIRED',   1, 1,    '线性表、树、图与常用排序查找算法', 'ACTIVE'),
(4, 'CS202', '数据库系统原理', 3.0, 48, 'REQUIRED',   1, 1,    '关系模型、SQL、事务与索引', 'ACTIVE'),
(5, 'CS203', '计算机网络',     3.0, 48, 'REQUIRED',   1, 1,    'TCP/IP 协议栈与网络编程基础', 'ACTIVE'),
(6, 'CS301', '操作系统',       4.0, 64, 'REQUIRED',   1, 1,    '进程、内存、文件系统与并发控制', 'ACTIVE'),
(7, 'CS302', '软件工程',       3.0, 48, 'REQUIRED',   1, 2,    '需求、设计、测试与项目管理方法', 'ACTIVE'),
(8, 'CS303', 'Web 应用开发',   3.0, 48, 'ELECTIVE',   1, 2,    '前后端分离架构与 RESTful 接口设计', 'ACTIVE'),
(9, 'MA201', '线性代数',       3.0, 48, 'REQUIRED',   2, NULL, '矩阵、行列式与线性方程组', 'ACTIVE'),
(10,'MA202', '概率论与数理统计',3.0, 48, 'REQUIRED',   2, NULL, '随机变量、分布与参数估计', 'ACTIVE'),
(11,'CS401', '人工智能导论',   3.0, 48, 'ELECTIVE',   1, 1,    '搜索、推理与机器学习基础', 'ACTIVE'),
(12,'CS402', '编译原理',       3.0, 48, 'RESTRICTED', 1, 1,    '词法、语法分析与代码生成', 'ACTIVE');

-- —————————— 先修关系 ——————————
-- 用于演示 BR-06：未修读并通过先修课将被拒绝
INSERT INTO `course_prereq` (`course_id`, `prereq_course_id`, `requirement`) VALUES
(3, 2, 'REQUIRED'),   -- 数据结构 需要 C语言程序设计
(6, 3, 'REQUIRED'),   -- 操作系统 需要 数据结构与算法
(12, 3, 'REQUIRED');  -- 编译原理 需要 数据结构与算法

-- —————————— 教学班 ——————————
INSERT INTO `teaching_class`
(`id`, `course_id`, `term_id`, `teacher_id`, `class_name`, `capacity`, `enrolled_count`, `credit`,
 `location`, `start_week`, `end_week`, `open_class_date`, `status`, `description`) VALUES
(1,  1,  3, 2, '高等数学(上) 01班',    60, 0, 5.0, '教1-201', 1, 16, '2099-01-01', 'PUBLISHED', '面向全校工科专业'),
(2,  2,  3, 2, 'C语言程序设计 01班',   50, 0, 4.0, '教1-305', 1, 16, '2099-01-01', 'PUBLISHED', '机房授课，需自备笔记本'),
(3,  3,  3, 2, '数据结构与算法 01班',  45, 0, 4.0, '教2-108', 1, 16, '2099-01-01', 'PUBLISHED', '先修：C语言程序设计'),
(4,  4,  3, 2, '数据库系统原理 01班',  40, 0, 3.0, '教2-210', 1, 16, '2099-01-01', 'PUBLISHED', '含 8 学时上机'),
(5,  5,  3, 2, '计算机网络 01班',      40, 0, 3.0, '教2-211', 1, 16, '2099-01-01', 'PUBLISHED', ''),
(6,  6,  3, 2, '操作系统 01班',        35, 0, 4.0, '教3-102', 1, 16, '2099-01-01', 'PUBLISHED', '先修：数据结构与算法'),
(7,  7,  3, 2, '软件工程 01班',        50, 0, 3.0, '教3-205', 1, 16, '2099-01-01', 'PUBLISHED', ''),
(8,  8,  3, 2, 'Web应用开发 01班',     30, 0, 3.0, '机房A-1', 1, 16, '2099-01-01', 'PUBLISHED', '限选，需具备编程基础'),
(9,  9,  3, 2, '线性代数 01班',        55, 0, 3.0, '教1-108', 1, 16, '2099-01-01', 'PUBLISHED', ''),
(10, 10, 3, 2, '概率论与数理统计 01班',50, 0, 3.0, '教1-110', 1, 16, '2099-01-01', 'PUBLISHED', ''),
(11, 11, 3, 2, '人工智能导论 01班',    30, 0, 3.0, '教4-201', 1, 16, '2099-01-01', 'PUBLISHED', '选修，欢迎跨专业'),
(12, 12, 3, 2, '编译原理 01班',        25, 0, 3.0, '教4-203', 1, 16, '2099-01-01', 'PUBLISHED', '先修：数据结构与算法'),
(13, 4,  3, 2, '数据库系统原理 02班',  40, 0, 3.0, '教2-212', 1, 16, '2099-01-01', 'DRAFT',     '尚未发布，学生不可见'),
(14, 5,  3, 2, '计算机网络 02班',      40, 0, 3.0, '教2-213', 1, 16, '2020-01-01', 'CLOSED',    '已结课');

-- —————————— 上课时间段 ——————————
-- 注意：id=6 与 id=7 故意安排在同一时段，用于演示 BR-05 时间冲突
INSERT INTO `class_schedule`
(`teaching_class_id`, `day_of_week`, `start_section`, `end_section`, `start_minute`, `end_minute`, `location`, `week_desc`, `start_week`, `end_week`) VALUES
(1,  1, 1, 2, 480, 590, '教1-201', '第1-16周', 1, 16),
(1,  3, 3, 4, 610, 720, '教1-201', '第1-16周', 1, 16),
(2,  2, 1, 2, 480, 590, '教1-305', '第1-16周', 1, 16),
(2,  4, 5, 6, 840, 950, '机房A-2','第1-16周', 1, 16),
(3,  1, 3, 4, 610, 720, '教2-108', '第1-16周', 1, 16),
(3,  3, 1, 2, 480, 590, '教2-108', '第1-16周', 1, 16),
(4,  2, 3, 4, 610, 720, '教2-210', '第1-16周', 1, 16),
(5,  4, 1, 2, 480, 590, '教2-211', '第1-16周', 1, 16),
(6,  5, 1, 2, 480, 590, '教3-102', '第1-16周', 1, 16),   -- 与 7 冲突
(7,  5, 1, 2, 480, 590, '教3-205', '第1-16周', 1, 16),   -- 与 6 冲突
(8,  3, 5, 6, 840, 950, '机房A-1','第1-16周', 1, 16),
(9,  1, 5, 6, 840, 950, '教1-108', '第1-16周', 1, 16),
(10, 2, 5, 6, 840, 950, '教1-110', '第1-16周', 1, 16),
(11, 4, 3, 4, 610, 720, '教4-201', '第1-16周', 1, 16),
(12, 5, 3, 4, 610, 720, '教4-203', '第1-16周', 1, 16),
(13, 2, 7, 8, 970, 1080,'教2-212', '第1-16周', 1, 16),
(14, 3, 7, 8, 970, 1080,'教2-213', '第1-16周', 1, 16);

-- —————————— 上一学期成绩数据（用于演示成绩查询与 GPA） ——————————
-- 学期 2（2024-2025-1）已结课，为 student1 构造两条已完成并发布的成绩
INSERT INTO `teaching_class`
(`id`, `course_id`, `term_id`, `teacher_id`, `class_name`, `capacity`, `enrolled_count`, `credit`,
 `location`, `start_week`, `end_week`, `open_class_date`, `status`, `description`) VALUES
(15, 2, 2, 2, 'C语言程序设计 01班(2024秋)', 50, 1, 4.0, '教1-305', 1, 16, '2024-09-02', 'CLOSED', '历史教学班'),
(16, 9, 2, 2, '线性代数 01班(2024秋)',      55, 1, 3.0, '教1-108', 1, 16, '2024-09-02', 'CLOSED', '历史教学班');

INSERT INTO `enrollment`
(`student_id`, `teaching_class_id`, `course_id`, `term_id`, `credit`, `status`, `enrolled_at`, `source`) VALUES
(3, 15, 2, 2, 4.0, 'COMPLETED', '2024-09-03 09:00:00', 'PORTAL'),
(3, 16, 9, 2, 3.0, 'COMPLETED', '2024-09-03 09:05:00', 'PORTAL');

INSERT INTO `grade`
(`enrollment_id`, `teaching_class_id`, `student_id`, `course_id`, `term_id`,
 `regular_score`, `midterm_score`, `final_score`, `total_score`, `grade_point`, `is_pass`,
 `status`, `published_at`, `published_by`)
SELECT e.id, e.teaching_class_id, e.student_id, e.course_id, e.term_id,
       88.00, 82.00, 90.00, 87.20, 3.04, 1, 'PUBLISHED', '2025-01-20 10:00:00', 2
  FROM enrollment e WHERE e.student_id = 3 AND e.teaching_class_id = 15;

INSERT INTO `grade`
(`enrollment_id`, `teaching_class_id`, `student_id`, `course_id`, `term_id`,
 `regular_score`, `midterm_score`, `final_score`, `total_score`, `grade_point`, `is_pass`,
 `status`, `published_at`, `published_by`)
SELECT e.id, e.teaching_class_id, e.student_id, e.course_id, e.term_id,
       75.00, 68.00, 72.00, 71.70, 1.88, 1, 'PUBLISHED', '2025-01-20 10:00:00', 2
  FROM enrollment e WHERE e.student_id = 3 AND e.teaching_class_id = 16;

-- —————————— 审计与登录日志示例 ——————————
INSERT INTO `sys_login_log` (`username`, `user_id`, `ip`, `user_agent`, `result`, `login_at`) VALUES
('admin',    1, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'SUCCESS', NOW()),
('student1', 3, '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'SUCCESS', NOW());

INSERT INTO `sys_audit_log` (`user_id`, `username`, `role_code`, `module`, `action`, `target_type`, `target_id`, `result`, `ip`, `trace_id`, `created_at`) VALUES
(1, 'admin', 'ADMIN', 'BASE', 'COURSE_CREATE', 'COURSE', 1, 'SUCCESS', '127.0.0.1', 'seed0001', NOW()),
(1, 'admin', 'ADMIN', 'CLASS', 'CLASS_STATUS_CHANGE', 'TEACHING_CLASS', 1, 'SUCCESS', '127.0.0.1', 'seed0002', NOW());
