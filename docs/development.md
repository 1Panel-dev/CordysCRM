# 开发指南

## 环境准备

安装 Java 21、Git、Node.js 18+ 和 pnpm。仓库自带 Maven Wrapper，无需单独安装 Maven。运行依赖 MySQL 与 Redis；执行使用 Testcontainers 的后端集成测试时还需要 Docker。

不要提交本机路径、数据库密码、API Key 或真实客户数据。后端部署配置以 `installer/conf/cordys-crm.properties` 为参考，前端本地覆盖使用各应用的 `.env.development.local`。

## 初始化与构建

```bash
./mvnw install -N
pnpm --dir frontend install --frozen-lockfile
./mvnw clean package
```

第一条命令安装父 POM，第二条安装锁定的前端依赖，最后一条构建完整项目。只构建后端时可执行：

```bash
./mvnw -f backend/pom.xml clean package -DskipTests
```

## 本地前端开发

先确保后端和所需中间件可访问，再在 `.env.development.local` 配置 `VITE_API_BASE_URL` 与 `VITE_DEV_DOMAIN`。该文件中的个人地址和凭据不得提交。

```bash
pnpm --dir frontend --filter @cordys/web dev
pnpm --dir frontend --filter @cordys/mobile dev
```

移动端在 PC 上模拟登录的步骤参见 `frontend/REDEME.md`。

## 测试与质量检查

```bash
./mvnw -f backend/pom.xml test
pnpm --dir frontend --filter @cordys/web type:check
pnpm --dir frontend --filter @cordys/web lint
pnpm --dir frontend --filter @cordys/web lint:styles
pnpm --dir frontend build
```

前端 `lint` 和 `lint:styles` 会自动修复文件，执行后检查差异。修改移动端时将过滤包替换为 `@cordys/mobile`。提交前应从仓库根目录检查 `git diff`，确认没有构建产物、调试日志、密钥或无关格式化变更。

## 推荐工作流

1. 从 Issue 或需求确认变更边界，重要功能先讨论方案。
2. 阅读根目录及目标子目录的 `AGENTS.md`。
3. 进行小而聚焦的修改，并为行为变化补充测试。
4. 运行受影响模块检查；跨模块变更执行完整构建。
5. 使用 Conventional Commits 编写提交信息，并在 PR 中说明原因、测试结果、文档影响及界面截图。

架构边界参见 `docs/architecture.md`，构建补充说明参见根目录 `BUILD.md`。
