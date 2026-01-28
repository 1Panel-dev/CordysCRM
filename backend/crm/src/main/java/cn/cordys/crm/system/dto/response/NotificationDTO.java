package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.domain.Notification;
import lombok.Data;

@Data
public class NotificationDTO extends Notification {
  private String contentText;
}
