# Cordys CRM 团队 Agent 规范

开发规范、协作流程、验证命令和 Codex 配置集中维护在本目录。仓库根目录、`backend/` 和 `frontend/` 的 `AGENTS.md` 只负责自动发现与路径分流；每位团队成员、每个执行或审查 Agent 使用同一份正文。

## 规范入口

| 文件 | 职责 | 读取时机 |
| --- | --- | --- |
| [guides/project.md](guides/project.md) | 项目结构、编码、测试、提交和 PR 约定 | 所有任务 |
| [workflow.md](workflow.md) | 接手任务、实现、审查、协作与交付 | 所有任务 |
| [guides/backend.md](guides/backend.md) | Java 分层、数据访问、迁移和后端测试 | 涉及后端 |
| [guides/frontend.md](guides/frontend.md) | Vue、共享包、组件、国际化和前端依赖 | 涉及前端 |
| [verification.md](verification.md) | 按变更范围选择检查命令及完成标准 | 选择验证命令时 |
| [config.toml](config.toml) | 项目级沙箱、网络和指南发现默认值 | 配置维护 |
| [rules/default.rules](rules/default.rules) | 沙箱外命令的审批策略 | 规则维护 |
| [check.py](check.py) | 文档链接、TOML、指南容量及命令策略自检 | 修改团队规范后 |

## 加载与使用

1. 从仓库或其子目录启动 Codex，检查自动加载的 `AGENTS.md`，按入口读取对应正文。从根目录接手子目录任务时，也必须读取对应领域规范。
2. 审查仓库配置后，将项目标记为 **trusted**，项目 `.codex` 配置和规则才会加载。信任状态保存在个人环境中，不提交个人路径或信任配置。
3. 修改指南后开启新任务；修改命令规则后重启 Codex。不要假设正在运行的 Agent 已经重新加载文件。
4. 其他支持 `AGENTS.md` 的 Agent 使用同样的指南入口；不支持自动发现的工具，应显式读取根入口及其引用文件。`.codex/config.toml` 和 `.rules` 是 Codex 专用配置。

Codex 自动合并从项目根目录到当前工作目录的指南，引用文件由 Agent 按入口要求读取。配置只提供共享默认值；启动参数、个人环境差异和组织强制策略可能影响最终生效值。详见官方 [AGENTS.md 发现机制](https://learn.chatgpt.com/docs/agent-configuration/agents-md)和[配置加载机制](https://learn.chatgpt.com/docs/config-file/config-basic)。

## 命令策略的边界

默认使用 `workspace-write` 和 `on-request`，沙箱内禁用网络，不额外开放个人目录。某些环境仍保护 `.git/`、`.codex/` 和 `.agents/`，修改这些路径需要按当前环境提权；已获得的用户授权应作为审批依据，无需在对话中反复确认。

规则保留对直接 `git reset --hard` 和 `git clean` 的禁止，并对沙箱外的 Git、npm/pnpm 和 Maven 调用统一采用 `prompt`。这覆盖 `git -C`、`pnpm --dir`、Maven 选项及 goal 排列，避免只匹配裸 `push`、`publish`、`deploy`。普通检查和构建优先在沙箱内执行。

`.rules` 按参数前缀匹配，不能替代工作流程中的授权要求，也不覆盖 MCP/连接器或所有脚本、别名和绝对可执行路径。简单 shell 命令链可能被拆分评估；复杂脚本由调用整体及沙箱/审批策略处理。不要用改写命令的方式绕过策略。详见官方[命令规则说明](https://learn.chatgpt.com/docs/agent-configuration/rules)。

## 维护与自检

新增团队规范时修改对应正文；只有新的目录确实需要差异规则时，才增加薄的 `AGENTS.md` 入口。保持相对链接，不新增 `docs/` 目录，不提交凭据、真实客户数据、个人路径或私有服务配置。模型、推理级别、插件、MCP 和审批审核方式沿用个人或组织设置。

维护自检需要 Python 3.11+ 和包含 `execpolicy check` 的 Codex CLI，无需安装 Python 包、联网或调用模型。在仓库根目录运行：

```bash
python3 .codex/check.py
git diff --check
```

Windows 可将 `python3` 替换为 `py -3`。脚本只将示例参数交给规则检查器，**不会执行示例中的 Git、发布或删除命令**。单独排查规则可运行：

```bash
codex execpolicy check --pretty --rules .codex/rules/default.rules -- pnpm --dir frontend publish
```

预期 `prompt`；`git reset --hard` 预期 `forbidden`。新增配置项前对照官方[配置参考与 Schema](https://developers.openai.com/codex/config-reference)，修改规则时同步维护内联示例和自检用例。
