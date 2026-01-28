package cn.cordys.crm.integration.dingtalk.dto;

import lombok.Data;

@Data
public class DingTalkMsgDTO {
  private DingTalkTextDTO text;
  private String msgtype;
}
