package io.cordys.crm.system.notice.dto;

import io.cordys.crm.system.dto.response.NotificationDTO;
import lombok.Data;

import java.util.List;

@Data
public class SseMessageDTO {

    private boolean read;

    private List<NotificationDTO> notificationDTOList;

    private List<NotificationDTO>announcementDTOList;
}
