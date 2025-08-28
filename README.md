<h1 align="center">Cordys CRM</h1>
<h3 align="center">集信息化、数字化、智能化于一体的开源 CRM 系统</h3>
<p align="center">
  <a href="https://github.com/1Panel-dev/CordysCRM/releases"><img src="https://img.shields.io/github/v/release/1Panel/CordysCRM" alt="Latest release"></a>
  <a href="https://github.com/1Panel-dev/CordysCRM"><img src="https://img.shields.io/github/stars/1Panel-dev/CordysCRM?color=%231890FF&style=flat-square" alt="Stars"></a>    
  <a href="https://hub.docker.com/r/1panel/cordys-crm"><img src="https://img.shields.io/docker/pulls/1panel/cordys-crm?label=downloads" alt="Download"></a><br/>
</p>

<hr/>

**Cordys CRM** 是 [飞致云](https://fit2cloud.com/) 出品的中国首个开源 AI CRM，集信息化、数字化和智能化于一体。

Cordys（/ˈkɔːrdɪs/）由“Cord”（连接之绳）与“System”（系统）融合而成，寓意“关系的纽带系统”，诠释了 CRM 的本质：连接客户，缔造长期价值。

> 注：Cordys CRM 预计在2025年底正式开源并发布，现已开启公测。

<img alt="Cordys CRM Overview" src="https://resource.fit2cloud.com/1panel/cordys-crm/img/overview.png" />

**Cordys CRM** 的核心优势是： 

- **灵活易用**：现代化的使用体验，灵活可配置的表单、流程和权限，轻松助力企业实现销售自动化；
- **安全可控**：私有化部署，确保所有客户数据和业务信息都存储在企业自己的服务器上，企业对数据拥有完全的控制权；
- **BI 加持**：借助 [DataEase](https://github.com/dataease/dataease) 强大的嵌入式能力和 [SQLBot](https://github.com/dataease/SQLBot) 的智能问数能力，轻松助力企业实现高效的数据分析和可视化；
- **AI 加持**：借助 [MaxKB](https://github.com/1Panel-dev/MaxKB) 强大的智能体开发能力，轻松助力企业实现各类销售智能体。

## 快速开始

### 安装部署

准备一台 Linux 服务器，安装好 [Docker](https://docs.docker.com/get-docker/) 后，执行以下一键安装脚本。  

```bash
docker run -d \
  --name cordys-crm \
  --restart unless-stopped \
  -p 8081:8081 \
  -v ~/cordys/logs:/opt/cordys/logs \
  -v ~/cordys/data:/opt/cordys/data \
  1panel/cordys-crm
```

你也可以通过 [1Panel 应用商店](https://1panel.cn/) 快速部署 Cordys CRM。

如果是内网环境，你也可以通过 [离线安装包](https://community.fit2cloud.com//products/cordys-crm/downloads) 部署 Cordys CRM。

### 访问方式

- 在浏览器中打开: http://<你的服务器IP>:8081/
- 用户名: `admin`
- 密码: `CordysCRM`

### 联系我们

安装完成后，可以参考此 [快速入门指南](https://jlx18gc3up.feishu.cn/docx/VMJzdCipnoj5fYxcbakcWQSAnCh)。

在公测期间，欢迎大家参与并反馈宝贵意见。你可以通过以下微信交流群与 Cordys CRM 开源项目组进行交流和反馈。

<image height="150px" width="150px" alt="Cordys CRM QRCode" src="https://resource.fit2cloud.com/1panel/cordys-crm/img/wechat.png" />

## UI 展示

<table style="border-collapse: collapse; border: 1px solid black;">
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://resource.fit2cloud.com/1panel/cordys-crm/img/setting.png" alt="Settings" /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://resource.fit2cloud.com/1panel/cordys-crm/img/rbac.png" alt="RBAC" /></td>
  </tr>
  <tr>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://resource.fit2cloud.com/1panel/cordys-crm/img/opportunity.png" alt="Opportunity List" /></td>
    <td style="padding: 5px;background-color:#fff;"><img src= "https://resource.fit2cloud.com/1panel/cordys-crm/img/opportunity-detail.png" alt="Opportunity Detail" /></td>
  </tr>
</table>

## Roadmap

- [x] 2024.09：写下第一行代码
- [x] 2025.06：v1.0 开发完成
- [x] 2025.07：吃自己的狗粮，成功替换飞致云使用 7 年的 Salesforce CRM
- [x] 2025.08：完成和 SQLBot 和 DataEase 的对接
- [x] 2025.09：v1.1.5 发布，开始公测
- [ ] 2025.10：完成和 MaxKB 的对接
- [ ] 2025.11：代码开源、正式发布

## 飞致云旗下的其他明星项目

- [1Panel](https://github.com/1panel-dev/1panel/) - 现代化、开源的 Linux 服务器运维管理面板
- [MaxKB](https://github.com/1panel-dev/MaxKB/) - 强大易用的企业级智能体平台
- [JumpServer](https://github.com/jumpserver/jumpserver/) - 广受欢迎的开源堡垒机
- [DataEase](https://github.com/dataease/dataease/) - 人人可用的开源 BI 工具
- [SQLBot](https://github.com/dataease/SQLBot/) - 基于大模型和 RAG 的智能问数系统
- [MeterSphere](https://github.com/metersphere/metersphere/) - 新一代的开源持续测试工具
- [Halo](https://github.com/halo-dev/halo/) - 强大易用的开源建站工具

## License

本仓库遵循 [FIT2CLOUD Open Source License](LICENSE) 开源协议，该许可证本质上是 GPLv3，但有一些额外的限制。

你可以基于 Cordys CRM 的源代码进行二次开发，但是需要遵守以下规定：

- 不能替换和修改 Cordys CRM 的 Logo 和版权信息；
- 二次开发后的衍生作品必须遵守 GPL V3 的开源义务。

如需商业授权，请联系：`support@fit2cloud.com`。
