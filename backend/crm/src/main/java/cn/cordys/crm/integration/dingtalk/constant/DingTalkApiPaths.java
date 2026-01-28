package cn.cordys.crm.integration.dingtalk.constant;

public interface DingTalkApiPaths {

  String DING_TALK_GET_TOKEN = "https://api.dingtalk.com/v1.0/oauth2/accessToken";

  String DING_USER_INFO = "https://api.dingtalk.com/v1.0/contact/users/me";

  String DING_USER_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

  /**
   * 根据unionid获取userid POST
   * https://oapi.dingtalk.com/topapi/user/getbyunionid?access_token=ACCESS_TOKEN
   */
  String DING_USERID_BY_UNIONID =
      "https://oapi.dingtalk.com/topapi/user/getbyunionid?access_token={0}";

  /**
   * 调用本接口，获取企业部门下的所有直属子部门列表。调用本接口获取根部门的子部门ID列表，获取的结果是[部门1Id,部门2Id]，部门1和部门2下的子部门是获取不到的。 POST body {
   * "dept_id":1 部门ID，根部门ID为1。 }
   */
  String DING_DEPARTMENT_IDS =
      "https://oapi.dingtalk.com/topapi/v2/department/listsubid?access_token={0}";

  /** POST body { "language":"zh_CN", "dept_id":1 部门ID，根部门ID为1。 } */
  String DING_DEPARTMENT_DETAIL =
      "https://oapi.dingtalk.com/topapi/v2/department/get?access_token={0}";

  /**
   * 本接口只支持获取指定部门下的员工详情信息，子部门员工信息获取不到。 POST body { "dept_id":1 部门ID，如果是根部门，该参数传1 "cursor":"",
   * 分页游标，第一次调用时，cursor填空字符串。 "size":100 分页大小，最大100。 }
   */
  String DING_DEPARTMENT_USER_DETAIL_LIST =
      "https://oapi.dingtalk.com/topapi/v2/user/list?access_token={0}";

  /**
   * 发送工作通知消息
   * 接口文档: <a href="https://open.dingtalk.com/document/orgapp/asynchronous-sending-of-enterprise-session-messages">https://developers.dingtalk.com/document/app/send-work-notification</a>
   * <pre>
   *     https请求方式: POST https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token=ACCESS_TOKEN
   *     access_token	是	调用接口凭证，由钉钉开放平台或企业自建应用后台生成
   *     {
   *         "msg":{
   *                 "text":{
   *                         "content":"123"
   *                 },
   *                 "msgtype":"text"
   *         },
   *         "to_all_user":"false",
   *         "agent_id":"123",
   *         "userid_list":"123,456"
   * }
   */
  String DING_NOTICE_URL =
      "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token={0}";
}
