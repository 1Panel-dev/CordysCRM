# 云市场部署指南

!!! Abstract ""

    本指南将介绍如何通过阿里云云市场购买、部署和使用 **Cordys CRM** 。

## 1 购买镜像

!!! Abstract ""

    - Cordys CRM 已经上架到阿里云云市场，您可以通过以下链接直接购买镜像：
    [Cordys CRM 云市场购买链接](https://market.aliyun.com/detail/cmjj00071934)
    
    - 您也可以自行购买阿里云服务器，并在选择镜像时搜索 **Cordys CRM**，即可快速选择镜像进行部署。

    **注意：** 购买时请选择合适的实例规格，建议 2C/4G 及以上。


## 2 启动服务

!!! Abstract ""

    镜像启动后，您可以通过浏览器访问以下地址登录 Cordys CRM：

    ```
    服务访问地址: http://服务器IP:8081
    默认用户名: admin
    默认密码: CordysCRM
    ```

    首次登录后，建议及时更改密码并进行其他安全设置。

## 3 开放端口

!!! Abstract ""

    - 为了确保外部能够正常访问 Cordys CRM 服务，您需要在阿里云服务器的安全组规则中开放 `8081` 端口。

    - 具体的开放步骤可以参考阿里云的 [端口放行教程](https://help.aliyun.com/document_detail/25471.html)。
