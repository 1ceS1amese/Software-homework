# Software-homework：选课与课程管理系统

基于 Spring Boot、Vue 3 + Vite + Nuxt UI 和 MySQL 8 的高校教务课程项目。

学生可浏览课程、选退课、查看课表与成绩；教师可查看教学班、名单并录入成绩；管理员可查询基础数据、统计、用户和审计记录。当前仍有管理端写接口、审计自动落库等功能缺口，不能将本地运行成功等同于完整验收。

## 目录

- [环境](#环境)
- [安装与启动](#安装与启动)
- [配置](#配置)
- [测试与检查](#测试与检查)
- [开发状态](#开发状态)
- [文档](#文档)
- [贡献与问题报告](#贡献与问题报告)
- [许可证](#许可证)

## 环境

已验证环境为 Arch WSL / Linux x86_64（glibc 2.42+）。必须使用 Linux 原生工具；Windows 的旧依赖目录不应直接复用。

| 依赖 | 本地基线与声明 |
|---|---|
| JDK | Java 17；引导器固定 Temurin 17.0.20.1+1 |
| Maven | 3.9.12；后端附 Maven Wrapper |
| Node.js | 22.12+；引导器固定 22.23.3 LTS |
| pnpm | 10.16.1；`frontend/package.json` 的 `packageManager` |
| MySQL | 原生 MySQL 8.0；引导器固定 8.0.46 |
| 前端依赖 | `frontend/package.json` + `pnpm-lock.yaml` |
| 后端依赖 | `backend/pom.xml`，以 registry 实际解析为准 |
| 浏览器测试 | Playwright 自带 Chromium，首次需要下载浏览器 |

原生工具下载地址与 SHA-256 锁定在 `scripts/linux-tools.json`。引导器把工具和运行库安装到被 Git 忽略的 `.local/tools`，不升级系统级 Java/Node。其他系统的准备方式与基础命令要求见 [本地开发指南](docs/12-本地开发指南.md)。

## 安装与启动

以下流程使用原生 MySQL，无需启动 Docker。

```bash
# 希望保存项目的父目录
git clone https://github.com/1ceS1amese/Software-homework.git "UML Software"
cd "UML Software"

# 项目根目录：下载并校验固定版本的 Linux 工具
python3 scripts/bootstrap-linux.py

# 项目根目录：生成配置、初始化本地库、安装依赖、测试与打包后端
./scripts/dev.sh setup

# 项目根目录：启动原生数据库、后端与前端
./scripts/dev.sh start
```

已有代码时，在项目根目录执行 `start`。访问 **<http://localhost:5173/login>**；后端健康检查地址为 <http://127.0.0.1:8081/actuator/health>，必须返回 `status: UP`。

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `teacher1` | `123456` | 教师 |
| `student1` | `123456` | 学生 |

演示账号只应用于本地数据。MySQL 默认监听 `127.0.0.1:3307`，数据保存在 `.local/mysql/data`，业务表和演示数据由 Flyway V1～V4 初始化。已有外部数据库数据不会自动导入（AS-16）。

```bash
# 项目根目录
./scripts/dev.sh status
./scripts/dev.sh logs
./scripts/dev.sh stop       # 保留数据库数据
./scripts/dev.sh start      # 手动再次启动
```

后端源码修改后必须重新 `setup` 打包，再 `restart`。未配置开机自启。日志位置与端口占用、配置缺失、数据库未就绪、版本不匹配等处理见 [开发指南排错](docs/12-本地开发指南.md#6-排错)。

这里的 Vite 开发服务器只用于本地开发；生产运行应使用构建产物、正式数据库、TLS 与独立配置，详见 [部署与运维设计](docs/08-部署与运维设计.md)。当前未授权、未执行远程部署。

## 配置

无真实密钥的字段示例为 [`.env.local.example`](.env.local.example)。首次 `setup` 会生成随机数据库密码与 JWT 密钥到 `.env.local`，并设置文件权限为 `600`。仓库只应提交源码、依赖声明与锁文件、可复用脚本、脱敏文档和配置示例；实际 `.env*` 文件、`.local/`、依赖目录、数据库、日志、私钥和浏览器认证状态必须留在本地。

脚本启动时：`.env.local` 覆盖同名 shell 变量，环境变量再覆盖后端 YAML 默认值。账号密码变更需要同步数据库账号并重启后端；不能只改文件。字段用途、是否必填、默认值、示例与重启要求见 [配置参考](docs/13-配置参考.md)。

## 测试与检查

```bash
# 项目根目录：首次准备浏览器
source scripts/env.sh
(cd frontend && pnpm exec playwright install chromium)

# 项目根目录：完整单元与模拟接口浏览器测试，不需要数据库
./scripts/test.sh

# 项目根目录：提交隐私、格式空白、Shell 语法、类型与构建检查
./scripts/check.sh

# 项目根目录：项目启动后，测试真实本地后端与数据库
./scripts/test.sh live
```

测试必须使用本地开发数据，不得连接生产数据库。当前套件禁止启动 Docker / Testcontainers；CI 样例不配置数据库容器，尚未启用。单个测试、直接执行 Maven / pnpm、测试数据来源和失败产物见 [测试与检查指南](docs/14-测试与检查指南.md)。

## 开发状态

已实现真实 JWT 登录、角色数据隔离、课程与教学班查询、选退课规则、成绩暂存与发布、管理员解锁、真实统计、用户查询与启停。当前运行页面使用 Nuxt UI，旧版未路由的 Element Plus 源码仍保留。

尚未完成：部分管理端增删改、教学班状态流转 HTTP 入口、审计自动落库、成绩批量导入、生产配置，以及并发选课 / 成绩写入的可重复自动化集成测试。完整范围和进展必须以设计契约与 `.ai/` 记录为准。

## 文档

| 文档 | 用途 |
|---|---|
| [00 需求规格说明](docs/00-需求规格说明.md) | 角色、功能、BR 规则与范围 |
| [01 系统总体架构设计](docs/01-系统总体架构设计.md) | 分层、模块与技术选型 |
| [02 数据库设计](docs/02-数据库设计.md) | 表名、字段、枚举的唯一契约 |
| [03 后端模块设计](docs/03-后端模块设计.md) | 业务实现与事务 |
| [04 前端设计](docs/04-前端设计.md) | 页面、路由、组件与权限 |
| [05 接口设计与 API 清单](docs/05-接口设计与API清单.md) | 接口与错误码的唯一契约 |
| [06 非功能与安全设计](docs/06-非功能与安全设计.md) | 性能与安全目标 |
| [07 测试方案](docs/07-测试方案.md) | 业务规则与用例映射 |
| [08 部署与运维设计](docs/08-部署与运维设计.md) | 生产方案，未执行部署 |
| [09 实施计划与里程碑](docs/09-实施计划与里程碑.md) | 实施顺序 |
| [10 验收标准](docs/10-验收标准.md) | 验收门槛 |
| [11 风险与假设登记表](docs/11-风险与假设登记表.md) | 假设、风险与回退 |
| [12 本地开发指南](docs/12-本地开发指南.md) | 系统、安装、运行与排错 |
| [13 配置参考](docs/13-配置参考.md) | 字段、优先级与敏感配置 |
| [14 测试与检查指南](docs/14-测试与检查指南.md) | 完整 / 单个测试、检查与 CI |

验证记录位于 `docs/evidence/`，交接状态见 [`.ai/HANDOFF.md`](.ai/HANDOFF.md)。

## 贡献与问题报告

修改前必须阅读 `AGENTS.md` 与契约；提交前必须运行检查，并说明实际测试结果与边界。分支、PR、问题报告与发布约定见 [CONTRIBUTING.md](CONTRIBUTING.md)，仓库提供 PR 与问题模板。

尚未开始持续发版，因此暂不引入没有实际发布对应的版本承诺或变更记录。开始发版时应补版本规则、升级与数据迁移说明。

README 组织参考 [Standard Readme 中文规范](https://github.com/RichardLitt/standard-readme/blob/main/spec.zh-CN.md)，按本项目的应用与教学场景调整。

## 许可证

当前保留课程学习与教学演示用途，未授予通用开源许可。详见 [LICENSE](LICENSE)；第三方依赖适用各自许可证。
