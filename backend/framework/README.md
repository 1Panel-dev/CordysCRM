通用模块使用说明

本项目是一个企业级应用框架，包含多种常用的功能模块，包括 AOP 支持、公共功能类、MyBatis 配置、文件处理、安全性管理等。以下是如何使用这些模块的详细说明。

1. AOP 支持

AOP 模块包含多个子模块，包括注解、切面、构建器、常量、DTO 和事件。

- **annotation**：定义了用于 AOP 功能的注解。
- **aspect**：定义了具体的切面逻辑。
- **builder**：提供了构建 AOP 相关对象的构建器模式。
- **constants**：定义了常量类，避免硬编码。
- **dto**：用于 AOP 事件的数据传输对象。
- **event**：定义了与 AOP 相关的事件。

使用示例

设计思路来源：https://github.com/mouzt/mzt-biz-log 

1. 普通的记录日志

- `resourceId`：业务资源的 ID
- `success`：方法调用成功后记录的日志内容
- SpEL 表达式：用双大括号包围的表达式（例如：`{{#user.username}}`）是 SpEL 表达式，Spring 支持的表达式都能在此使用，如调用静态方法、三目表达式等。

```java
@OperationLog(
        module = LogModule.SYSTEM,
        type = LogType.ADD,
        operator = "{{#user.name}}",
        resourceId = "{{#user.id}}",
        success = "添加用户成功",
        extra = "{{#newUser}}"
)
public void addUser(User user) {
    // 业务代码

    // 添加日志上下文
    OperationLogContext.putVariable("newUser", LogExtraDTO.builder()
            .originalValue(null)
            .modifiedValue(user)
            .build());
}
```

2. 记录失败的日志

如果抛出异常则记录失败日志，未抛出异常则记录成功日志。

```java
@OperationLog(
        fail = "业务操作失败，失败原因：「{{#_errorMsg}}」",
        success = "业务操作成功",
        operator = "{{#user.name}}",
        type = LogType.ADD,
        resourceId = "{{#biz.id}}"
)
public boolean create(BizObj obj) {
    OperationLogContext.putVariable("innerOrder", LogExtraDTO.builder()
            .originalValue(obj)
            .modifiedValue(obj)
            .build());
    return true;
}
```

- `#_errorMsg` 是方法抛出异常后的错误信息。

3. 方法记录多条日志

若希望一个方法记录多条日志，可以在方法上重复写两个注解，前提是两个注解不相同。

```java
@OperationLog(
        module = LogModule.SYSTEM,
        type = LogType.UPDATE,
        operator = "{{#user.name}}",
        resourceId = "{{#user.id}}",
        success = "更新用户成功",
        extra = "{{#upUser}}"
)
@OperationLog(
        module = LogModule.SYSTEM,
        type = LogType.UPDATE,
        operator = "{{#user.name}}",
        resourceId = "{{#user.id}}",
        success = "更新用户成功",
        extra = "{{#upUser}}"
)
public void updateUser(User user) {
    // 更新用户
    User preUser = userMapper.selectByPrimaryKey(user.getId());

    // 添加日志上下文
    OperationLogContext.putVariable("upUser", LogExtraDTO.builder()
            .originalValue(preUser)
            .modifiedValue(user)
            .build());
}
```

2. 公共功能模块

该模块提供了常用的公共工具和功能，包括：

- **constants**：常量类，避免魔法值。
- **exception**：自定义异常处理。
- **groups**：分组管理。
- **pager**：分页查询功能。
- **response**：统一的响应格式和响应处理。
- **uid**：生成唯一标识符。
- **util**：工具类，包含常用的加密方法（如 RSA 加密）。

3. 配置模块

配置模块主要用于配置项目中的各种常量、参数和配置信息。

4. 文件处理模块

该模块用于处理与文件相关的操作，包括文件上传、下载、存储等。

- **engine**：文件引擎，用于文件的操作和管理。

5. MyBatis 配置

MyBatis 配置模块提供了对 MyBatis 配置的支持，包括拦截器和 Lambda 表达式支持。

- **interceptor**：自定义拦截器，用于扩展 MyBatis 的功能。
- **lambda**：Lambda 支持，简化 MyBatis 查询语句的构建。
-  Use it like this:
```
    @Resource
    private BaseMapper<User> userMapper;

    List<User> userList = userMapper.selectListByLambda(
            new LambdaQueryWrapper<User>()
                    .eq(User::getId, "admin")
    );
```
- 自动生成SQL
`  SELECT * FROM user WHERE id = 'admin'

`6. 安全性管理模块

该模块用于实现安全性相关功能，如身份验证、授权、加密等。