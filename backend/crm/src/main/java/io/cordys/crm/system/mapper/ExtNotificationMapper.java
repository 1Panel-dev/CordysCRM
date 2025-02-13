package io.cordys.crm.system.mapper;


import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtNotificationMapper {

    List<NotificationDTO> listNotification(@Param("request") NotificationRequest notificationRequest, String organizationId);

    void deleteByTime(@Param("timestamp") long timestamp);

    void deleteByResourceId(@Param("resourceId") String resourceId);

    int updateByReceiver(@Param("request") Notification request);

    int countByReceiver(@Param("request") Notification request);

    List<NotificationDTO> selectByAnyOne(@Param("request") Notification request);

}
