# 前端约定

- 使用 pnpm。Web 使用 Naive UI，Mobile 使用 Vant；`@lib/shared` 只放两端共享能力，不依赖端侧 UI 或页面。
- 复用现有 `crm-*` 组件、设计变量、模型和 HTTP 封装；共享 API 放入 `lib-shared/api`。`pure` 组件保持业务独立，`business` 组件不直接修改父页面状态。
- API 类型与后端契约一致；新增页面同步必要的路由、权限和菜单映射。
- 用户可见文案使用国际化 Key，中英文同步维护；本地代理配置放各包 `.env.development.local`。
- 前端调整不编写或补充自动化测试，也不新增临时测试脚本；按实际影响检查类型、构建或界面效果。
- 所有颜色样式遵循 `lib-shared/assets/style/themeVariables.less` 声明的变量
- 所有 vue 内置 api 无需声明导入，所有原生组件优先使用`naive-ui`组件库内的组件
- 数据表格使用 CrmTable 组件结合 useTable 钩子
- 代码规范遵循项目内`tsconfig.json`以及`eslint.config.cjs`规则约束，所有代码改动后都必须先执行`type:check`TS类型检查，然后执行`eslint`校验，尽量不执行构建检查
