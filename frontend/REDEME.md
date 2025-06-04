# CORDYS CRM 前端工程

## 工程简介

使用 `monorepo` 模式管理前端工程，拆分为`lib-shared`公共资源包、`mobile`移动端工程包和`web`工程包

## 工程结构

```plaintext
├── packages
│   ├── lib-shared            # 公共库模块
│   │   ├── api               # API 封装
│   │   ├── assets            # 静态资源
│   │   ├── enums             # 枚举
│   │   ├── hooks             # 钩子函数
│   │   ├── locale            # 国际化封装
│   │   ├── method            # 工具函数
│   │   ├── model             # 数据模型
│   │   ├── types             # 全局类型声明
│   ├── mobile                # 移动端项目
│   ├── web                   # WEB端项目
```

## 工程初始化&运行

在`/packages`目录下运行依赖安装命令：

```node
pnpm i -w
```

统一构建工程：

```node
npm run build
```

## mobile 移动端工程包

移动端工程由 Vite+Vue3+TS+Vant-UI 基础框架组成。

运行移动端项目：

```node
cd package/mobile
npm run dev
```

在`package/mobile`下单独构建移动端项目：

```node
npm run build
```

### mobile 调试&开发

移动端项目接入了企业微信登录，所以在 PC 上开发调试时需要模拟登录态方便快速开发调试：

1. 先运行 `web` 项目，并登录，登录后打开控制台，将`localStorage`中的`sessionId`和`csrfToken`俩属性及值复制
2. 运行 `mobile` 项目，打开控制台，将第 1 步复制的`localStorage`属性值粘贴后，刷新页面即可模拟完成登录（登录过期的话重新登录`web`后再复制新的属性值到`mobile`页面中替换即可）
3. 在手机端调试，进入页面授权登录后，切换到`我的`菜单，短时间内点击 10 次用户名区域可唤出`Eruda`调试工具

## WEB 端工程包

WEB 端工程由 Vite+Vue3+TS+Naive-UI 基础框架组成。

运行 WEB 端项目：

```node
cd package/web
npm run dev
```

在`package/web`下单独构建 WEB 端项目：

```node
npm run build
```
