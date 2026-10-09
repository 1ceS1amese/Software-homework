# TASKS.md

> 状态：`[x]` 完成 / `[~]` 进行中 / `[ ]` 未开始 / `[!]` 阻塞
> 最后更新：2026-10-09 业务功能优化与 Git 协作实践

## 阶段 A — 仓库与约束基线

| ID | 任务 | 产出 | 状态 |
|---|---|---|---|
| A-1 | 扫描工作区与目标目录，确认无既有仓库/文档需继承 | 结论：初始化前无历史代码与文档；机器路径不入库 | [x] |
| A-2 | 建立 `AGENTS.md` 约束文件 | `AGENTS.md` | [x] |
| A-3 | 建立 `.ai/` 过程文档 | PROJECT / TASKS / HANDOFF / WORKFLOW | [x] |
| A-4 | 核对项目工具链与本地记录边界 | `.ai/PROJECT.md` §7；项目环境要求见 docs/12 | [x] |

## 阶段 B — 设计文档集（本轮核心交付）

| ID | 任务 | 产出 | 依赖 | 状态 |
|---|---|---|---|---|
| B-1 | 需求规格说明（含 BR-xx 业务规则与可观察验收标准） | `docs/00-需求规格说明.md` | A | [x] |
| B-2 | 总体架构设计（分层、技术选型、目录结构、部署拓扑） | `docs/01-系统总体架构设计.md` | B-1 | [x] |
| B-3 | 数据库设计（ER、15 表、索引约束、并发与事务方案） | `docs/02-数据库设计.md` | B-1 | [x] |
| B-4 | 后端模块设计（分层职责、核心流程时序、异常与状态机） | `docs/03-后端模块设计.md` | B-2,B-3 | [x] |
| B-5 | 接口设计与 API 清单（统一响应、鉴权、错误码、全量端点） | `docs/05-接口设计与API清单.md` | B-3 | [x] |
| B-6 | 前端设计（页面/路由/组件/状态/权限/主题） | `docs/04-前端设计.md` | B-5 | [x] |
| B-7 | 非功能与安全设计（性能、安全、审计、日志、可观测） | `docs/06-非功能与安全设计.md` | B-4 | [x] |
| B-8 | 测试方案（分层策略、用例集、并发超卖用例） | `docs/07-测试方案.md` | B-3,B-5 | [x] |
| B-9 | 部署与运维设计（环境、编排、Nginx、配置项、回滚） | `docs/08-部署与运维设计.md` | B-2 | [x] |
| B-10 | 实施计划与里程碑（含最小纵向切片定义） | `docs/09-实施计划与里程碑.md` | B-4,B-6 | [x] |
| B-11 | 验收标准（可观察、可执行） | `docs/10-验收标准.md` | B-1,B-8 | [x] |
| B-12 | 风险与假设登记表 | `docs/11-风险与假设登记表.md` | 全部 | [x] |
| B-13 | README 索引 + 全局一致性自检 | `README.md` + 自检记录 | B-1..B-12 | [x] |

> **B-13 自检结论**：文件齐备 12/12；BR-01~16 唯一定义且全引用；15 张表唯一定义；端点登记 76 个；错误码 46 个全部有定义、无游离码；枚举无拼写变体；无未验证的肯定性声明。
> **自检中发现并修复 1 个缺陷**：AC-D-08（BR↔用例可追溯）原不可核对 → 已在 `docs/07` 新增 §10 追溯矩阵。详见 `HANDOFF.md` §4.3。

## 阶段 C — 编码实现

| ID | 任务 | 前置 | 状态 |
|---|---|---|---|
| C-0 | 用户宣布进入编码阶段，更新 PROJECT.md 阶段字段 | 用户明确指令 | [x] |
| C-1 | 脚手架：`backend/` 工程 + Flyway 迁移脚本 + 编译通过 | C-0 | [x] |
| C-2 | 最小纵向切片：建课开班 → 选课 → 录成绩（M3） | C-1 | [x] |
| C-3 | 选课原子性与三重校验落地 | C-2 | [x] |
| C-4 | RBAC 与数据隔离 | C-2 | [x] |
| C-5 | 前端三角色页面 + 构建通过 | C-2 | [x] |
| C-6 | 自动化测试补齐与覆盖率门禁 | C-2..C-5 | [ ] |
| C-7 | 本地端到端证据采集 | C-6 | [x] |
| C-8 | 管理端写操作接口（院系/专业/学期/课程/教学班 CRUD） | C-2 | [ ] |
| C-9 | 教学班状态流转 HTTP 接口 | C-2 | [ ] |
| C-10 | 审计切面：写操作自动落 sys_audit_log | C-2 | [ ] |
| C-11 | 前端接线统计页满员度 / 分数段两个 Tab | C-5 | [ ] |
| C-12 | 移除前端硬编码占位数据 | C-5 | [ ] |
| C-13 | 清理死代码（8 个未引用视图 + 4 个重复 API 文件） | C-5 | [ ] |

## 本轮修复记录（编码轮次 2）

| # | 问题 | 处理 |
|---|---|---|
| 1 | Lombok 1.18.30 与 JDK 26 不兼容，`mvn compile` 报 `TypeTag :: UNKNOWN` | `pom.xml` 覆盖 `lombok.version=1.18.46`；JDK 17 / 26 均编译通过 |
| 2 | 数据库密码为空且硬编码在 `application-dev.yml` | 改为环境变量 `CSMS_DB_*`；连接串加 `createDatabaseIfNotExist=true` |
| 3 | 无可用 MySQL 凭据（root 密码未知） | 用现有 MySQL 二进制在独立目录起 3307 实例，附 `start-dev-db.bat` |
| 4 | V2 种子数据的 BCrypt 哈希与 `123456` 不匹配（实测 `matches=false`），三个演示账号全无法登录 | 新增 `V4__fix_demo_passwords.sql` 修正哈希 |
| 5 | `EnrollmentCommandService` 使用不存在的 `enrollment.user_id` 列 | 改为 `student_id`，并补齐 `course_id/term_id/credit` 必填列 |
| 6 | `EnrollmentRuleChecker` 四条规则全为注释 | 完整实现重复 / 时间冲突 / 先修课 / 学分上下限 |
| 7 | `EnrollmentController` 硬编码 `currentUserId = 1L` | 改从 `CurrentUserHolder` 取，并加角色校验 |
| 8 | `AuthServiceImpl` 为桩，返回字面量 `"sample-jwt-token"` | 真实登录：查库 + BCrypt + JWT + 失败计数 + 锁定 |
| 9 | 缺少教学班 / 基础数据 / 统计 / 成绩 / 用户 / 审计接口 | 新增 6 个 Controller + 4 个查询服务 |
| 10 | 数据库无任何教学数据，页面全空 | 新增 `V3__init_teaching_data.sql`（3 学期 / 12 课程 / 16 教学班 / 17 时段 / 历史成绩） |
| 11 | 管理员登录跳 `/admin/overview`（路由不存在）→ 404 | 改为 `/admin/dashboard` |
| 12 | 前端 `postcss.config.*` 缺失 | 已确认 Tailwind v4 由 Vite 插件处理，无需该文件 |
| 13 | **401 重定向死循环**：`request.ts` 把当前完整 URL（含自身 `redirect` 参数）再次编码，导致 `redirect` 层层嵌套（`%2Flogin%3Fredirect%3D%252F...`），早期桩实现遗留的 `sample-jwt-token` 会持续触发 | `request.ts`：在登录页不再跳转；构造 redirect 前用 `URLSearchParams` 剥掉已有 `redirect`。路由守卫：仅在确有 `userType` 时才从登录页跳走，并先 `fetchMe()` 校验 Token。`LoginView`：登录后优先回 `redirect`，且拒绝指向 `/login` 的目标 |
| 14 | `fetchMe` 失败时调用 `logout()` 会再发一次必然 401 的请求 | 抽出 `clearSession()`，失败路径只清本地状态 |

## 未完成 / 阻塞

| ID | 项 | 原因 | 解除条件 |
|---|---|---|---|
| BLK-1 | 外部数据库连接未配置 | 私有连接参数不应入库 | 开发使用独立实例；外部库通过本地环境变量配置 |
| BLK-2 | 自动化测试缺失 | 时间优先级 | 见 C-6 |
| BLK-3 | 远程部署 | 无授权 | 用户明确授权后执行 `docs/08` 方案 |

## 下一步（唯一）

补齐管理端写操作与审计切面（C-8 ~ C-10），然后补自动化测试（C-6）。

## 2026-10-06 本地环境与工程基线

| ID | 任务 | 证据 / 产出 | 状态 |
|---|---|---|---|
| ENV-01 | 安装锁定的原生 Node / JDK / Maven / MySQL 与浏览器依赖 | scripts/bootstrap-linux.py、linux-tools.json；独立空工具目录安装验证通过 | [x] |
| ENV-02 | 本地启动、停止、再次启动并保留数据 | scripts/dev.sh；5173 / 8081 / 3307 与健康检查通过 | [x] |
| ENV-03 | 后端、前端、真实本地数据库验证 | JUnit 6、Vitest 3、模拟 E2E 12、live 3 与数据库一致性检查通过 | [x] |
| ENV-04 | 文档、配置、检查、贡献与 CI 样例基线 | README、CONTRIBUTING、LICENSE、docs/12～14、.github（CI 样例未启用）、scripts/check.sh/test.sh | [x] |
| ENV-05 | 推送指定 GitHub 仓库 | main 已推送：dae2bf2；已核对契约与凭据排除；CI 以样例提供 | [x] |
| ENV-06 | 限定项目上传范围、脱敏机器记录并检查暂存文件 | .gitignore、scripts/check-privacy.py、脱敏文档与 Windows 参数化脚本；233 个历史文件版本未检出当前真实凭据或运行文件 | [x] |

本轮仅处理本地环境与工程基线，管理端写接口、审计切面、并发与成绩写入自动化覆盖等原有未完成项继续保留。

## 2026-10-09 Git 协作实践

| ID | 任务 | 产出 | 状态 |
|---|---|---|---|
| GIT-01 | 复用公开仓库、重新克隆并保留历史 | feature/course-experience；起始 main d31592e | [x] |
| GIT-02 | 课程查询与已选课程状态防止过期响应覆盖 | CourseSelectionView、enroll store；单元 5 项、课程竞态 E2E 1 项通过 | [x] |
| GIT-03 | 学生成绩随学期刷新与关键词 / 通过结果筛选 | MyGradesView、CreditSummaryView 与回归测试 | [ ] |
| GIT-04 | 教师花名册快速检索与可靠导出 | ClassRosterView、CSV 工具与回归测试 | [ ] |
| GIT-05 | 错误提交、revert 回滚、合并与正常推送 | 可查提交图与回滚证据 | [ ] |
| GIT-06 | 操作指南及文件 / 分支 / 完整历史同步验收 | docs/15、docs/evidence/git-practice-20261009.md | [ ] |
