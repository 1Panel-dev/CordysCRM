## 1 环境要求

!!! Abstract ""
    部署服务器要求：

    - 操作系统: 支持主流 Linux 发行版本（基于 Debian / RedHat，包括国产操作系统，内核版本要求 ≥ 3.10）
    - CPU/内存: 4 核 8 G
    - 磁盘空间: 100G

    提示：Docker 版本太老可能会导致安装失败，建议使用安装包内的 Docker，或者使用 v23.0.5 版本及以上的 Docker。

## 2 下载离线安装包

!!! Abstract ""
    打开[**飞致云开源社区 Cordys CRM 社区版下载**](https://community.fit2cloud.com/#/products/cordys-crm/downloads) 页面下载最新版本安装包，并上传至部署服务器（以 v1.0.0 为例说明安装部署过程）。


## 3 端口要求

!!! Abstract ""
    离线部署 Cordys CRM 需要开通的访问端口说明如下：

| 端口   | 作用       | 说明                        |
|------|:---------|:--------------------------|
| 22   | SSH      | 安装、升级及管理使用                |
| 8081 | Web 服务端口 | 默认 Web 服务访问端口，可根据实际情况进行更改 |
| 8082 | Mcp Server 服务端口 | 默认 Mcp 服务访问端口，可根据实际情况进行更改 |



## 4  安装部署

### 4.1 解压安装包

!!! Abstract ""

    以 root 用户通过 ssh 协议登录到部署服务器, 对安装包进行解压：
    ```
    tar -zxvf cordys-crm-v1.0.0-x86_64-offline-installer.tar.gz
    ```

### 4.2 设置安装参数（可选）

!!! Abstract ""

    Cordys CRM 安装目录、服务运行端口、数据库配置等信息可在安装包解压后中的 install.conf 文件进行配置。

    ```
        # 基础配置
        ## 安装路径, Cordys CRM 配置及数据文件默认将安装在 ${CORDYS_BASE}/cordys 目录下
        CORDYS_BASE=/opt
        ## Cordys CRM 使用的 docker 网络网段信息
        CORDYS_DOCKER_SUBNET=172.30.10.0/24
        ## 镜像前缀
        CORDYS_IMAGE_PREFIX='registry.fit2cloud.com/cordys'
        ## 镜像相关
        CORDYS_IMAGE_NAME=cordys-crm-ce
        CORDYS_IMAGE_TAG=v1.0.0
        
        ## CORDYS 主程序的 HTTP 服务监听端口
        CORDYS_SERVER_PORT=8081
        
        # 数据库配置
        ## 是否使用外部数据库
        CORDYS_EXTERNAL_MYSQL=false
        ## 数据库地址
        CORDYS_MYSQL_HOST=$(hostname -I|cut -d" " -f 1)
        ## 数据库端口
        CORDYS_MYSQL_PORT=3306
        ## 数据库库名
        CORDYS_MYSQL_DB=cordys-crm
        ## 数据库用户名
        CORDYS_MYSQL_USER=root
        ## 数据库密码
        CORDYS_MYSQL_PASSWORD=CordysCRM@mysql
        
        # Redis 配置
        ## 是否使用外部Redis
        CORDYS_EXTERNAL_REDIS=false
        ## Redis 端口
        CORDYS_REDIS_PORT=6379
        ## Redis 密码
        CORDYS_REDIS_PASSWORD=CordysCRM@redis
        ## Redis地址
        CORDYS_REDIS_HOST=$(hostname -I|cut -d" " -f 1)
         
        #MCP Server
        CORDYS_MCP_EXTERNAL_SERVER=false
        CORDYS_MCP_SERVER_PORT=8082 
    ```



### 4.3 执行安装脚本

!!! Abstract ""

    ```
    # 进入安装包解压缩后目录  
    cd cordys-crm-v1.0.0-x86_64-offline-installer

    # 执行安装命令
    bash install.sh
    ```

## 5 登录访问

!!! Abstract ""

    安装成功后即可通过浏览器访问地址 `http://目标服务器 IP 地址:8081`，并使用默认的管理员用户和密码登录 Cordys CRM。

    ```
    用户名：admin

    默认密码：CordysCRM
    ```
![访问Cordys CRM](../img/installation/login.png)
