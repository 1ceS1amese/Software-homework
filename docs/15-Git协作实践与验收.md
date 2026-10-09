# 15 Git 协作实践与验收

## 1. 仓库与验收范围

公开仓库与项目名均为 [Software-homework](https://github.com/1ceS1amese/Software-homework)。用户已确认复用现有仓库，必须保留原始完整历史。根提交 `480c7de` 已包含 README 和源码；本轮实际重新克隆，没有重新创建仓库或勾选 README 初始化选项。

若从零创建其他仓库，应在 GitHub 的 New repository 中填写项目名，选择 Public，勾选 Add a README file 后创建，再克隆。现有仓库应跳过创建步骤，禁止重新初始化或覆盖原历史。

“完全同步”指源码、迁移、依赖声明和锁文件、可复用脚本、脱敏文档及配置示例。真实配置、工具、依赖、数据库、日志与构建产物必须留在本地。完整历史指所有项目分支和标签可达的提交，不包含本机 reflog 与悬空对象。

以下记录本轮从 `d31592e` 开始的实际操作顺序。现在克隆会得到完成后的版本，应阅读提交历史或创建新的练习分支，不能机械重复同名分支及已完成的提交。

## 2. 完整克隆与功能分支

```bash
# 保存项目的父目录；不使用 --depth
git clone https://github.com/1ceS1amese/Software-homework.git Software-homework
cd Software-homework

# 以下命令均在克隆根目录执行
pwd -P
git remote -v
git status --short
git log --graph --decorate --oneline --all
git show 480c7de:README.md

# 使用本人的公开提交身份或 GitHub no-reply 邮箱
git config user.name "你的 GitHub 用户名"
git config user.email "你的 GitHub no-reply 邮箱"
git switch -c feature/course-experience
```

根提交 `480c7de` 是初始提交；`dae2bf2`、`3428d0b`、`d31592e` 是既有工程与隐私改进。认证应由凭据管理器处理，禁止把访问令牌写入 URL。克隆行为依据 [Git 官方文档](https://git-scm.com/docs/git-clone)。

## 3. 三个业务模块分别开发与提交

原生工具和浏览器安装应遵循 [本地开发指南](12-本地开发指南.md) 与 [测试与检查](14-测试与检查.md)。本轮重新克隆后使用已有原生工具，按锁文件全新安装依赖；禁止启动 Docker 测试或把真实配置复制到待提交目录。

```bash
# 克隆根目录：工具与浏览器准备完毕后
source scripts/env.sh
(cd frontend && pnpm install --frozen-lockfile)
```

AS-17 登记本轮三个已有业务模块的优化范围。每个模块必须有实际代码改动、相关测试和独立提交，不能以 Git 工具或文档数量替代业务功能数量。

| 模块 | 独立提交 | 必须可观察的行为 |
|---|---|---|
| 课程查询与已选课程状态 | `620e73d` | 慢的旧查询 / 学期响应不得覆盖新课程列表、已选课程、学分及加载状态；加载中禁止操作旧列表 |
| 学生成绩与学业概览 | `feb0b56` | 切换学期自动刷新；支持课程名与通过结果筛选、重置；筛选不改变全学期汇总，隐藏未发布成绩 |
| 教师花名册 | `c020f45` | 按学号、姓名、教学班检索；导出当前筛选结果；正确处理中文、引号、换行及公式前缀，保留原始学号；空结果禁止导出 |

下面列出主要源码与测试的提交入口。实际提交同时包含相关前端说明、任务状态与脱敏证据，必须先用 `git diff --cached` 审查暂存内容。

```bash
# 模块一完成后：根目录
(cd frontend && pnpm test && pnpm typecheck)
(cd frontend && pnpm exec playwright test tests/e2e/course-search.spec.ts)
git add frontend/src/views/student/CourseSelectionView.vue frontend/src/stores/enroll.ts
git add frontend/src/views/teacher/ClassGradeView.vue
git add frontend/tests/unit/stores.spec.ts frontend/tests/e2e/course-search.spec.ts
python3 scripts/check-privacy.py
git diff --cached --check
git commit -m "fix(enrollment): prevent stale course and enrollment responses"

# 模块二完成后：根目录
(cd frontend && pnpm typecheck)
(cd frontend && pnpm exec playwright test tests/e2e/student-grades.spec.ts)
git add frontend/src/views/student/MyGradesView.vue frontend/src/views/student/CreditSummaryView.vue
git add frontend/tests/e2e/student-grades.spec.ts
python3 scripts/check-privacy.py
git diff --cached --check
git commit -m "feat(grades): refresh by term and filter published student results"

# 模块三完成后：根目录
(cd frontend && pnpm test && pnpm typecheck)
(cd frontend && pnpm exec playwright test tests/e2e/teacher-roster.spec.ts)
git add frontend/src/views/teacher/ClassRosterView.vue frontend/src/utils/rosterCsv.ts
git add frontend/tests/unit/rosterCsv.spec.ts frontend/tests/e2e/teacher-roster.spec.ts
python3 scripts/check-privacy.py
git diff --cached --check
git commit -m "feat(roster): add student search and filtered reliable CSV exports"
```

第一模块同时修复全新锁文件安装发现的三个成绩输入事件隐式 any 类型错误，仅标注字符串 / 数字类型，不改变成绩计算规则。每次提交前必须核对 README 索引、BR / AS 引用以及契约；本轮 `docs/00`、`docs/02`、`docs/05` 与原始版本逐字相同。

## 4. 故意错误提交与回滚

在正确状态 `c020f45` 上，编辑 `frontend/src/utils/rosterCsv.ts`，将 `text.replaceAll('"', '""')` 改为 `text.replaceAll('"', '')`，故意造成姓名中的引号丢失。只用于作业演示，必须立即回滚，禁止交付错误版本。

```bash
# 克隆根目录：编辑上述一行后，确认仅发生预期改动
git diff -- frontend/src/utils/rosterCsv.ts
git add frontend/src/utils/rosterCsv.ts
python3 scripts/check-privacy.py
git diff --cached --check
git commit -m "exercise(roster): intentionally break quoted CSV names for rollback"

# 错误提交 03679c7：此命令预期返回非零；实测 2 失败、2 通过
(cd frontend && pnpm exec vitest run tests/unit/rosterCsv.spec.ts)

# 新增反向提交，保留错误记录及完整协作历史
git revert --no-edit 03679c7

# 回滚提交 75440c4：实测 4/4 通过；整个项目树与正确状态相同
(cd frontend && pnpm exec vitest run tests/unit/rosterCsv.spec.ts)
git diff --exit-code c020f45 HEAD
git rev-parse c020f45^{tree} HEAD^{tree}
```

两个树对象均为 `57902c69ea670ba465ce72a8408f274d501696d2`。本轮没有强制推送或删除已发布提交。回滚行为依据 [Git revert 官方文档](https://git-scm.com/docs/git-revert)。

## 5. 全量检查、推送与合并

```bash
# 克隆根目录；使用 JUnit、Vitest 与模拟接口 Playwright
./scripts/test.sh
./scripts/check.sh

# 推送前必须确认绝对目录、远端地址与干净工作树
pwd -P
git remote get-url origin
git status --short

git push -u origin feature/course-experience
git switch main
git merge --no-ff feature/course-experience \
  -m "Merge three course management improvements and rollback practice"
git push origin main
git log --graph --decorate --oneline --all
```

实际通过：后端 6 项、前端单元 9 项、模拟接口浏览器 16 项，共 31 项；隐私、空白、Shell 语法、TypeScript 与构建检查通过。此轮没有启动 Docker 或真实数据库测试。CI 仍为配置样例，不得把本地结果称为远端 CI 已通过。

合并提交 `12e79b2` 已将 main 从 `d31592e` 更新，保留 10 个可达提交。随后在主分支补全本说明和证据，以普通提交及推送收尾；最终主分支应以最后一次远端读取回执为准。

原工作区两个仅换行差异的文件已逐字备份到被 Git 忽略的私有目录，再恢复为仓库版本；禁止直接覆盖未经检查的功能修改。原工作区应执行：

```bash
# 原工作区根目录：已有修改处理完成后
git fetch --prune origin
git pull --ff-only origin main

# 仅在本地不存在该分支时执行
git branch --track feature/course-experience origin/feature/course-experience
```

## 6. 文件、分支、标签与完整历史验收

以下命令必须在最后一次提交及推送后执行。普通克隆默认只创建默认分支的本地分支，应建立功能跟踪分支再比较，不能把远端分支误判为文件遗漏。

```bash
# 项目根目录：独立镜像读取真实远端全部对象和引用
git fetch --prune origin
verification_dir="$(mktemp -d)"
git clone --mirror https://github.com/1ceS1amese/Software-homework.git \
  "$verification_dir/remote.git"

test -z "$(git status --porcelain --untracked-files=all)"
test "$(git rev-parse --is-shallow-repository)" = false
test "$(git -C "$verification_dir/remote.git" rev-parse --is-shallow-repository)" = false
git fsck --full
git -C "$verification_dir/remote.git" fsck --full

# 分支、标签名称及对象 ID 必须相同
diff <(git for-each-ref --format='%(objectname) %(refname)' refs/heads refs/tags | sort) \
     <(git -C "$verification_dir/remote.git" for-each-ref --format='%(objectname) %(refname)' refs/heads refs/tags | sort)

# 所有分支与标签可达的完整提交集合必须相同
diff <(git rev-list --branches --tags | sort) \
     <(git -C "$verification_dir/remote.git" rev-list --branches --tags | sort)

# 每个分支的文件路径、模式及内容对象必须相同
while IFS= read -r ref; do
  cmp <(git ls-tree -r -z --full-tree "$ref") \
      <(git -C "$verification_dir/remote.git" ls-tree -r -z --full-tree "$ref")
done < <(git for-each-ref --format='%(refname)' refs/heads)

# 再读真实远端，排除核验期间的变化
git ls-remote --refs origin 'refs/heads/*' 'refs/tags/*'
git for-each-ref --format='%(objectname) %(refname)' refs/heads refs/tags
git ls-files | wc -l
git rev-list --count --branches --tags
```

所有 `diff` / `cmp` 必须无差异且退出码为 0；最后两份引用清单应逐项相等。`fsck` 必须退出 0；本机旧 reflog 导致的 dangling 对象提示与对象损坏必须区别对待。远端读取依据 [Git ls-remote 官方文档](https://git-scm.com/docs/git-ls-remote)。

还应从 GitHub 做一次不带深度限制的普通克隆，建立功能跟踪分支，再重复引用、历史和树清单比较，并比较主分支实际工作树每个受控文件的字节及可执行位。合并快照已通过镜像核验；本说明补全推送后必须再次核验最终状态。

## 7. 提交作业时展示什么

实际证据见 [Git 实践记录](evidence/git-practice-20261009.md)。应展示公开仓库 main 页面、三项功能对应代码提交、`git log --graph --all`、错误测试与 revert 记录、最终同步结果。

最终应保留 `main` 和 `feature/course-experience` 两个项目分支、0 个标签、主分支 225 个受控文件。补全说明后完整可达历史应为 11 个提交，包含三个独立业务优化、错误提交和回滚提交。必须运行上述核验，不能仅凭 push 成功认定完全同步。

若页面仍显示 `d31592e` 或 4 commits，应刷新并核对账号、仓库及当前分支。
