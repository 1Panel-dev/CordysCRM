## 1 环境要求


!!! Abstract ""

    **部署服务器要求：**

    * 操作系统：支持主流 Linux 发行版本（基于 Debian / RedHat，包括国产操作系统，内核版本要求 ≥ 3.10）
    * CPU/内存: 4 核 8 G
    - 磁盘空间: 100G

## 2 端口要求

!!! Abstract ""

    在线部署 Cordys CRM 需要开通的访问端口说明如下：

| 端口   | 作用              | 说明                        |
|------|:----------------|:--------------------------|
| 22   | SSH             | 安装、升级及管理使用                |
| 8081 | Web 服务端口        | 默认 Web 服务访问端口，可根据实际情况进行更改 |
| 8082 | Mcp Server 服务端口 | 默认 Mcp 服务访问端口，可根据实际情况进行更改 |




## 3 安装部署

!!! Abstract ""
    在配置 Docker 环境的操作系统中，进行以下操作：

    ```
    docker run -d \
    --name cordys-crm \
    --restart unless-stopped \
    -p 8081:8081 \
    -p 8082:8082 \
    -v ~/cordys:/opt/cordys \
    1panel/cordys-crm
    ```

# 4 登录访问

!!! Abstract ""

    安装成功后即可通过浏览器访问地址 `http://目标服务器 IP 地址:8081`，并使用默认的管理员用户和密码登录 Cordys CRM。

    ```
    用户名：admin

    默认密码：CordysCRM
    ```
![访问 Cordys CRM](../img/installation/login.png)

