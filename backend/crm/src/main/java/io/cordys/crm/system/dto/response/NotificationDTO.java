package io.cordys.crm.system.dto.response;

import io.cordys.crm.system.domain.Notification;
import lombok.Data;

@Data
public class NotificationDTO extends Notification {
    private String contentText;
}
