package cn.cordys.crm.system.notice.dto;

import cn.cordys.crm.system.dto.response.NotificationDTO;
import java.util.List;
import lombok.Data;

@Data
public class SseMessageDTO {

  private boolean read;

  private List<NotificationDTO> notificationDTOList;

  private List<NotificationDTO> announcementDTOList;
}
