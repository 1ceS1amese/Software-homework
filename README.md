# 选课与课程管理系统（CSMS）

> Course Selection and Management System —— 基于 Spring Boot + Vue 3 的高校课程管理系统

一套面向高校教务场景的 Web 系统，覆盖 **开课 → 选课 → 成绩** 全链路，服务学生、教师、管理员三类角色。

---

## 功能特性

| 角色 | 功能 |
|---|---|
| **学生** | 浏览与筛选教学班、在线选课 / 退课、周视图课表、成绩查询、学分统计 |
| **教师** | 我的教学班、选课名单（可导出）、成绩录入与发布 |
| **管理员** | 用户管理、院系 / 专业 / 学期 / 课程 / 教学班管理、统计报表、操作审计、系统参数 |

**系统设计上的三个重点：**

- **选课不超卖** —— 容量判定与自增在同一条原子 SQL 中完成，由数据库行锁保证，杜绝并发下的名额超发。
- **选课规则可解释** —— 重复选课、上课时间冲突、先修课未修读、学分超限，每一条被拒绝都能给出明确原因而非一个 500。
- **操作可追溯** —— 选退课、成绩发布与解锁、基础数据变更全部留痕（操作人、时间、前后值、IP）。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3.5（`<script setup>` + TypeScript 6）、Vite 8、Nuxt UI 4、Vue Router 5、Pinia 4、Axios 1.20、Tailwind CSS 4 |
| 后端 | Spring Boot 3.2.5、Spring Security、MyBatis-Plus 3.5.6、JJWT 0.12.5、Flyway、Java 17 |
| 数据库 | MySQL 8（InnoDB / utf8mb4） |

---

## 系统架构

```
浏览器 ──HTTPS/JSON──▶ Nginx ─┬──▶ Vue 3 SPA（静态资源）
                             └──▶ Spring Boot ──JDBC──▶ MySQL 8
                                          └──▶ Flyway 迁移 + 审计日志
```

后端按 **接入层 → 应用层 → 领域层 → 数据层** 四层组织，包级划分为
`auth` `user` `base` `classroom` `enroll` `grade` `stat` `audit` `security` `common` 十个模块。

统一响应体：`{ code, message, data, traceId }`，其中 `code = 0` 表示成功。

---

## 目录结构

```
.
├── backend/                     Spring Boot 工程
│   ├── pom.xml
│   ├── build.bat                Windows 快速编译脚本
│   └── src/main/
│       ├── java/com/csms/       按业务模块划分的包
│       └── resources/
│           ├── application.yml / application-dev.yml
│           └── db/migration/    Flyway 迁移脚本
│
├── frontend/                    Vue 3 工程
│   └── src/
│       ├── api/                 按模块封装的接口调用 + 类型定义
│       ├── router/              路由表与全局守卫
│       ├── stores/              Pinia 状态
│       ├── layouts/             三种布局：登录 / 默认 / 管理台
│       ├── components/          通用组件
│       ├── directives/          权限指令
│       └── views/               页面（按角色分目录）
│
├── start-dev-db.bat              启动本地开发数据库（可选，见下）
├── docs/                        设计文档（需求、架构、数据库、接口、测试、部署）
└── .ai/                         项目过程文档
```

---

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | **17 或更高** |
| Maven | 3.9+ |
| Node.js | 20+ |
| pnpm | 9+ |
| MySQL | 8.x（已启动，能连上即可） |

> 工程已固定 `Lombok 1.18.46`，在 **JDK 17 与 JDK 26 上均实测编译通过**，无需手动切换 `JAVA_HOME`。
> 若你的 JDK 过旧（< 17）仍会失败，请升级 JDK。

### 1. 准备数据库

数据库 `csms` **不需要手工创建** —— JDBC 连接串已带 `createDatabaseIfNotExist=true`，首次启动时驱动自动建库，随后 Flyway 依次执行四个迁移脚本建表并写入演示数据。

你只需要一个可用的 MySQL 实例。二选一：

**A. 用你已有的 MySQL**（若你知道 root 密码）
无需额外操作，直接把端口与密码填进下一步的环境变量即可。

**B. 用独立的开发实例**（不知道现有密码时）
仓库提供 `start-dev-db.bat`：用你机器上已有的 MySQL 二进制，在独立数据目录启动一个 3307 端口实例，**不影响原有 MySQL 服务与数据**。

首次使用需初始化数据目录：

```bash
"D:\MYSQL Server\bin\mysqld.exe" --initialize-insecure ^
  --basedir="D:\MYSQL Server" ^
  --datadir="D:\code\homework\.csms-devdb\data"
```

之后每次启动开发库只需运行 `start-dev-db.bat`（关闭窗口即停止）。

### 2. 配置数据库连接

地址与账号密码通过**环境变量**注入，不会写进仓库：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `CSMS_DB_HOST` | `localhost` | 数据库主机 |
| `CSMS_DB_PORT` | `3306` | 端口（用开发实例时填 `3307`） |
| `CSMS_DB_NAME` | `csms` | 库名 |
| `CSMS_DB_USERNAME` | `root` | 账号 |
| `CSMS_DB_PASSWORD` | 空 | 密码 |

### 3. 启动后端（端口 8081）

**方式一（推荐）**：运行 `backend/run.bat`，按提示输入密码即可。

**方式二**：手动设置环境变量

```powershell
cd backend
$env:CSMS_DB_PORT="3307"        # 使用开发实例时
$env:CSMS_DB_PASSWORD="你的密码"
mvn spring-boot:run
```

**仅编译**（不启动）：`backend/build.bat` 或 `mvn clean compile`。

### 4. 启动前端（端口 5173）

```bash
cd frontend
pnpm install
pnpm dev
```

访问 <http://localhost:5173>。开发服务器已将 `/api` 请求代理到 `http://localhost:8081`，无需处理跨域。

### 默认演示账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `teacher1` | `123456` | 教师 |
| `student1` | `123456` | 学生 |

> 演示账号仅用于本地开发，部署前请务必修改密码。

---

## 常用命令

| 目录 | 命令 | 说明 |
|---|---|---|
| `backend/` | `mvn clean compile` | 编译 |
| `backend/` | `mvn spring-boot:run` | 启动（8081） |
| `backend/` | `mvn clean package` | 打包 |
| `frontend/` | `pnpm dev` | 开发服务器（5173） |
| `frontend/` | `pnpm build` | 生产构建 + 类型检查 |
| `frontend/` | `pnpm test` | Vitest 单元测试 |
| `frontend/` | `pnpm test:e2e` | Playwright 浏览器测试（使用测试接口桩，需本机 Edge，无需后端） |
| `frontend/` | `pnpm test:e2e:live` | 连接已启动的本地后端与开发库，执行三角色只读链路测试 |
| `frontend/` | `pnpm preview` | 预览生产构建产物 |

---

## 文档

| 文档 | 内容 |
|---|---|
| [需求规格说明](docs/00-需求规格说明.md) | 角色权限矩阵、功能需求、业务规则 BR-01~BR-16、范围边界 |
| [系统总体架构设计](docs/01-系统总体架构设计.md) | 分层架构、技术选型、工程目录、架构决策记录 |
| [数据库设计](docs/02-数据库设计.md) | ER 图、15 张表定义、索引、枚举、并发与事务方案 |
| [后端模块设计](docs/03-后端模块设计.md) | 模块职责、核心流程时序、状态机、异常体系 |
| [前端设计](docs/04-前端设计.md) | 路由表、页面设计、状态管理、权限控制 |
| [接口设计与 API 清单](docs/05-接口设计与API清单.md) | 统一响应、鉴权、接口清单、错误码表 |
| [非功能与安全设计](docs/06-非功能与安全设计.md) | 性能预算、安全基线、审计、可观测性 |
| [测试方案](docs/07-测试方案.md) | 测试分层、用例集、并发选课专项 |
| [部署与运维设计](docs/08-部署与运维设计.md) | 环境规划、容器编排、发布与回滚 |
| [实施计划与里程碑](docs/09-实施计划与里程碑.md) | 阶段划分与交付顺序 |
| [验收标准](docs/10-验收标准.md) | 验收项与状态 |
| [风险与假设登记表](docs/11-风险与假设登记表.md) | 假设、风险与回退方案 |

---

## 开发状态

> **核心业务链路已跑通（M3 纵向切片完成），管理端与统计为只读实现。**

### 已实现并实测通过

| 模块 | 能力 |
|---|---|
| 认证 | 真实登录（BCrypt 校验 + JWT 签发）、当前用户、改密、按钮级权限码、连续 5 次失败锁定账号 |
| 选课 | 选课 / 退课 / 重选 / 我的选课 / 我的课表 / 选课预检 / 教学班名单 |
| 选课规则 | 重复选课、时间冲突（含周次区间）、先修课、学分上限，**每条都返回可读中文原因** |
| 并发安全 | 单条原子 SQL 控制容量，**20 线程抢 1 个名额实测 1 成功 / 19 拒绝，零超卖** |
| 成绩 | 批量暂存、服务端按配置权重算总评与绩点、发布（单向锁）、管理员解锁（需填原因并计数） |
| 教学班 | 列表分页筛选（学期 / 课程 / 教师 / 关键词 / 仅看有余量）、详情、教师工作台 |
| 基础数据 | 院系、专业、学期、课程、系统参数查询；系统参数修改 |
| 统计 | 选课总览、按课程聚合、满员度分析、学生学分与 GPA 汇总（全部实时聚合，无假数据） |
| 用户与审计 | 用户列表 / 详情 / 启停 / 统计，操作审计日志与登录日志检索 |
| 权限隔离 | 学生访问管理端接口返回 `10002`，无 Token 返回 `401` |

### 尚未实现

| 项 | 说明 |
|---|---|
| 管理端写操作 | 院系 / 专业 / 学期 / 课程 / 教学班的增删改接口（前端对应按钮也尚未接线） |
| 教学班状态流转接口 | `TeachingClassStateMachine` 已实现但未暴露 HTTP 入口 |
| 审计切面 | 审计表可查，但写操作尚未自动落审计（当前只有种子数据） |
| 成绩批量导入 | CSV 导入（P2） |
| 测试覆盖缺口 | 已有后端 JUnit 规则与异常映射测试、前端 Vitest / Playwright 测试，以及连接真实数据库的只读浏览器测试；并发与成绩写入流程尚未纳入可重复运行的自动化集成测试 |
| 部署配置 | 无 Dockerfile / Compose / 生产 profile |

### 已知限制

- 当前路由中的页面已移除硬编码数据回退，接口失败会显示错误或空态；仓库中未接入路由的旧版页面仍保留待清理。
- 统计页已接入 `capacity-analysis`；全校成绩分布因后端缺少汇总接口，明确显示暂不可用。
- 成绩录入页从 `sys_config` 读取权重，最终总评始终以服务端计算为准。
- 管理端院系、专业、学期、课程、教学班写入接口尚未实现，因此相应页面只展示实际可用的查询能力。

具体进展见 [`.ai/TASKS.md`](.ai/TASKS.md) 与 [`.ai/HANDOFF.md`](.ai/HANDOFF.md)。

---

## 贡献

1. 从 `main` 切出特性分支：`git checkout -b feature/xxx`
2. 变更数据表 / 接口时，**必须同步更新 `docs/` 中对应文档**（它们是契约的唯一出处）
3. 提交前确保 `backend` 编译通过、`frontend` 的 `pnpm build` 通过
4. 合并请求需说明变更范围与验证方式

## 许可证

本项目仅用于课程学习与教学演示。
