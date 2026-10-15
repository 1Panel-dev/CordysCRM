# 前端约定

- 使用 pnpm。Web 使用 Naive UI，Mobile 使用 Vant；`@lib/shared` 只放两端共享能力，不依赖端侧 UI 或页面。
- 复用现有 `crm-*` 组件、设计变量、模型和 HTTP 封装；共享 API 放入 `lib-shared/api`。`pure` 组件保持业务独立，`business` 组件不直接修改父页面状态。
- API 类型与后端契约一致；新增页面同步必要的路由、权限和菜单映射。
- 用户可见文案使用国际化 Key，中英文同步维护；本地代理配置放各包 `.env.development.local`。
- 前端调整不编写或补充自动化测试，也不新增临时测试脚本；按实际影响检查类型、构建或界面效果。
