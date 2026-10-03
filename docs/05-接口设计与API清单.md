# 05 接口设计与 API 清单

| 项 | 值 |
|---|---|
| 上游 | `docs/00`（BR）`docs/02`（表字段）`docs/01`（分层） |
| 下游 | `docs/03`（实现）`docs/04`（前端调用）`docs/07`（用例） |
| 地位 | **接口契约唯一出处**。`04` 只引用不新增；`07` 按端点编号写用例 |

---

## 1. 通用约定

| 项 | 约定 |
|---|---|
| 基础路径 | `/api/v1` |
| Content-Type | `application/json;charset=UTF-8`（文件上传为 `multipart/form-data`） |
| 鉴权 | `Authorization: Bearer <JWT>` |
| 响应体 | 恒定信封，见下 |
| 分页入参 | `page`（**从 1 起**）、`size`（默认 20，上限 100）、`sortBy`、`sortOrder`(asc/desc) |
| 分页出参 | `data.records` / `data.total` / `data.page` / `data.size` / `data.pages` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`；纯日期 `yyyy-MM-dd` |
| 金额/学分 | 一律字符串或数值型 `Decimal`，**禁止浮点** |
| Long 型 ID | JSON 中以 **字符串**返回（防前端精度丢失） |
| 布尔查询参数 | `true`/`false` 小写 |
| 跨域 | 开发期由 Vite 代理解决；生产同源部署，**不开放 CORS 通配** |

### 1.1 统一响应体

```jsonc
// 成功
{ "code": 0, "message": "success", "data": { }, "traceId": "a1b2c3d4" }

// 失败
{ "code": 50103, "message": "与《大学英语》上课时间冲突（周二 1-2 节）", "data": null, "traceId": "a1b2c3d4" }
```

- `code = 0` 表示成功；非 0 为业务错误码（见 §4）。
- `traceId` 同时写入响应体、HTTP 响应头 `X-Trace-Id`、应用日志与审计表，便于排障。
- **失败时 `message` 必须是可直接展示给用户的中文**，禁止把 `SQLException`/堆栈透传给前端（`03` §5）。

### 1.2 幂等与并发约定

| 约定 | 说明 |
|---|---|
| 选课重复提交 | 前端按钮 loading 禁用；服务端靠 `uk_enroll_student_class` 保证，返回 `50101` |
| 成绩发布 | 幂等：重复调用返回 `code=0` + `message="成绩已发布"` |
| 退课 | 幂等：已退课再调用返回 `code=0` + `message="已退课"` |
| 状态流转 | 非法流转返回 `40103`，不做静默忽略 |

---

## 2. 认证与授权

### 2.1 登录 `POST /api/v1/auth/login`

**权限**：公开

请求：
```jsonc
{
  "username": "2024010101",
  "password": "******",
  "captcha": "8f3k"      // P2，默认关闭；配置开启时必填
}
```

响应 `data`：
```jsonc
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 7200,              // 秒
  "user": {
    "id": "10001",
    "username": "2024010101",
    "realName": "张三",
    "role": "STUDENT",
    "deptId": "1",
    "deptName": "计算机学院",
    "majorId": "12",
    "majorName": "软件工程",
    "mustChangePwd": false
  }
}
```

失败：密码错 `20001`、用户不存在 `20001`（**统一话术，不区分用户是否存在**，防用户名枚举）、账号禁用 `20003`、锁定 `20002`。

### 2.2 JWT 结构

| Claim | 说明 |
|---|---|
| `sub` | `userId` |
| `username` / `role` / `deptId` / `majorId` | 角色判定所需，服务端每次请求解析，**不再查库**（性能取舍见 `06` §2） |
| `iat` / `exp` | 过期时间，默认 2 小时（`sys_config` 可调） |
| `jti` | 令牌唯一标识，写入 `sys_login_log` 关联 |

### 2.3 授权判定顺序（服务端强制）

1. JWT 校验签名与过期 → 失败 `20005`/`20006` → HTTP 401。
2. 由 `role` 判定接口是否对该角色开放（URL 级）→ 不开放 `10002` → HTTP 403。
3. 加载资源并判定归属（教师是否名下教学班、学生是否本人）→ 不通过 `10002` → HTTP 403。
4. 执行业务。

> **第 3 步不可省略**：URL 级放行不等于数据安全（BR-14）。

---

## 3. API 清单

图例：**权限** `公开` / `S`学生 / `T`教师 / `A`管理员 / `SA`学生或管理员 / `STA`全部已登录。
**幂等** `Y` = 重复调用结果一致；`N` = 写操作。
**事务** `TX` = 需事务；`—` = 无。

### 3.1 认证与个人信息 AUTH

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| A-01 | POST | `/auth/login` | 公开 | Y | — | 登录 |
| A-02 | POST | `/auth/logout` | 全部 | Y | — | 写审计，Token 由前端清除 |
| A-03 | GET | `/auth/me` | 全部 | Y | — | 当前用户档案 + 权限码 |
| A-04 | PUT | `/auth/password` | 全部 | N | TX | 改密，需带 `oldPassword` |
| A-05 | GET | `/auth/permissions` | 全部 | Y | — | 返回按钮级权限码数组 |

### 3.2 用户管理 USER（管理员）

| # | 方法 | 路径 | 权限 | 事务 | 说明 |
|---|---|---|---|:---:|---|
| U-01 | GET | `/users` | A | — | 分页；筛选 `role/deptId/majorId/status/keyword` |
| U-02 | POST | `/users` | A | TX | 新建；用户名唯一 `70101` |
| U-03 | GET | `/users/{id}` | A | — | 详情 |
| U-04 | PUT | `/users/{id}` | A | TX | 编辑基础信息（**不含密码**） |
| U-05 | DELETE | `/users/{id}` | A | TX | 逻辑删除；有选课/成绩则拒绝 `40105` |
| U-06 | PUT | `/users/{id}/password` | A | TX | 重置密码，返回一次性初始密码并置 `mustChangePwd=1` |
| U-07 | PUT | `/users/{id}/status` | A | TX | 启用/禁用 |
| U-08 | PUT | `/users/{id}/roles` | A | TX | 分配角色 |
| U-09 | GET | `/users/statistics` | A | — | 学生/教师数、男女比例（`FR-SYS-01` 附属） |

### 3.3 基础数据 BASE

| # | 方法 | 路径 | 权限 | 事务 | 说明 |
|---|---|---|---|:---:|---|
| B-01 | GET | `/depts` | 全部 | — | 列表（含 `status`）；`tree=true` 返回树 |
| B-02 | POST | `/depts` | A | TX | |
| B-03 | PUT | `/depts/{id}` | A | TX | |
| B-04 | DELETE | `/depts/{id}` | A | TX | 存在下级专业或用户时拒绝 `30101` |
| B-05 | GET | `/majors` | 全部 | — | 筛选 `deptId/keyword/status` |
| B-06 | POST | `/majors` | A | TX | `deptId` 必填 |
| B-07 | PUT | `/majors/{id}` | A | TX | |
| B-08 | DELETE | `/majors/{id}` | A | TX | 存在用户时拒绝 |
| B-09 | GET | `/terms` | 全部 | — | 按 `startDate` 倒序 |
| B-10 | POST | `/terms` | A | TX | `code` 唯一 `30301` |
| B-11 | PUT | `/terms/{id}` | A | TX | |
| B-12 | PUT | `/terms/{id}/status` | A | TX | 迁移 `PLANNED→ENROLLING→RUNNING→CLOSED` |
| B-13 | GET | `/courses` | 全部 | — | 分页；筛选 `termId/deptId/majorId/courseType/status/keyword(代码或名称)` |
| B-14 | POST | `/courses` | A | TX | `courseCode` 唯一 `30401` |
| B-15 | GET | `/courses/{id}` | 全部 | — | 含先修项 |
| B-16 | PUT | `/courses/{id}` | A | TX | |
| B-17 | DELETE | `/courses/{id}` | A | TX | 存在教学班时拒绝 |
| B-18 | GET | `/courses/{id}/prereqs` | 全部 | — | |
| B-19 | POST | `/courses/{id}/prereqs` | A | TX | 检测环形依赖 `30501` |
| B-20 | DELETE | `/courses/{id}/prereqs/{prereqCourseId}` | A | TX | |
| B-21 | GET | `/configs` | 全部 | — | 参数字典（成绩权重等，前端计算用） |
| B-22 | PUT | `/configs/{key}` | A | TX | 改参数写审计 |

### 3.4 教学班 CLASS

| # | 方法 | 路径 | 权限 | 事务 | 说明 |
|---|---|---|---|:---:|---|
| C-01 | GET | `/teaching-classes` | 全部 | — | **核心列表**。筛选：`termId`(默认当前学期)/`courseId`/`teacherId`/`deptId`/`status`/`onlyAvailable=true`/`keyword`/`hasConflict`(学生视角)；排序 `enrolled_count`/`start_time` |
| C-02 | POST | `/teaching-classes` | T/A | TX | 教师建班自动归属自己 |
| C-03 | GET | `/teaching-classes/{id}` | 全部 | — | 详情含时间段 |
| C-04 | PUT | `/teaching-classes/{id}` | T(本人)/A | TX | `capacity` 不得 < `enrolledCount` → `40102` |
| C-05 | DELETE | `/teaching-classes/{id}` | T(本人,DRAFT)/A | TX | 有选课记录禁止删 → `40105` |
| C-06 | PUT | `/teaching-classes/{id}/status` | T(本人)/A | TX | body `{ "action": "PUBLISH|CLOSE|CANCEL|ROLLBACK_DRAFT" }` |
| C-07 | GET | `/teaching-classes/{id}/schedules` | 全部 | — | |
| C-08 | POST | `/teaching-classes/{id}/schedules` | T(本人)/A | TX | 班内时段自重叠拒绝 `40104` |
| C-09 | PUT | `/teaching-classes/{id}/schedules/{sid}` | T(本人)/A | TX | |
| C-10 | DELETE | `/teaching-classes/{id}/schedules/{sid}` | T(本人)/A | TX | 有选课时删除需二次确认 |
| C-11 | GET | `/teaching-classes/{id}/students` | T(本人)/A | — | 选课名单分页；含 `studentId/学号/姓名/专业/选课时间/enrollStatus/是否冲突` |
| C-12 | GET | `/my/teaching-classes` | T | — | 教师工作台：我的教学班 + 选课人数 + 成绩状态 |
| C-13 | GET | `/my/teaching-classes/{id}/students` | T | — | 同 C-11 语义，教学班归属校验在此处强制 |
| C-14 | GET | `/my/teaching-classes/{id}/roster` | T | — | 成绩录入表（学生 × 成绩列），P2 可导出 CSV |

### 3.5 选课 ENROLL

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| E-01 | POST | `/enrollments` | S | N | **TX** | **核心接口**。body `{ "teachingClassId": 123 }`；返回选课关系 + 最新余量 |
| E-02 | DELETE | `/enrollments/{id}` | S(本人) | Y | **TX** | 退课；`id` 为选课关系 ID（也可支持 `?teachingClassId=` 形式，二者取一实现并写明） |
| E-03 | GET | `/my/enrollments` | S | Y | — | 筛选 `termId/status/courseId/keyword` |
| E-04 | GET | `/my/timetable` | S | Y | — | 按周视图：返回 7×12 网格 + `conflictIds` 标记集合 |
| E-05 | POST | `/enrollments/check` | S | Y | — | **选课前预检**（不落库）：返回 `{canEnroll, reasonCode, reason, conflicts[]}`，供前端提前灰显 |
| E-06 | POST | `/enrollments/batch` | S | N | **TX** | 批量选课（P1）：逐条独立处理，返回成功列表与失败明细 |
| E-07 | GET | `/my/enrollments/statistics` | S | Y | — | 已选/已获学分、GPA、学分进度（P1） |
| E-08 | POST | `/enrollments/admin-enroll` | A | N | **TX** | 管理员代选课。body `{ "studentId": "1001", "teachingClassId": "123" }`；校验同 E-01，`source` 置为 `ADMIN` |

### 3.6 成绩 GRADE

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| G-01 | GET | `/teaching-classes/{id}/grades` | T(本人)/A | Y | — | 成绩录入表：`DRAFT` 可见编辑按钮，`PUBLISHED` 只读 |
| G-02 | POST | `/teaching-classes/{id}/grades` | T(本人) | Y | **TX** | 批量保存（upsert）。空值视为"暂不录入"，允许留空 |
| G-03 | PUT | `/grades/{id}` | T(本人) | N | TX | 单条修改；`PUBLISHED` 拒绝 `60103` |
| G-04 | POST | `/teaching-classes/{id}/grades/publish` | T(本人) | **Y** | **TX** | 发布；存在空缺 → `60102` |
| G-05 | POST | `/teaching-classes/{id}/grades/unpublish` | **A** | N | **TX** | 解锁；**必须**带 `reason`，写审计并 `unlock_count+1` |
| G-06 | POST | `/teaching-classes/{id}/grades/import` | T(本人) | N | TX | P2：上传 CSV → 返回预览差异 `[{enrollmentId, before, after, valid, message}]`，**不立即落库** |
| G-07 | POST | `/teaching-classes/{id}/grades/import/confirm` | T(本人) | N | TX | P2：确认导入，返回成功/失败计数 |
| G-08 | GET | `/my/grades` | S | Y | — | 筛选 `termId`；返回学分、绩点、是否及格 |

### 3.7 统计 STAT

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| S-01 | GET | `/stats/enrollment/overview` | STA | Y | — | 学生视角=本人；教师视角=本人教学班合计；管理员=全校。返回 `{termId,totalEnrollments,totalCapacity,avgFillRate,classCount,studentCount}` |
| S-02 | GET | `/stats/enrollment/by-course` | STA | Y | — | 按课程聚合：`[{courseId,courseName,capacity,enrolledCount,fillRate}]`，支持 `deptId/limit` |
| S-03 | GET | `/stats/enrollment/by-teacher` | A | Y | — | 按教师聚合，P1 |
| S-04 | GET | `/stats/capacity-analysis` | A | Y | — | 满员班 / 闲置班（fillRate<30%）/ 临界班（30%~90%）三个列表 |
| S-05 | GET | `/stats/grade-distribution` | T/A | Y | — | 分数段直方图：`[{bucket:"90-100",count,n}]` |
| S-06 | GET | `/stats/students/{studentId}/summary` | S(本人)/A | Y | — | 已选/已获学分、GPA、修读明细，P1 |

### 3.8 审计 AUDIT

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| L-01 | GET | `/audit-logs` | A | Y | — | 筛选 `userId/module/action/targetType/targetId/result/startTime/endTime` |
| L-02 | GET | `/login-logs` | A | Y | — | 筛选 `username/result/startTime/endTime` |
| L-03 | GET | `/audit-logs/{id}` | A | Y | — | 详情，含 before/after 对比 |

### 3.9 系统 SYS

| # | 方法 | 路径 | 权限 | 幂等 | 事务 | 说明 |
|---|---|---|---|:---:|:---:|---|
| Y-01 | GET | `/actuator/health` | 公开（可配置关闭） | Y | — | 探活 |
| Y-02 | GET | `/enrollment/reconcile` | A | Y | — | **P0 运维接口**：执行 §`02` §6.7 对账查询，返回不一致的教学班列表；为空即一致 |

---

## 4. 错误码表

> 前端依据 `code` 决定交互（提示/置灰/跳转），**不解析 message 文案**。

### 4.1 通用（1xxxx）

| code | HTTP | 含义 | 前端处理 |
|---|---|---|---|
| 0 | 200 | 成功 | — |
| 10000 | 400 | 参数校验失败 | 定位到表单字段 |
| 10001 | 400 | 业务规则拒绝 | 弹出 message |
| 10002 | 403 | 无权限 | 跳 `/403` |
| 10003 | 404 | 资源不存在 | 空态页 |
| 10004 | 409 | 资源冲突（重复提交） | 提示并刷新 |
| 10005 | 503 | 系统繁忙（死锁重试失败） | 提示稍后重试 |
| 10006 | 500 | 服务器内部错误 | 提示并上报 traceId |

### 4.2 认证（2xxxx）

| code | HTTP | 含义 |
|---|---|---|
| 20001 | 401 | 用户名或密码错误（统一话术） |
| 20002 | 401 | 账号已锁定，请 N 分钟后再试 |
| 20003 | 403 | 账号已禁用 |
| 20004 | 400 | 旧密码错误 |
| 20005 | 401 | 登录已过期，请重新登录 |
| 20006 | 401 | 登录凭证无效 |

### 4.3 基础数据（3xxxx）

| code | HTTP | 含义 |
|---|---|---|
| 30101 | 409 | 院系代码已存在 |
| 30102 | 409 | 院系存在下级专业或用户，无法删除 |
| 30201 | 409 | 专业代码已存在 |
| 30301 | 409 | 学期代码已存在 |
| 30401 | 409 | 课程代码已存在 |
| 30501 | 400 | 先修关系将形成循环依赖 |
| 30601 | 409 | 系统参数不可修改（内置只读） |

### 4.4 教学班（4xxxx）

| code | HTTP | 含义 |
|---|---|---|
| 40101 | 409 | 同课程同学期已存在同名教学班 |
| 40102 | 400 | 容量不能小于当前已选人数 |
| 40103 | 409 | 当前状态不允许该操作 |
| 40104 | 400 | 上课时间段互相重叠 |
| 40105 | 409 | 存在选课记录，不允许删除 |

### 4.5 选课（5xxxx）——**验收重点**

| code | HTTP | 含义 | 对应 BR |
|---|---|---|---|
| 50101 | 409 | 你已经选择了该教学班 | BR-04 |
| 50102 | 409 | 名额已满，未能选课 | BR-03 |
| 50103 | 409 | 与《{课程名}》上课时间冲突（{周几} {节次}） | BR-05 |
| 50104 | 400 | 需先修读并通过《{课程名}》 | BR-06 |
| 50105 | 400 | 当前不在选课时间（{开始} ~ {结束}） | BR-02 |
| 50106 | 400 | 该教学班未开放选课 | BR-01 |
| 50107 | 400 | 已过退课截止时间 | BR-08 |
| 50108 | 400 | 教学班已开课，不能退课 | BR-08 |
| 50109 | 400 | 本学期选课学分（{n}）超过上限（{max}） | BR 扩展 |
| 50110 | 400 | 本学期选课学分低于下限（{min}），不能退课 | BR 扩展 |
| 50111 | 400 | 账户状态异常，无法选课 | — |
| 50112 | 503 | 选课人数众多，请稍后重试 | 系统繁忙 |

### 4.6 成绩（6xxxx）

| code | HTTP | 含义 |
|---|---|---|
| 60101 | 400 | 成绩必须在 0 ~ 100 之间 |
| 60102 | 400 | 存在未录入成绩的学生，无法发布 |
| 60103 | 409 | 成绩已发布，如需修改请联系管理员解锁 |
| 60104 | 403 | 只有管理员可以解锁已发布成绩 |
| 60105 | 400 | 解锁必须填写原因（至少 5 个字） |
| 60106 | 400 | 导入文件中存在 {n} 条非法数据，请修正后重试 |

### 4.7 用户（7xxxx）

| code | HTTP | 含义 |
|---|---|---|
| 70101 | 409 | 用户名已存在 |
| 70102 | 409 | 该用户名已存在但已注销，请更换用户名 |
| 70103 | 400 | 内置数据不允许删除 |
| 70104 | 400 | 用户已存在选课记录，不能删除 |

---

## 5. 关键接口详细定义

### 5.1 `POST /api/v1/enrollments`（选课，BR-01~BR-07）

**请求**：`{ "teachingClassId": "1024" }`

**校验顺序（服务端强制，顺序不可调换）**：

| 序 | 校验 | 失败码 |
|---|---|---|
| 1 | 教学班存在且 `status=PUBLISHED` | 10003 / 50106 |
| 2 | 当前时间在学期选课窗口内 | 50105 |
| 3 | 账户为 `STUDENT` 且 `ACTIVE` | 50111 |
| 4 | 无重复有效选课关系 | 50101 |
| 5 | 上课时间无冲突 | 50103 |
| 6 | 先修课已满足 | 50104 |
| 7 | 学期学分不超上限 | 50109 |
| 8 | **原子预占名额**（受影响行数=1） | 50102 |

> 第 8 步是**唯一**的并发安全兜底；前 7 步全在事务内读，为的是给出**可读的具体原因**。即使前 7 步与第 8 步之间发生竞争，第 8 步也会挡住超卖。

**成功响应**：
```jsonc
{
  "code": 0, "message": "选课成功",
  "data": {
    "enrollment": { "id": "90001", "status": "ENROLLED", "enrolledAt": "2025-09-01 10:00:00" },
    "teachingClass": { "id": "1024", "enrolledCount": 31, "capacity": 40, "remaining": 9 }
  },
  "traceId": "a1b2c3d4"
}
```

### 5.2 `DELETE /api/v1/enrollments/{id}`（退课，BR-08~BR-10）

**响应**：
```jsonc
{ "code": 0, "message": "退课成功",
  "data": { "teachingClassId": "1024", "enrolledCount": 30, "remaining": 10 } }
```

**幂等**：已 `WITHDRAWN` → `code=0, message="已退课"`，不重复减名额。

### 5.3 `GET /api/v1/my/timetable`（我的课表）

```jsonc
{
  "code": 0, "message": "success",
  "data": {
    "term": { "id": "3", "code": "2024-2025-1", "name": "2024-2025学年第1学期" },
    "startWeek": 1, "endWeek": 16,
    "courses": [
      { "enrollmentId": "90001", "teachingClassId": "1024", "courseName": "数据结构",
        "teacherName": "李四", "location": "教1-201", "credit": 3.0,
        "schedules": [ { "dayOfWeek": 1, "startSection": 1, "endSection": 2, "weekDesc": "1-16周" } ] }
    ],
    "conflictedEnrollmentIds": ["90007"],
    "summary": { "enrolledCount": 6, "totalCredit": 18.0 }
  },
  "traceId": "a1b2c3d4"
}
```

### 5.4 `POST /api/v1/teaching-classes/{id}/grades`（批量存成绩）

**请求**：
```jsonc
{
  "items": [
    { "enrollmentId": "90001", "regularScore": 88, "midtermScore": 76.5, "finalScore": 82 },
    { "enrollmentId": "90002", "regularScore": 90, "midtermScore": null, "finalScore": null }
  ]
}
```

**行为**：upsert；`null` 表示"暂不录入"；后端按 `sys_config` 权重算 `totalScore` 与 `gradePoint`；任一分数越界 → `60101` 并**整批拒绝**（不做部分成功，避免教师误以为保存成功）。

**权限前置**：`teachingClass.teacherId == 当前用户` 或 `ADMIN`，否则 `10002`。

### 5.5 `POST /api/v1/teaching-classes/{id}/grades/publish`

**请求**：`{ "confirm": true }`

**校验**：
1. 归属校验；
2. 无空缺成绩（`totalScore IS NULL` 的记录）→ `60102`；
3. 全部成绩 `status` 已为 `DRAFT`。

**副作用（同一事务，BR-13）**：所有成绩 → `PUBLISHED`；对应 `enrollment.status` → `COMPLETED`。

---

## 6. 版本演进与兼容策略

| 规则 | 说明 |
|---|---|
| 向后兼容 | 只**新增**字段/接口，不删除、不改语义；新增字段前端按可选处理 |
| 破坏性变更 | 必须升 `/api/v2`，旧版本至少保留一个完整学期周期 |
| 字段弃用 | 标记 `deprecated` 并在响应头 `Deprecation` 说明，过两个版本移除 |
| 错误码新增 | 只增不改；前端遇到未知 code 走"通用错误提示"兜底分支 |

---

## 7. 接口与模块对应关系（供 `03` 实现自检）

| 模块 | 实现端点 | 涉及表 |
|---|---|---|
| `auth` | A-01 ~ A-05 | `sys_user` `sys_login_log` |
| `user` | U-01 ~ U-09 | `sys_user` `sys_user_role` `sys_role` |
| `base` | B-01 ~ B-22 | `dept` `major` `term` `course` `course_prereq` `sys_config` |
| `classroom` | C-01 ~ C-14 | `teaching_class` `class_schedule` `enrollment` |
| `enroll` | E-01 ~ E-07 | `enrollment` `teaching_class` `class_schedule` `course_prereq` `term` |
| `grade` | G-01 ~ G-08 | `grade` `enrollment` `sys_config` |
| `stat` | S-01 ~ S-06 | 只读跨表聚合 |
| `audit` | L-01 ~ L-03 | `sys_audit_log` `sys_login_log` |