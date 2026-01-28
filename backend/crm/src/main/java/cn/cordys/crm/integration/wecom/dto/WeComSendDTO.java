package cn.cordys.crm.integration.wecom.dto;

import lombok.Data;

@Data
public class WeComSendDTO {

  /** 指定接收消息的成员，多个接收者用'｜'分隔，最多支持1000个,特殊情况：指定为"@all"，则向该企业应用的全部成员发送 */
  private String touser;

  /** 指定接收消息的部门，部门ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为"@all"时忽略本参数 */
  private String toparty;

  /** 指定接收消息的标签，标签ID列表，多个接收者用‘|’分隔，最多支持100个。 当touser为"@all"时忽略本参数 */
  private String totag;

  /** 消息类型 */
  private String msgtype;

  /** 应用id */
  private Integer agentid;

  /** 接收者的UserID列表 */
  private Text text;

  /** 表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0 */
  private boolean safe;

  /** 表示是否开启id转译，0表示不转译，1表示转译，默认为0 */
  private boolean enable_id_trans;

  /** 表示是否开启重复消息检查，0表示不检查，1表示检查，默认为0 */
  private boolean enable_duplicate_check;

  /** 表示是否重复消息检查的时间间隔，默认1800s，最大不超过4小时 */
  private Integer duplicate_check_interval;
}
