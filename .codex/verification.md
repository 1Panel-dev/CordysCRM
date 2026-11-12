# 验证命令参考

按改动风险选用，不要求逐项执行。静态文案、局部样式用定向检查和人工确认；行为变化验证受影响路径，共享逻辑关注实际消费者。

## 后端

按影响选择相关测试；公共框架、权限、数据范围或迁移变化扩大验证范围。集成测试使用 `@Tag("integration")`，继承 `BaseTest` 会自动继承标签；其他需要启动完整应用或容器的测试也应显式添加标签。不要按 `*Test` / `*Tests` 后缀区分测试类型。

```bash
./mvnw -f backend/pom.xml -pl crm -am test -DexcludedGroups=integration  # 纯单测，无需 Docker
./mvnw -f backend/pom.xml -pl crm -am test -Dgroups=integration          # 仅集成测试，需要 Docker
./mvnw -f backend/pom.xml -pl crm -am test                              # 默认仍运行全部测试
./mvnw -f backend/pom.xml test                                         # 完整后端
./mvnw -f backend/pom.xml -pl crm -am test -Dtest=FormulaEngineTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw clean package                      # 需要完整应用打包时
```

Surefire 统一使用 UTF-8、headless 模式并预加载 Mockito agent；无需手动配置 JVM 自附加权限。

测试应调用真实被测对象，只模拟外部依赖；不验证 mock 自身的返回值。异步行为等待可观测结果并设置超时，不使用固定休眠。测试数据优先在本用例内创建和清理，断言应限定到本用例的资源，避免其他测试的数据造成假通过。

## 前端

以下以 Web 为例，Mobile 替换过滤器。生产构建已包含类型检查，选择能覆盖本次变化的命令即可；共享包没有独立构建，通过两端消费者验证。

```bash
pnpm --dir frontend --filter @cordys/web test        # 执行现有 Node 单测
pnpm --dir frontend --filter @cordys/web type:check
pnpm --dir frontend --filter @cordys/web build
pnpm --dir frontend build                 # 两端构建
```

代码、样式与格式检查按变动文件调用现有 ESLint、Stylelint、Prettier。包内 `lint`、`lint:styles` 带 `--fix`，注意修复范围；构建可能更新生成声明，交付前审查差异。空白检查可用 `git diff --check`。
