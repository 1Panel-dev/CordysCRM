# 前端开发规范

本文件适用于 `frontend/**`，用于补充根目录 `AGENTS.md`。前端采用 pnpm workspace 管理。

## 工作区职责

- `packages/web`
  - 桌面端应用。
  - 技术栈为 Vue 3、TypeScript、Vite 和 Naive UI。

- `packages/mobile`
  - 移动端应用。
  - 技术栈为 Vue 3、TypeScript、Vite 和 Vant。

- `packages/lib-shared`
  - 保存两端共享的 API、模型、枚举、Hooks、工具、资源和国际化能力。
  - 不得依赖 Naive UI、Vant 或某一端的页面组件。

- 共享边界
  - 仅被单端使用的代码留在 `web` 或 `mobile`。
  - 两端确实共享且不依赖具体 UI 的能力才移入 `lib-shared`。
  - 不为“可能复用”提前抽象公共层。

## 目录与文件放置

- 页面：`src/views/<feature>`。
- 路由：`src/router/routes/modules/<feature>.ts`。
- API：`src/api`；共享接口优先放入 `lib-shared/api`。
- 状态：`src/store`，按现有 Store 模式扩展。
- 组合式函数：`src/hooks`，文件或导出名称以 `use` 开头。
- 通用组件：`src/components/pure`。
- 业务组件：`src/components/business`。
- 端侧配置：`src/config`。
- 页面或组件私有文案：就近放在 `locale/zh-CN.ts` 与 `locale/en-US.ts`。
- 组件目录沿用现有 kebab-case，例如 `crm-input-number/index.vue`。

## Vue 与 TypeScript 约定

- 使用 Composition API，并沿用相邻组件的 `<script setup>` 或既有写法。
- 缩进使用两个空格。
- 字符串使用单引号。
- 语句保留分号。
- 行宽上限为 120 字符。
- 类型和组件名称使用 PascalCase。
- 变量、函数和组合式函数使用 camelCase。
- 常量按相邻模块约定使用 camelCase 或 `UPPER_SNAKE_CASE`。
- 优先提供明确类型，避免无理由的 `any`、类型断言和非空断言。
- Props、Emits 和 API 返回值应有明确类型。
- 不重复声明 `lib-shared/models` 已存在的业务模型。
- 不在模板中编写复杂业务表达式；提取为计算属性或函数。

## 组件与样式

- 优先复用现有 `crm-*` 组件和项目设计变量。
- `pure` 组件不得隐式依赖具体业务页面或全局业务状态。
- `business` 组件应保持清晰输入输出，避免直接修改父页面状态。
- 样式优先使用局部作用域，避免无边界的全局选择器。
- 不随意覆盖 Naive UI 或 Vant 的全局样式。
- 新增交互需考虑加载、空数据、禁用、错误和重复提交状态。
- 桌面端修改需检查常用窗口宽度；移动端修改需检查触摸区域和长文本换行。

## API、路由与状态

- API 请求统一经过现有 HTTP 封装，不在组件中直接创建 Axios 实例。
- 请求参数和响应类型应与后端 DTO 契约一致。
- 新增页面时同步配置路由、权限资源和必要的菜单或路径映射。
- 路由名称、路径和 feature 文件名应与现有模块保持一致。
- 跨页面共享状态才进入 Store；局部 UI 状态保留在页面或组件内部。
- 异步请求必须处理异常和最终状态，确保 loading 能够复位。

## 国际化

- 所有用户可见文案使用国际化 Key。
- 新增或修改文案时同步维护中文和英文文件。
- Key 应表达业务语义，不使用 `text1`、`label2` 等无意义名称。
- 删除功能时同时清理确认不再使用的 Key。
- 提交前检查按钮、表单校验、空状态、提示消息和表格列名的中英文显示。

## 依赖与环境配置

- 使用 pnpm，不混用 npm 或 yarn 安装依赖。
- 不手工编辑 `pnpm-lock.yaml`。
- 仅在明确需要时添加依赖，并检查是否已有同类能力。
- 依赖变更后运行 pnpm 安装并审查锁文件差异。
- 本地代理变量位于各包 `.env.development.local`。
- 常用变量包括 `VITE_API_BASE_URL`、`VITE_DEV_DOMAIN` 和 Web 端的 `VITE_ALLOWED_HOSTS`。
- 不提交个人服务地址、访问令牌或其他凭据。

## 常用命令

```bash
# 安装锁定依赖
pnpm --dir frontend install --frozen-lockfile

# 启动开发服务
pnpm --dir frontend --filter @cordys/web dev
pnpm --dir frontend --filter @cordys/mobile dev

# 类型检查
pnpm --dir frontend --filter @cordys/web type:check
pnpm --dir frontend --filter @cordys/mobile type:check

# 代码与样式检查；命令可能自动修改文件
pnpm --dir frontend --filter @cordys/web lint
pnpm --dir frontend --filter @cordys/web lint:styles

# 构建全部工作区
pnpm --dir frontend build
```

## 提交前检查

- 修改位于正确的端侧包或共享包。
- 没有向 `lib-shared` 引入端侧 UI 依赖。
- API 类型、路由和权限配置已同步。
- 中文与英文文案均已补充。
- 已检查加载、空状态、错误状态和重复提交。
- 已运行受影响包的类型检查、ESLint 和 Stylelint。
- 已运行对应包或全部工作区的生产构建。
- 已检查自动修复结果，未包含无关格式化和本地环境配置。
- 可见界面变更已人工验证，并为 PR 准备截图。
