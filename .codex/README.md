# Codex 项目配置

此目录存放随仓库共享的 Codex 配置。项目必须在 Codex 中标记为 **trusted**，配置、Hooks 和规则才会加载；修改规则后需重启 Codex。

## 文件说明

- `config.toml`：采用 `workspace-write` 沙箱，仅允许写入当前工作区；联网命令需要显式审批。未固定模型或推理级别，以便贡献者沿用个人配置。
- `rules/default.rules`：禁止可能丢失本地修改的 Git 命令，并要求在推送代码或发布制品前确认。
- `../AGENTS.md`：项目开发规范、构建命令、测试要求和提交约定。业务与代码约束应维护在该文件，而不是重复写入 `config.toml`。

## 维护与验证

新增配置项前，请对照 [Codex 配置参考](https://developers.openai.com/codex/config-reference)，不得提交访问令牌、API Key、个人路径或私有服务地址。可使用支持 TOML Schema 的编辑器校验 `config.toml`。

在安装 Codex CLI 的环境中，使用以下命令验证规则：

```powershell
codex execpolicy check --pretty --rules .codex/rules/default.rules -- git push origin feature/example
```

预期结果应包含 `prompt`。同时验证 `git reset --hard` 返回 `forbidden`，而普通构建命令不命中发布规则。
