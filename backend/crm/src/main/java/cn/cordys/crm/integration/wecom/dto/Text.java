package cn.cordys.crm.integration.wecom.dto;

import lombok.Data;

@Data
public class Text {

  /** 消息内容，最长不超过2048个字节，超过将截断（支持id转译） */
  private String content;

  public Text(String content) {
    this.content = content;
  }

  public Text() {
    // 默认构造函数
  }
}
