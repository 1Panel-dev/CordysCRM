!!! Tip ""
    Cordys CRM 支持通过 MCP 进行服务端智能创建（线索/客户/商机/联系人）、智能录入（跟进记录）、智能查重等。

## 1 服务配置
!!! Tip ""
    Cordys CRM MCP Server 默认监听端口为 8082，使用 SSE 协议进行通信。基本配置如下：

    ```
    {
       "cordys-crm-mcp-server": {
           "url": "http://<your-server-ip>:8082/sse", // 将 IP 替换为部署机器的地址
           "transport": "sse",
            "headers": {
				"X-Access-Key": "ak", // 替换为实际的 Access Key
				"X-Secret-Key": "sk" // 替换为实际的 Secret Key
			}
        }
    }
    ```
!!! Tip ""
    登录 Cordys CRM 从左下角 【 个人中心 - API Keys 】中获取 Access Key 和 Secret Key


![APIKeys](img/index/api_keys.png)


!!! Tip ""
    Cordys CRM 的 MCP Server 和 基础镜像安装方式相同，具体可以参考：[**Cordys CRM 安装部署**](../installation/1panel_installtion/)



## 2 MCP 工具说明
!!! Tip ""
    Cordys CRM 的 MCP Server 提供以下工具。

| 工具名称              | 功能描述   | 输入参数参考 input_schema       | 输出结果                                                                  |
|-------------------|--------|---------------------------|-----------------------------------------------------------------------|
| search            | 通用查询   | keyword: 检索, pageSize: 10 | {"lead": {"list": null,"total": 0,"pageSize": 10,"current": 1}}       |
| add_lead          | 添加线索   | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |
| add_account       | 添加客户   | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |
| add_opportunity   | 添加商机   | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |
| add_contact       | 添加联系人  | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |
| add_follow_record | 添加跟进记录 | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |
| add_follow_plan   | 添加跟进计划 | 自定义表单内容                   | {"content":[{"type":"text","text":{"name":"xxx公司"}}],"isError":false} |


!!! Tip ""
    `Tools` 将 Cordys CRM 系统中配置的动态表单参数输出为标准的 `input_schema`，客户端需根据该格式传递正确参数。

!!! Tip ""
    以 add_lead 为例，其他工具用法类似，输入参数 (input_schema)
    ```
    {
      "type" : "object",
      "properties" : {
        "公司" : {
          "type" : "string"
        },
        "负责人" : {
          "type" : "string"
        },
        "区域" : {
          "type" : "string",
          "enum" : [ "东区", "北区", "南区"]
        },
        "所属产品" : {
          "type" : "array",
          "items" : {
            "type" : "string"
          }
        },
        "线索来源" : {
          "type" : "string",
          "enum" : [ "官网下载", "400电话" ]
        },
        "联系人姓名" : {
          "type" : "string"
        },
        "联系人手机" : {
          "type" : "string",
          "description" : "手机号"
        }
      },
      "required" : [ "公司", "负责人", "区域", "所属产品", "线索来源"]
    }
    ```
!!! Tip ""
    参数说明：

    | 字段         | 说明       | 示例/可选值                     |
    | ------------ | ---------- | ------------------------------- |
    | `type`       | 参数类型   | `string` / `number` / `array`… |
    | `enum`       | 字段可选值 | 枚举值列表                      |
    | `description`| 字段描述   | 文本说明                        |
    | `required`   | 必填字段   | `true` / `false`                |



## 3 使用示例

###  3.1 MaxKB 集成示例

![agent](img/mcp/agent_mcp.png)


!!! Tip ""

    步骤⼀： 创建或进入一个高级编排类型的应用。

    步骤二： 在「基本信息」里添加两个用户输入，分别是 ak 和 sk，添加两个会话变量，分别是 Cordys CRM 的 Access Key 和 Secret Key。

    步骤三： 添加 Cordys CRM MCP 工具。

    添加 MCP 调用节点

    - MCP Server Config 服务配置：
        ```
        {
            "cordys-crm-mcp-server": {
                "url": "http://<Cordys_MCP_IP>:8082/sse",
                "transport": "sse",
                "headers": {
                    "X-Access-Key": "{{global.ak}}",
                    "X-Secret-Key": "{{global.sk}}"
                }
            }
        }
        ```
    步骤四： 根据自身量身定做的业务场景，设计提示词和工具调用逻辑。

!!! Tip ""
    方式一：Cordys CRM 智能查询效果

![智能查询客户](../img/user_manual/agent-query-customer.png)

!!! Tip ""
    方式二：Cordys CRM 智能添加效果

![智能创建线索](../img/user_manual/agent-Create-lead.png)

###  3.2 开发工具 TRAE 集成示例

!!! Tip ""
    第一步: 配置 `MCP Server`
    ```
    {
        "cordys-crm-mcp-server": {
            "url": "http://<Cordys_MCP_IP>:8082/sse",
            "headers": {
                "X-Access-Key": "ak",
                "X-Secret-Key": "sk"
            }
        }
    }
    ```
!!! Tip ""
    第二步: 通过`Agent`对话验证工具可用性

![Trae.png](img/mcp/trae.png)
