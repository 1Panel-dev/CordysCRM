# 框架通用模块使用说明

## 目录

- [概述](#概述)
- [模块结构](#模块结构)
- [使用指南](#使用指南)
    - [AOP 支持](#aop-支持)
    - [公共功能](#公共功能)
    - [MyBatis 配置](#mybatis-配置)
    - [文件处理](#文件处理)
    - [安全性管理](#安全性管理)

## 概述

本框架是一个企业级应用开发基础设施，提供了多种常用功能模块，包括 AOP 支持、公共工具类、MyBatis 增强、文件处理、安全性管理等。框架旨在简化开发流程，提高代码质量和开发效率。

## 模块结构

```
framework/
├── aop/            # AOP 相关功能
├── common/         # 公共功能
├── config/         # 配置相关
├── file/           # 文件处理
├── mybatis/        # MyBatis 增强
└── security/       # 安全性管理
```

## 使用指南

### AOP 支持

AOP 模块提供了面向切面编程的基础设施，包含以下子模块：

| 子模块 | 说明 |
|--------|------|
| annotation | 定义 AOP 功能的注解 |
| aspect | 实现切面逻辑 |
| builder | 提供构建器模式工具 |
| constants | 定义常量，避免硬编码 |
| dto | 数据传输对象 |
| event | 定义 AOP 相关事件 |

#### 使用示例

##### 1. 基础日志记录

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

说明：
- `resourceId`：业务资源的 ID
- `success`：方法调用成功后记录的日志内容
- 双大括号 `{{}}` 中的内容是 SpEL 表达式，支持调用静态方法、三目表达式等

##### 2. 成功/失败日志记录

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

- `#_errorMsg` 是方法抛出异常后自动获取的错误信息

##### 3. 多条日志记录

```java
@OperationLog(
    module = LogModule.SYSTEM,
    type = LogType.UPDATE,
    operator = "{{#user.name}}",
    resourceId = "{{#user.id}}",
    success = "更新用户基本信息成功",
    extra = "{{#upUser}}"
)
@OperationLog(
    module = LogModule.SYSTEM,
    type = LogType.UPDATE,
    operator = "{{#user.name}}",
    resourceId = "{{#user.id}}",
    success = "更新用户权限成功",
    extra = "{{#upPermission}}"
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

### 公共功能

公共功能模块提供了常用工具和基础功能：

- **constants**：常量定义，避免魔法值
- **exception**：自定义异常体系
- **groups**：分组管理
- **pager**：分页查询功能
- **response**：统一响应格式
- **uid**：唯一标识符生成
- **util**：工具类（包含加密、编码等）

### MyBatis 配置

MyBatis 配置模块增强了 MyBatis 的功能：

- **interceptor**：自定义拦截器
- **lambda**：Lambda 表达式支持，简化查询构建

#### 使用示例

```java
@Resource
private BaseMapper<User> userMapper;

// Lambda 查询示例
List<User> userList = userMapper.selectListByLambda(
    new LambdaQueryWrapper<User>()
        .eq(User::getId, "admin")
);
```

自动生成的 SQL：
```sql
SELECT * FROM user WHERE id = 'admin'
```

### 文件处理

文件处理模块提供了文件操作相关功能：

- **engine**：文件引擎，支持文件上传、下载、存储等操作

### 安全性管理

安全性管理模块提供了身份验证、授权和加密等功能：

- 身份验证
- 权限控制
- 数据加密
- 安全审计

## 更多信息

更多详细信息和高级用法，请参阅各模块的详细文档或源代码注释。