# 后端开发规范

本文件适用于 `backend/**`，用于补充根目录 `AGENTS.md`。通用提交、PR 和安全要求不在此重复。

## 模块职责

- `framework`
  - 提供安全、文件、MyBatis 扩展、上下文、Excel 和通用配置等基础能力。
  - 只能包含跨业务复用的代码，不得依赖 `crm` 中的具体领域。

- `crm`
  - 承载 CRM 领域、接口和持久化实现。
  - 按 `clue`、`customer`、`opportunity`、`contract`、`order`、`product`、`approval`、`system` 等业务包组织。

- `app`
  - 提供 Spring Boot 启动入口、资源装配和最终制品打包。
  - 不在此编写领域逻辑或数据访问代码。

- 依赖方向
  - 保持 `app -> crm -> framework`。
  - 新增反向依赖或跨领域循环依赖前必须重新设计边界。

## 业务包分层

新增代码应放入对应领域包，并沿用现有结构：

- `controller`
  - 负责路由、参数校验、权限入口和协议转换。
  - 不直接拼装 SQL，不承载复杂业务规则。

- `service`
  - 负责业务校验、事务边界和跨对象编排。
  - 多步骤写操作需要原子性时，在 Service 层明确事务范围。

- `mapper`
  - 负责数据访问。
  - 自定义 Mapper 接口与对应 `Ext*Mapper.xml` 保持同包、同名和语义一致。

- `domain`
  - 表达持久化实体和领域数据。
  - 优先复用现有 `BaseModel`、资源字段等基类，不重复定义公共字段。

- `dto/request` 与 `dto/response`
  - 请求、响应模型分离。
  - API 边界不要直接暴露仅供持久化使用的实体。

- 跨领域调用
  - 通过明确的 Service 边界协作。
  - Controller 不应直接调用其他领域的 Mapper。

## Java 与实现约定

- 使用 Java 21 和四空格缩进。
- 包名统一位于 `cn.cordys` 下。
- 类名使用 PascalCase。
- 方法、参数和字段使用 camelCase。
- 常量使用 `UPPER_SNAKE_CASE`。
- 优先使用项目已有异常、响应模型、权限检查、会话工具和基础 Service。
- 日志应提供业务定位信息，但不得记录密码、Token、完整客户隐私数据或其他敏感字段。
- 注释重点解释业务原因和边界条件，不重复描述代码表面行为。
- 不在功能修改中顺带大规模重排 import 或格式化无关文件。
- 统一用 `@Slf4j` 声明 logger；不手写 `LoggerFactory.getLogger`（除非特殊场景）。

## 数据库与资源文件

- 数据库脚本位于 `backend/crm/src/main/resources/migration/<version>/ddl` 或 `dml`。
- 文件名沿用现有 `V<version>_<sequence>__<description>.sql` 模式。
- 已发布版本的迁移脚本不得随意改写；修复应新增可按顺序执行的脚本。
- 表结构、初始化数据和权限数据应分别放入对应的 DDL、DML 或 permission 脚本。
- Mapper XML 中的字段别名应与 DTO 或 Domain 属性保持一致。
- 修改数据结构时，同步检查查询、导入导出、权限数据和测试夹具。

## 测试规范

- 测试目录：`backend/crm/src/test/java`。
- 测试资源：`backend/crm/src/test/resources`。
- 测试类命名：`*Test.java`。
- 测试包结构：镜像生产代码包结构。
- SQL 初始化与清理脚本：分别使用清晰的 `init_*_test.sql`、`cleanup_*_test.sql` 命名。
- 集成测试可能启动 MySQL 和 Redis Testcontainers，执行前确保 Docker 可用。
- 修复缺陷时，优先增加能够复现问题的回归测试。
- 修改 Controller、Service 或 Mapper 行为时，覆盖成功、校验失败、权限或边界条件。

## 常用验证命令

```bash
# 完整后端测试
./mvnw -f backend/pom.xml test

# 仅构建后端，不运行测试
./mvnw -f backend/pom.xml clean package -DskipTests

# 构建并测试 crm 及其依赖模块
./mvnw -f backend/pom.xml -pl crm -am test
```

## 提交前检查

- 代码位于正确模块和领域包。
- 没有引入 `framework -> crm` 反向依赖。
- API、DTO、Mapper XML 和数据库字段保持一致。
- 数据库变更包含迁移脚本和必要测试数据。
- 已运行受影响模块测试。
- 公共框架、权限或跨领域变更已运行完整后端测试。
- 配置、日志和测试资源中不含真实凭据或客户数据。

