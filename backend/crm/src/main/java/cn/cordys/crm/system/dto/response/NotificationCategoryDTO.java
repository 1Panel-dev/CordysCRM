package cn.cordys.crm.system.dto.response;

import lombok.Data;

@Data
public class NotificationCategoryDTO {

    private String key;

    private String formName;

    private int unreadCount;

    private int totalCount;
}
