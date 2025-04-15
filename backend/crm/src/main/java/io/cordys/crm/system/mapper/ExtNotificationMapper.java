package io.cordys.crm.system.mapper;


import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtNotificationMapper {

    List<NotificationDTO> listNotification(@Param("request") NotificationRequest notificationRequest, @Param("organizationId") String organizationId);

    void deleteByTime(@Param("timestamp") long timestamp);

    void deleteByResourceId(@Param("resourceId") String resourceId);

    int updateByReceiver(@Param("request") Notification request);

    int countByReceiver(@Param("request") Notification request);

    List<NotificationDTO> selectByAnyOne(@Param("request") Notification request);

    List<NotificationDTO> selectLastList(@Param("userId") String userId, @Param("organizationId") String organizationId, @Param("modules") List<String> modules);

    List<NotificationDTO> selectLastAnnouncementList(@Param("userId") String userId, @Param("organizationId") String organizationId);
}
