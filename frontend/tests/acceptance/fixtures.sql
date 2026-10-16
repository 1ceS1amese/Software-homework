-- 仅由 scripts/acceptance.py 在全新独立验收实例执行，不修改开发数据。
INSERT INTO sys_user (id, username, password, real_name, user_type, dept_id, major_id, status)
SELECT 9002, 'acceptance_teacher', password, '验收教师乙', 'TEACHER', 1, NULL, 'ACTIVE'
FROM sys_user WHERE username = 'teacher1';
INSERT INTO sys_user (id, username, password, real_name, user_type, dept_id, major_id, status)
SELECT 9003, 'acceptance_student', password, '验收学生', 'STUDENT', 1, 1, 'ACTIVE'
FROM sys_user WHERE username = 'student1';
INSERT INTO term (id, code, name, start_date, end_date, enroll_start, enroll_end, withdraw_end, status)
VALUES (9001, 'ACCEPTANCE', '独立验收学期', CURRENT_DATE, DATE_ADD(CURRENT_DATE, INTERVAL 1 YEAR),
DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ENROLLING');
INSERT INTO course (id, course_code, name, credit, credit_hours, course_type, dept_id, status)
VALUES (9001, 'ACCEPT-A', '验收互动课程', 3.0, 48, 'ELECTIVE', 1, 'ACTIVE'),
       (9002, 'ACCEPT-B', '验收归属课程', 2.0, 32, 'ELECTIVE', 1, 'ACTIVE'),
       (9003, 'ACCEPT-C', '验收并发课程', 1.0, 16, 'ELECTIVE', 1, 'ACTIVE');
INSERT INTO teaching_class (id, course_id, term_id, teacher_id, class_name, capacity, credit,
start_week, end_week, open_class_date, status)
VALUES (9101, 9001, 9001, 2, '验收互动班', 30, 3.0, 1, 16, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PUBLISHED'),
       (9102, 9002, 9001, 9002, '验收归属班', 30, 2.0, 1, 16, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PUBLISHED'),
       (9103, 9003, 9001, 2, '验收并发班', 1, 1.0, 1, 16, DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 'PUBLISHED');

-- 20 个独立学生账号，只用于真实并发争抢同一个名额。
INSERT INTO sys_user (id, username, password, real_name, user_type, dept_id, major_id, status)
SELECT 9200+n, CONCAT('acceptance_race_', n), u.password, CONCAT('并发学生', n), 'STUDENT', 1, 1, 'ACTIVE'
FROM sys_user u CROSS JOIN (
  SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) numbers WHERE u.username='student1';
