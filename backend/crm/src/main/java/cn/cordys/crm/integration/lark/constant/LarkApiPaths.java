package cn.cordys.crm.integration.lark.constant;

public interface LarkApiPaths {

    String LARK_APP_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    String LARK_USER_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    String LARK_USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";


    String LARK_SUITE_APP_TOKEN_URL = "https://open.larksuite.com/open-apis/auth/v3/app_access_token/internal";
    String LARK_SUITE_USER_TOKEN_URL = "https://open.larksuite.com/open-apis/authen/v1/oidc/access_token";
    String LARK_SUITE_USER_INFO_URL = "https://open.larksuite.com/open-apis/authen/v1/user_info";

    /**
     * 获取子部门列表
     * GET
     * https://open.feishu.cn/open-apis/contact/v3/departments/{department_id}/children
     */
    String LARK_CHILDREN_DEPARTMENT_URL = "https://open.feishu.cn/open-apis/contact/v3/departments/{0}/children";

    /**
     * 获取部门直属用户
     * GET
     * https://open.feishu.cn/open-apis/contact/v3/users/find_by_department
     */
    String LARK_DEPARTMENT_USERS_URL = "https://open.feishu.cn/open-apis/contact/v3/users/find_by_department";

    /**
     * 发送消息给用户
     * POST
     * https://open.feishu.cn/open-apis/im/v1/messages
     */
    String LARK_SEND_MESSAGE_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    /**
     * 获取企业信息
     * GET
     * https://open.feishu.cn/open-apis/tenant/v2/tenant/query
     */
    String LARK_TENANT_INFO_URL = "https://open.feishu.cn/open-apis/tenant/v2/tenant/query";
}