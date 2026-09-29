# 按变更范围验证

以下命令均从仓库根目录执行。先检查 Java、Node.js、pnpm 和依赖环境；集成测试涉及 MySQL/Redis Testcontainers 时确认 Docker 可用。Node/pnpm 的集成构建版本以根 `pom.xml` 的 `node.version`、`pnpm.version` 为准。

## 验证矩阵

| 变更范围 | 至少完成的验证 |
| --- | --- |
| 仅 `.codex/` 或 `AGENTS.md` | `python3 .codex/check.py`、`git diff --check`；无需运行业务构建 |
| 后端领域代码、DTO 或 Mapper | 能复现变更的针对性测试、`crm` 及依赖模块测试 |
| 后端公共框架、权限、数据范围或迁移 | 回归测试及完整后端测试，检查所有消费者和测试资源 |
| 仅 Web 或 Mobile | 对应包类型检查、变动文件代码/样式/格式检查、对应包生产构建 |
| `lib-shared` 或两端共用契约 | Web 和 Mobile 类型检查及生产构建，共享文件格式检查、受影响端侧文件代码/样式检查 |
| API/权限/路由跨前后端 | 后端和受影响端侧的检查，核对请求/响应、错误状态和权限入口 |
| POM、锁文件或集成打包配置 | 对应模块检查及根应用打包；依赖变动按现有工具安装并审查差异 |

命令通过后不重复扩大验证，除非又发生修改、失败或发现未覆盖的影响。检查被依赖、Docker、网络或权限阻塞时，说明实际失败、已完成检查和待补命令；保留环境限制，不修改配置来制造通过。

## 环境准备与本地开发

```bash
# 必要时安装根 Parent POM
./mvnw install -N

# 缺少前端依赖时严格按锁文件安装
pnpm --dir frontend install --frozen-lockfile

# 按任务选择启动的端
pnpm --dir frontend --filter @cordys/web dev
pnpm --dir frontend --filter @cordys/mobile dev
```

依赖下载遵循当前环境的联网审批。Windows 可使用仓库的 `mvnw.cmd`。完整构建流程参见[BUILD.md](../BUILD.md)，前端运行说明参见[frontend/REDEME.md](../frontend/REDEME.md)；旧文档与当前脚本不一致时以 POM/package.json 为准，并按任务范围修正文档。

## 后端

```bash
# crm 及其依赖模块测试
./mvnw -f backend/pom.xml -pl crm -am test

# 公共框架、权限、迁移或跨领域变更：完整后端测试
./mvnw -f backend/pom.xml test

# 后端编译和打包；跳过测试不等于测试通过
./mvnw -f backend/pom.xml clean package -DskipTests
```

定位缺陷时可用 `-Dtest=实际测试类名 -Dsurefire.failIfNoSpecifiedTests=false` 在 `-pl crm -am test` 的 reactor 中运行指定回归测试。该选项仅避免不含目标测试的依赖模块失败；交付仍按上表执行相应范围的测试。不要使用占位类名冒充已执行的验证。

## 前端

```bash
# 类型检查
pnpm --dir frontend --filter @cordys/web type:check
pnpm --dir frontend --filter @cordys/mobile type:check

# 对应包生产构建，包含 vue-tsc --noEmit
pnpm --dir frontend --filter @cordys/web build
pnpm --dir frontend --filter @cordys/mobile build

# 两端或工作区构建配置变化
pnpm --dir frontend build
```

`lib-shared` 的包名是 `@lib/shared`；没有独立类型检查/构建脚本，`test` 也是退出失败的占位脚本，通过两个消费者验证共享变更。`type:check` 使用 `skipLibCheck`，仍需执行相应生产构建。

定向检查使用已安装的工具。下面的 `path/to/changed-file.vue` 是需要替换的示例路径；ESLint/Stylelint 路径相对于对应包，Prettier 路径相对于 `frontend/`。Mobile 将过滤器及格式检查路径改为相应包，共享文件格式检查使用 `packages/lib-shared/` 路径：

```bash
pnpm --dir frontend --filter @cordys/web exec eslint path/to/changed-file.vue
pnpm --dir frontend --filter @cordys/web exec stylelint path/to/changed-file.vue
pnpm --dir frontend exec prettier --check packages/web/path/to/changed-file.vue
```

样式检查仅针对含样式的 Vue、CSS、SCSS、Less 等文件。现有 `lint` 和 `lint:styles` 脚本会对包内文件执行 `--fix`；不得让它们覆盖已有修改或带入无关差异。确需自动修复时限定本次文件并审查结果。

界面变更还需人工检查中英文、权限、加载/空数据/错误/重复提交、长文本及常用桌面宽度或移动触摸区域；提供 PR 所需截图。没有前端自动化测试脚本，不得宣称已通过不存在的测试套件。

## 集成与规范维护

```bash
# Maven 完整应用打包，包括前端
./mvnw clean package

# 团队规范与命令规则自检，不执行其中的危险命令
python3 .codex/check.py

# 所有任务交付前检查空白错误
git diff --check
```

规范自检需要 Python 3.11+ 和 Codex CLI；Windows 可使用 `py -3 .codex/check.py`。仅文档/配置变更不需要安装 Maven 或 pnpm 依赖。
