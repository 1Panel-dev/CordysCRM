package io.cordys.crm.integration.wecom.constant;

public interface WeComApiPaths {

    /**
     * 接口基础路径
     */
    String QYAPI_BASE_PATH = "https://qyapi.weixin.qq.com/cgi-bin";


    /**
     * 获取Access token
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/91039">https://developer.work.weixin.qq.com/document/path/91039</a>
     * <pre>
     * https请求方式: GET https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=ID&corpsecret=SECRET
     * corpid	是	企业ID，获取方式参考：<a href="https://developer.work.weixin.qq.com/document/path/90665#corpid">术语说明-corpid</a>
     * corpsecret	是	应用的凭证密钥，注意应用需要是启用状态，获取方式参考：<a href="https://developer.work.weixin.qq.com/document/path/90665#secret">术语说明-secret</a>
     * </pre>
     */
    String GET_TOKEN = QYAPI_BASE_PATH + "/gettoken?corpid={0}&corpsecret={1}";

    /**
     * 获取指定的应用详情
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/96448">https://developer.work.weixin.qq.com/document/path/91039</a>
     * <pre>
     * https请求方式: GET https://qyapi.weixin.qq.com/cgi-bin/agent/get?access_token=ACCESS_TOKEN&agentid=AGENTID
     * access_token		是	调用接口凭证，GET_TOKEN
     * agentid	是	应用id
     * </pre>
     */
    String GET_AGENT = QYAPI_BASE_PATH + "/agent/get?access_token={0}&agentid={1}";

    /**
     * <p>
     * 获取部门列表<br>
     * 【重要】从2022年8月15日10点开始，“企业管理后台 - 管理工具 - 通讯录同步”的新增IP将不能再调用此接口，企业可通过「获取部门ID列表」接口获取部门ID列表。查看调整详情。<br>
     * 权限说明：<br>
     * 只能拉取token对应的应用的权限范围内的部门列表<br>
     * </p>
     * <p>
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/90208">https://developer.work.weixin.qq.com/document/path/90208</a> <br>
     * http请求方式: GET https://qyapi.weixin.qq.com/cgi-bin/department/list?access_token=ACCESS_TOKEN&id=ID
     * </p>
     */
    String DEPARTMENT_LIST = QYAPI_BASE_PATH + "/department/list?access_token={0}&id={1}";


    /**
     * <p>
     * 获取部门成员详情 <br>
     * 应用只能获取可见范围内的成员信息，且每种应用获取的字段有所不同，在返回结果说明中会逐个说明。企业通讯录安全特别重要，企业微信持续升级加固通讯录接口的安全机制，以下是关键的变更点：<br>
     * - 从2022年6月20号20点开始，除通讯录同步以外的基础应用（如客户联系、微信客服、会话存档、日程等），以及新创建的自建应用与代开发应用，调用该接口时，不再返回以下字段：头像、性别、手机、邮箱、企业邮箱、员工个人二维码、地址，应用需要通过oauth2手工授权的方式获取管理员与员工本人授权的字段。<br>
     * - 【重要】从2022年8月15日10点开始，“企业管理后台 - 管理工具 - 通讯录同步”的新增IP将不能再调用此接口，企业可通过「获取成员ID列表」和「获取部门ID列表」接口获取userid和部门ID列表。查看调整详情。<br>
     * </p>
     * <p>
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/90201">https://developer.work.weixin.qq.com/document/path/90201</a> <br>
     * http请求方式: GET https://qyapi.weixin.qq.com/cgi-bin/user/list?access_token=ACCESS_TOKEN&department_id=DEPARTMENT_ID
     * </p>
     */
    String USER_LIST = QYAPI_BASE_PATH + "/user/list?access_token={0}&department_id={1}";

    /**
     * <p>
     * 获取访问用户身份 <br>
     * 该接口用于根据code获取成员信息，适用于自建应用与代开发应用 <br>
     * 权限说明： <br>
     * 跳转的域名须完全匹配access_token对应应用的可信域名，否则会返回50001错误。
     * </p>
     * <p>
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/91023">https://developer.work.weixin.qq.com/document/path/91023</a>
     * http请求地址: GET https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=ACCESS_TOKEN&code=CODE
     * </p>
     */
    String USER_ID_TICKET = QYAPI_BASE_PATH+"/auth/getuserinfo?access_token={0}&code={1}";

    /**
     * <p>
     * 读取成员 <br>
     * 自建应用与代开发应用可通过该接口获取成员授权的敏感字段
     * </p>
     * <p>
     * 接口文档： <a href="https://developer.work.weixin.qq.com/document/path/95833">...</a>
     * 请求方式：POST（HTTPS）
     * http请求地址: POST  https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail?access_token=ACCESS_TOKEN
     * 请求包体：
     *
     * {
     *    "user_ticket": "USER_TICKET"
     * }
     * </p>
     */
    String USER_DETAIL = QYAPI_BASE_PATH+"/auth/getuserdetail?access_token={0}";

    /**
     * <p>
     * 读取成员 <br>
     * 应用只能获取可见范围内的成员信息，且每种应用获取的字段有所不同，在返回结果说明中会逐个说明。企业通讯录安全特别重要，企业微信将持续升级加固通讯录接口的安全机制，以下是关键的变更点：
     *
     * 从2022年6月20号20点开始，除通讯录同步以外的基础应用（如客户联系、微信客服、会话存档、日程等），以及新创建的自建应用与代开发应用，调用该接口时，不再返回以下字段：头像、性别、手机、邮箱、企业邮箱、员工个人二维码、地址，应用需要通过oauth2手工授权的方式获取管理员与员工本人授权的字段。
     * </p>
     * <p>
     * 接口文档： <a href="https://developer.work.weixin.qq.com/document/path/96255">...</a>
     * 请求方式：POST（HTTPS）
     * http请求地址: GET  https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&userid=USERID
     *
     * </p>
     */
    String USER_INFO =  QYAPI_BASE_PATH+"/user/get?access_token={0}&userid={1}";

    /**
     * <p>
     *     发送应用消息 <br>
     *     应用支持推送文本、图片、视频、文件、图文等类型。<br>
     * </p>
     * 接口文档: <a href="https://developer.work.weixin.qq.com/document/path/90236#%E6%96%87%E6%9C%AC%E6%B6%88%E6%81%AF">https://developer.work.weixin.qq.com/document/path/91039</a>
     * 请求方式: POST（HTTPS）
     * http请求地址: POST https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=ACCESS_TOKEN
     *
     */
    String SEND_INFO = QYAPI_BASE_PATH+"/message/send?access_token={0}";
}
