<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![License: Educational Use][license-shield]][license-url]
[![GitHub][github-shield]][contact-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/1ceS1amese/Software-homework">
    <img src="docs/images/logo.svg" alt="选课与课程管理系统标识" width="80" height="80">
  </a>

  <h3 align="center">Software-homework · 选课与课程管理系统</h3>

  <p align="center">
    面向学生、教师与管理员的课程管理 Web 系统，贯通选课、名单与成绩。
    <br />
    <a href="#documentation"><strong>查看项目文档 »</strong></a>
    <br />
    <br />
    <a href="#usage">本地演示</a>
    &middot;
    <a href="https://github.com/1ceS1amese/Software-homework/issues/new?template=bug_report.md">报告问题</a>
    &middot;
    <a href="https://github.com/1ceS1amese/Software-homework/issues/new?labels=enhancement">提出功能建议</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

[![管理员课程目录：真实页面与隔离演示数据][product-screenshot]](docs/16-三端联动与数据库验收.md)

本项目用于课程学习与教学演示，以前后端分离方式实现高校选课与课程管理。前端提交操作，后端执行角色、业务和事务校验，最终写入 MySQL；页面刷新后应保留已保存的数据。

| 使用端 | 当前可用功能 |
|---|---|
| 学生 | 搜索教学班、选课与退课、查看课表、查询已发布成绩和学分概览 |
| 教师 | 维护本人教学班与排课、查询及导出名单、暂存/修改/CSV导入成绩、正式发布成绩 |
| 管理员 | 院系/专业/学期/用户/课程管理、教学班状态与排课、成绩解锁、参数配置、统计和审计 |

学生选退课后，独立教师会话的名单、人数和变更提示应自动更新；教师发布或管理员解锁成绩后，学生成绩与概览应自动更新。在线页面每 5 秒同步，恢复前台时立即补查。“多端”指三个角色的独立 Web 会话及响应式布局，范围见 [AS-18](docs/11-风险与假设登记表.md)。

截图来自全新隔离 MySQL 实例的实际页面，仅包含演示数据。当前进入 1.0.0 发布验收；待办与交付边界见 [Roadmap](#roadmap)。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

- [![Vue][Vue.js]][Vue-url]
- [![Spring Boot][Spring-Boot]][Spring-Boot-url]
- [![MySQL][MySQL]][MySQL-url]

| 层次 | 技术与依赖声明 |
|---|---|
| 前端 | Vue 3、Vite、Nuxt UI、TypeScript、Vue Router、Pinia、Axios；[package.json](frontend/package.json) 与 [pnpm-lock.yaml](frontend/pnpm-lock.yaml) |
| 后端 | Java 17、Spring Boot、Spring Security、MyBatis-Plus、Flyway、JWT；[pom.xml](backend/pom.xml) |
| 数据库 | MySQL 8.0、InnoDB、utf8mb4；表结构与演示数据由 Flyway 管理 |
| 验证 | JUnit、Vitest、Playwright；真实写入测试使用独立原生 MySQL |

当前页面统一使用 Nuxt UI；未接入路由的旧版页面和 Element Plus 依赖已移除。依赖以锁文件和实际构建解析为准。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

以下流程从克隆仓库开始，使用原生工具在本地运行。首次下载需要网络；测试禁止启动 Docker 或 Testcontainers。

### Prerequisites

原生引导器已在 Arch WSL / Linux x86_64、glibc 2.42+ 环境验证。其他系统必须按 [本地开发指南](docs/12-本地开发指南.md) 自行准备对应工具，不应直接使用 Arch 专用运行库。

| 依赖 | 要求 / 引导器固定版本 |
|---|---|
| Java | JDK 17 / Temurin 17.0.20.1+1 |
| Maven | 3.9.12，后端附 Maven Wrapper |
| Node.js | 22.12+ / 22.23.3 |
| pnpm | 10.16.1，与 `packageManager` 一致 |
| 数据库 | 原生 MySQL 8.0 / 8.0.46 |
| 基础工具 | Bash、Git、Python 3.12+、GNU tar/zstd、binutils、OpenSSL、curl、util-linux、fontconfig |

原生二进制下载地址和 SHA-256 见 [linux-tools.json](scripts/linux-tools.json)。工具安装到 Git 忽略的 `.local/tools`，不替换系统级 Java/Node。浏览器运行库与受支持系统的准备方法见 [三端验收指南](docs/16-三端联动与数据库验收.md#2-自动验收)。

### Installation

1. 在希望保存项目的父目录克隆仓库，进入项目根目录。

   ```bash
   git clone https://github.com/1ceS1amese/Software-homework.git "UML Software"
   cd "UML Software"
   ```

2. 在项目根目录下载原生工具并校验摘要。

   ```bash
   python3 scripts/bootstrap-linux.py
   ```

3. 在项目根目录准备配置、数据库与依赖，并测试和打包后端。

   ```bash
   ./scripts/dev.sh setup
   ```

   首次运行会生成权限为 `600` 的 `.env.local`，包含随机数据库密码与 JWT 密钥；Flyway 初始化业务表和演示数据。开发数据位于 `.local/mysql/data`，不自动迁入已有外部数据库。

4. 在项目根目录启动数据库、后端与前端。

   ```bash
   ./scripts/dev.sh start
   ```

   访问 [登录页](http://localhost:5173/login)。[后端健康检查](http://127.0.0.1:8081/actuator/health) 必须返回 HTTP 200 和 `status: UP`；原生 MySQL 默认监听 `127.0.0.1:3307`。

5. 在项目根目录检查运行状态或查看日志。

   ```bash
   ./scripts/dev.sh status
   ./scripts/dev.sh logs
   ```

   日志位于 `.local/logs/`。端口占用、配置缺失、数据库未就绪和版本不匹配的处理见 [排错指南](docs/12-本地开发指南.md#6-排错)。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

### 本地演示

以下账号由 Flyway 创建，密码仅适用于本地演示数据。使用不同浏览器或独立个人资料登录，避免三个角色共用登录存储。

| 用户名 | 密码 | 角色与入口 |
|---|---|---|
| `admin` | `123456` | 管理员 → 课程目录 |
| `teacher1` | `123456` | 教师 → 我的教学班 → 选课名单 / 成绩录入 |
| `student1` | `123456` | 学生 → 课程选择 / 我的课表 / 我的成绩 |

验收时必须依次验证“页面增改删 → 后端校验 → 数据库变化”和“学生操作 → 教师会话更新”“教师发布 → 学生会话更新”。可重复演示、专用夹具账号、数据库核对 SQL 与二次校验步骤见 [三端联动与数据库验收](docs/16-三端联动与数据库验收.md)。

日常命令在项目根目录执行：

```bash
./scripts/dev.sh stop        # 停止服务，保留开发数据库
./scripts/dev.sh start       # 使用已有依赖和后端构建产物启动

# 后端源码修改或构建目录已清理后，必须先重新打包
./scripts/dev.sh setup
./scripts/dev.sh restart
```

Vite 开发服务器用于本地开发。用户指定演示目标已完成独立原生部署、IP 私有 CA HTTPS 和服务重启持久性验证，操作见 [服务器部署指南](docs/17-原生服务器部署与HTTPS.md)，实际地址与新密码通过私有渠道交付。公网验证与访问边界见 [部署回执](docs/evidence/deployment-20261017.md)；本轮完整升级与运维验证按 [发布指南](docs/18-版本发布与升级迁移.md) 执行，完成状态以最新验收记录为准。

### 配置

配置项用途、是否必填、默认值、示例、优先级和重启要求见 [配置参考](docs/13-配置参考.md)，无密钥样例见 [`.env.local.example`](.env.local.example)。使用项目脚本启动时，`.env.local` 覆盖同名 shell 变量，后端环境变量再覆盖 YAML 默认值。数据库密码修改必须同步数据库账号并重启后端。

仓库只应提交源码、依赖声明与锁文件、可复用脚本、必要夹具、脱敏文档和配置示例。真实 `.env*`、运行工具、依赖目录、数据库、日志、私钥与浏览器认证状态必须留在 Git 忽略目录中。

### 测试与检查

以下命令均在项目根目录执行。浏览器及系统运行库须先按 [测试指南](docs/14-测试与检查指南.md) 和 [真实链路指南](docs/16-三端联动与数据库验收.md#2-自动验收) 准备。

```bash
source scripts/env.sh
(cd frontend && pnpm exec playwright install chromium firefox webkit)

./scripts/test.sh             # 后端单元、前端单元、模拟接口 E2E；不需要数据库
./scripts/check.sh            # 暂存隐私、空白格式、Shell 语法、类型检查与生产构建
./scripts/test.sh acceptance  # 全新原生 MySQL：真实写入、跨角色互动、三引擎与视口

# 单个功能复验；下次运行会创建新的隔离实例
./scripts/test.sh acceptance -- course-admin.spec.ts --project=chromium
```

真实写入验收不得读取实际 `.env.local`、复用开发数据或连接生产库。只读真实链路入口为 `./scripts/test.sh live`，须先启动本项目托管的开发实例。完整/单个测试、失败产物与 CI 样例见 [测试与检查指南](docs/14-测试与检查指南.md)；CI 样例尚未启用。

2026-10-16 的两轮核心真实链路测试分别 **60/60 通过**，覆盖 Chromium、Firefox、WebKit 及 390/768/1440 像素；各轮五类数据库异常均为 0。后端单元 6 项、前端单元 9 项、原有界面回归 16 项通过。逐功能基线、修复、复测及运行环境限制见 [验收证据](docs/evidence/acceptance-20261016.md)。真实移动设备、其他操作系统及生产环境不在该结论内。

### Documentation

| 文档 | 内容 |
|---|---|
| [00 需求规格说明](docs/00-需求规格说明.md) | 角色、功能范围与业务规则 |
| [01 系统总体架构设计](docs/01-系统总体架构设计.md) | 分层、模块与技术选型 |
| [02 数据库设计](docs/02-数据库设计.md) | 表名、字段和枚举的唯一契约 |
| [03 后端模块设计](docs/03-后端模块设计.md) | 业务实现与事务 |
| [04 前端设计](docs/04-前端设计.md) | 页面、路由、组件与权限 |
| [05 接口设计与 API 清单](docs/05-接口设计与API清单.md) | 接口和错误码的唯一契约 |
| [06 非功能与安全设计](docs/06-非功能与安全设计.md) | 性能与安全目标 |
| [07 测试方案](docs/07-测试方案.md) | 业务规则与测试用例映射 |
| [08 部署与运维设计](docs/08-部署与运维设计.md) | 生产方案与部署门禁 |
| [09 实施计划与里程碑](docs/09-实施计划与里程碑.md) | 实施顺序 |
| [10 验收标准](docs/10-验收标准.md) | 验收门槛与状态 |
| [11 风险与假设登记表](docs/11-风险与假设登记表.md) | 假设、风险与回退 |
| [12 本地开发指南](docs/12-本地开发指南.md) | 安装、运行与排错 |
| [13 配置参考](docs/13-配置参考.md) | 字段、覆盖优先级与敏感配置 |
| [14 测试与检查指南](docs/14-测试与检查指南.md) | 测试、检查入口与 CI |
| [15 Git 协作实践与验收](docs/15-Git协作实践与验收.md) | 功能提交、回滚、推送与同步核验 |
| [16 三端联动与数据库验收](docs/16-三端联动与数据库验收.md) | 真实落库、三端互动、复测与兼容性 |
| [17 原生服务器部署与 HTTPS](docs/17-原生服务器部署与HTTPS.md) | 独立原生服务、IP 证书、站点配置、验证与维护 |
| [18 版本发布与升级迁移](docs/18-版本发布与升级迁移.md) | 标准 API 兼容、备份恢复、发布切换、回退与同步验收 |

脱敏验证记录位于 [docs/evidence](docs/evidence/)，当前交接状态见 [`.ai/HANDOFF.md`](.ai/HANDOFF.md)。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [x] 三角色 JWT 登录、路由与数据归属隔离
- [x] 课程增改删、关联保护及事务审计
- [x] 选退课、名额原子更新、重复与冲突校验
- [x] 教师名单/人数与学生选退课在线同步
- [x] 成绩批次校验、暂存修改、发布锁及管理员解锁接口
- [x] 学生成绩/学分同步与已完成课程课表保留
- [x] 逐功能独立提交、二次校验、三引擎兼容性与数据库核对
- [x] 原生演示服务器部署、IP 私有 CA HTTPS 与服务重启持久性验证
- [x] 其余管理模块完整写入口与教学班状态流转
- [x] 管理员成绩解锁页面、成绩批量导入
- [x] 覆盖所有操作的审计与完整标准 API 兼容
- [ ] 正式发版、变更记录、升级迁移说明与生产运维验证

版本变化见 [CHANGELOG](CHANGELOG.md)，升级与回退见 [发布指南](docs/18-版本发布与升级迁移.md)，本轮分项实测见 [1.0.0 验收记录](docs/evidence/release-1.0.0-20261018.md)。核心验收通过的范围见 [验收指南](docs/16-三端联动与数据库验收.md#5-尚未覆盖的设计范围)。在线同步没有离线消息中心或历史通知队列；全套原始设计尚未全部交付。新问题与建议可提交到 [Issues][issues-url]。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

修改前必须阅读 [AGENTS.md](AGENTS.md)、交接说明与对应设计契约。按功能点分别提交，PR 应说明具体问题、改变后的行为、实际验证结果及未覆盖边界；完整约定见 [CONTRIBUTING.md](CONTRIBUTING.md)。

1. Fork 仓库，并将自己的仓库克隆到本地。
2. 在项目根目录创建功能分支：`git switch -c feature/your-change`。
3. 完成一个功能点后运行 `./scripts/check.sh` 和对应测试，暂存具体文件，并运行 `python3 scripts/check-privacy.py`。
4. 提交并推送自己的分支：`git commit -m "feat: describe the change"`，`git push -u origin feature/your-change`。
5. 提交 Pull Request；问题报告应包含版本、复现步骤与脱敏日志。

禁止提交真实凭据、个人数据和机器配置。已推送的错误提交应优先使用 `git revert` 生成可追溯的撤销记录，操作范例见 [Git 协作指南](docs/15-Git协作实践与验收.md)。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Top contributors:

<a href="https://github.com/1ceS1amese/Software-homework/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=1ceS1amese/Software-homework" alt="项目贡献者" />
</a>

<!-- LICENSE -->
## License

本项目用于课程学习与教学演示，当前未授予通用开源许可证；其他使用、再分发或商业用途应先取得著作权人许可。完整声明见 [LICENSE](LICENSE)，第三方依赖适用各自许可证。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

维护者：[1ceS1amese][contact-url]。问题与功能建议请通过 [GitHub Issues][issues-url] 联系。

项目仓库：[Software-homework][project-url]。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

- [Best-README-Template](https://github.com/othneildrew/Best-README-Template)：本 README 的章节顺序、导航、徽章与页内返回结构。
- [Spring Boot](https://spring.io/projects/spring-boot)、[Vue](https://vuejs.org/)、[Vite](https://vite.dev/) 和 [Nuxt UI](https://ui.nuxt.com/)：应用框架与组件。
- [Playwright](https://playwright.dev/)、[Vitest](https://vitest.dev/) 和 [JUnit](https://junit.org/)：测试工具。
- [Shields.io](https://shields.io/) 与 [contrib.rocks](https://contrib.rocks/)：仓库徽章与贡献者展示。

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[project-url]: https://github.com/1ceS1amese/Software-homework
[contributors-shield]: https://img.shields.io/github/contributors/1ceS1amese/Software-homework.svg?style=for-the-badge
[contributors-url]: https://github.com/1ceS1amese/Software-homework/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/1ceS1amese/Software-homework.svg?style=for-the-badge
[forks-url]: https://github.com/1ceS1amese/Software-homework/network/members
[stars-shield]: https://img.shields.io/github/stars/1ceS1amese/Software-homework.svg?style=for-the-badge
[stars-url]: https://github.com/1ceS1amese/Software-homework/stargazers
[issues-shield]: https://img.shields.io/github/issues/1ceS1amese/Software-homework.svg?style=for-the-badge
[issues-url]: https://github.com/1ceS1amese/Software-homework/issues
[license-shield]: https://img.shields.io/badge/license-Educational%20Use-475569?style=for-the-badge
[license-url]: https://github.com/1ceS1amese/Software-homework/blob/main/LICENSE
[github-shield]: https://img.shields.io/badge/GitHub-1ceS1amese-181717?style=for-the-badge&logo=github
[contact-url]: https://github.com/1ceS1amese
[product-screenshot]: docs/images/course-management.png
[Vue.js]: https://img.shields.io/badge/Vue.js-35495E?style=for-the-badge&logo=vuedotjs&logoColor=4FC08D
[Vue-url]: https://vuejs.org/
[Spring-Boot]: https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[Spring-Boot-url]: https://spring.io/projects/spring-boot
[MySQL]: https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white
[MySQL-url]: https://dev.mysql.com/
