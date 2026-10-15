# 后端约定

- 使用 Java 21，包名位于 `cn.cordys`，日志沿用 `@Slf4j`。
- 保持 `app -> crm -> framework`；`app` 负责启动与装配，`framework` 不依赖 CRM 业务。跨领域通过 Service 协作，Controller 不直接调用其他领域的 Mapper。
- 请求与响应模型分离，不直接暴露持久化实体；复用现有公共模型。自定义 Mapper 与对应 `Ext*Mapper.xml` 保持同包同名，字段别名与模型一致。
- 迁移放在 `backend/crm/src/main/resources/migration/<version>/ddl` 或 `dml`，沿用 `V<version>_<sequence>__<description>.sql`；权限数据使用对应 permission 脚本。已发布迁移通过新增脚本修复。
- 后端行为变化补针对性测试，使用现有 JUnit 5、Spring Boot Test、Testcontainers。测试位于 `backend/crm/src/test`，类名为 `*Test.java`，包结构对应生产代码；SQL 夹具沿用 `init_*_test.sql`、`cleanup_*_test.sql`。
