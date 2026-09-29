# Cordys CRM Agent 入口

团队规范统一维护在 `.codex/`，本文件负责让 Agent 自动发现规范。

- 开始任何任务前，必须读取[项目规范](.codex/guides/project.md)和[工作流程](.codex/workflow.md)。
- 涉及 `backend/**` 时，必须同时读取[后端规范](.codex/guides/backend.md)，即使从仓库根目录启动。
- 涉及 `frontend/**` 时，必须同时读取[前端规范](.codex/guides/frontend.md)；涉及前后端的任务读取两份规范。
- 进入目标目录前检查更具体的 `AGENTS.md` 或 `AGENTS.override.md`，并遵循其适用范围。
- 选择测试、检查或构建命令时，必须读取[验证矩阵](.codex/verification.md)。
- 配置说明、加载边界和维护方式见[团队入口](.codex/README.md)。

新增或调整团队规则时，修改 `.codex/` 中的对应正文；各级 `AGENTS.md` 仅保留入口和作用范围。
