# 15 Git 协作实践与验收

## 1. 复用现有公开仓库

项目与仓库名为 `Software-homework`，公开仓库为 [1ceS1amese/Software-homework](https://github.com/1ceS1amese/Software-homework)。用户已确认复用现有仓库，必须保留原始完整历史；根提交 `480c7de` 已包含 README 和项目源码。本轮从现有远端重新克隆，不声称本轮重新创建了仓库或点击了 README 初始化选项。

首次从零创建其他仓库时，应在 GitHub 点击 New repository，填写与项目一致的仓库名，选择 Public，勾选 Add a README file 后创建，再克隆。已有仓库应跳过创建步骤，禁止重新初始化或覆盖原历史。

“文件完全同步”指应受版本控制的项目文件。源码、迁移、依赖声明与锁文件、脚本、脱敏文档及配置示例应上传；真实配置、数据库、工具、依赖、构建产物和日志必须留在本地。完整历史指所有项目分支和标签可达的提交，不含本机 reflog 与悬空对象。

## 2. 克隆与功能分支

```bash
# 保存项目的父目录；必须完整克隆，不使用 --depth
git clone https://github.com/1ceS1amese/Software-homework.git Software-homework
cd Software-homework

# 克隆根目录：核对远端、初始 README 与历史
git remote -v
git status --short
git log --graph --decorate --oneline --all
git show 480c7de:README.md

# 使用本人的公开提交身份或 GitHub no-reply 邮箱
git config user.name "你的 GitHub 用户名"
git config user.email "你的 GitHub no-reply 邮箱"
git switch -c feature/course-experience
```

凭据应由 Git 凭据管理器或 `gh auth login` 管理，禁止将访问令牌写入远端 URL。Git 克隆及远端分支行为参考 [官方 clone 文档](https://git-scm.com/docs/git-clone)。

## 3. 三个实际业务功能优化

本轮功能优化默认范围见 AS-17：课程查询防止旧响应覆盖新结果、学生成绩与学业概览同步学期并支持筛选、教师花名册检索与可靠导出。每个业务模块必须有独立代码提交、清晰提交信息以及回归测试，不应把 Git 工具或文档数量当成业务功能数量。

第一模块已经实现：连续课程查询及切换学期时，旧响应不得覆盖当前列表、已选课程、学分或加载状态；查询期间禁止操作旧列表。全新锁文件安装发现的三个成绩输入事件类型错误已显式标注类型，不改变分值规则。

```bash
# 克隆根目录：准备原生工具和前端依赖；无需启动 Docker
source scripts/env.sh
(cd frontend && pnpm install --frozen-lockfile)

# 克隆根目录：每个模块完成后执行相关测试
(cd frontend && pnpm test)
(cd frontend && pnpm typecheck)
(cd frontend && pnpm exec playwright test tests/e2e/course-search.spec.ts)

# 暂存明确的业务文件、测试与相关说明后提交
git add frontend/src/views/student/CourseSelectionView.vue frontend/src/stores/enroll.ts
git add frontend/tests/unit/stores.spec.ts frontend/tests/e2e/course-search.spec.ts
python3 scripts/check-privacy.py
git diff --cached --check
git commit -m "fix(enrollment): prevent stale course and enrollment responses"
```

后续两个模块、实际错误提交、revert 回滚、合并与完整同步核验，应在执行后补全命令与证据。实际回执见 `docs/evidence/git-practice-20261009.md`。
