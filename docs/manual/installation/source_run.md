# 构建过程说明

## 📋 源代码会在 11 月份公测结束后开源，届时根据以下文档编译运行。

本项目包含多个模块，构建分为基础配置安装、后端构建、前端构建和整体打包四个部分。

---

## 🔧 1. 安装基础 POM

该命令会将 `parent pom` 安装到本地 Maven 仓库，使其他外部子工程可以获取最新的 `<properties>` 配置。

```bash
./mvnw install -N
```

---

## 🖥️ 2. 后端构建

执行以下命令构建后端模块（如 `framework`、`crm`、`app` 等）并安装到本地仓库：

```bash
./mvnw clean install -DskipTests -DskipAntRunForJenkins --file backend/pom.xml
```

> ✅ 参数说明：
>
> * `-DskipTests`: 跳过测试用例执行。
> * `-DskipAntRunForJenkins`: 跳过 Jenkins 使用的 Ant 任务。

---

## 💻 3. 前端构建

前端构建请参考 [`/frontend/REDEME.md`](./source_run.md) 中的具体说明。

> 📌 提示：确保已正确安装 Node.js 和依赖环境。

---

## 📦 4. 整体打包

使用以下命令进行完整的构建与打包：

```bash
./mvnw clean package
```
