package cn.cordys.crm.integration.dingtalk.dto;

import lombok.Data;

@Data
public class DingTalkTextDTO {
  private String content;

  public DingTalkTextDTO(String content) {
    this.content = content;
  }
}
