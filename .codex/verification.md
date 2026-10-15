# 验证命令参考

按改动风险选用，不要求逐项执行。静态文案、局部样式用定向检查和人工确认；行为变化验证受影响路径，共享逻辑关注实际消费者。

## 后端

按影响选择相关测试；公共框架、权限、数据范围或迁移变化扩大验证范围。Testcontainers 集成测试需要 Docker。

```bash
./mvnw -f backend/pom.xml -pl crm -am test  # CRM 及依赖模块
./mvnw -f backend/pom.xml test             # 完整后端
./mvnw clean package                      # 需要完整应用打包时
```

## 前端

以下以 Web 为例，Mobile 替换过滤器。生产构建已包含类型检查，选择能覆盖本次变化的命令即可；共享包没有独立构建，通过两端消费者验证。

```bash
pnpm --dir frontend --filter @cordys/web type:check
pnpm --dir frontend --filter @cordys/web build
pnpm --dir frontend build                 # 两端构建
```

代码、样式与格式检查按变动文件调用现有 ESLint、Stylelint、Prettier。包内 `lint`、`lint:styles` 带 `--fix`，注意修复范围；构建可能更新生成声明，交付前审查差异。空白检查可用 `git diff --check`。
