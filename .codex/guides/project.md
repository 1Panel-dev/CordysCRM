# 项目开发规范

## 项目结构与模块组织

Cordys CRM 是 Maven 多模块项目。`backend/framework` 存放通用基础设施，`backend/crm` 包含领域与 API 代码，`backend/app` 提供 Spring Boot 启动入口。后端测试位于 `backend/crm/src/test`。`frontend/packages` 是 pnpm 工作区，包含桌面端 `web`、移动端 `mobile` 和公共包 `lib-shared`。团队 Agent 规范统一位于 `.codex`，容器文件和示例配置位于 `installer`。

所有 Agent 遵循[统一工作流程](../workflow.md)，涉及后端或前端时同时读取[后端规范](backend.md)或[前端规范](frontend.md)。开发与验证命令统一维护在[验证矩阵](../verification.md)。

## 开发环境

后端使用 Java 21，前端使用 pnpm workspace。Node.js 最低版本约定为 18；团队应优先使用根 `pom.xml` 中的 Node/pnpm 集成构建版本，并满足实际依赖要求。环境与打包说明见[BUILD.md](../../BUILD.md)和[frontend/REDEME.md](../../frontend/REDEME.md)，命令以当前 POM 和 package.json 为准。

## 编码风格与命名约定

Java 使用四空格缩进，包名统一置于 `cn.cordys` 下；类名使用 PascalCase，成员使用 camelCase。领域代码应放入对应的功能包。前端使用两空格缩进、单引号、分号和 120 字符行宽。类型与组件使用 PascalCase，函数及组合式函数使用 camelCase（如 `useLoading`）；文件名遵循相邻代码的既有风格，通常使用 kebab-case。提交前运行 ESLint、Stylelint 和 Prettier。

## 测试规范

后端测试使用 JUnit 5、Spring Boot Test 和 Testcontainers。测试文件命名为 `*Test.java`，包结构应与生产代码一致。变更业务行为时，应补充针对性测试及必要的 SQL 或资源夹具。项目已配置 JaCoCo，虽未规定固定覆盖率，但 PR 应为新增或修改逻辑补充合理覆盖。前端应用目前没有自动化测试脚本，至少应执行类型、代码、样式、格式检查及对应的生产构建；共享包通过两个消费者验证。按[验证矩阵](../verification.md)选择实际命令，并区分通过、失败和未执行。

## 提交与拉取请求规范

提交信息遵循 Commitlint 强制的 Conventional Commits，例如 `fix: correct invoice validation`、`feat: add customer filter` 或 `refactor: simplify AI conditions`。提交主题保持单行英文；存在用户明确提供的附带正文时，主题与正文之间必须保留一个空行，再原样写入正文，正文可包含中文、Markdown 链接及 `--bug`、`--user` 等跟踪信息；没有正文时不添加空行。提交和 PR 应保持小而聚焦，并能独立合并；开发重要功能前先创建 Issue 讨论。PR 需说明变更原因和内容，确认测试结果，并说明文档影响。关联相关 Issue；涉及可见界面变化时附截图。Issue、测试夹具和 PR 中不得包含凭据、客户数据、IP 地址或未脱敏日志。
