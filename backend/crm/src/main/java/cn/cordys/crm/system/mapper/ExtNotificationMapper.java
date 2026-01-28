package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.request.NotificationRequest;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtNotificationMapper {

  List<NotificationDTO> listNotification(
      @Param("request") NotificationRequest notificationRequest,
      @Param("organizationId") String organizationId);

  void deleteByTime(@Param("timestamp") long timestamp);

  void deleteByResourceId(@Param("resourceId") String resourceId);

  int updateByReceiver(@Param("request") Notification request);

  int countByReceiver(@Param("request") Notification request);

  List<NotificationDTO> selectByAnyOne(@Param("request") Notification request);

  List<NotificationDTO> selectLastList(
      @Param("userId") String userId,
      @Param("organizationId") String organizationId,
      @Param("modules") List<String> modules);

  List<NotificationDTO> selectLastAnnouncementList(
      @Param("userId") String userId, @Param("organizationId") String organizationId);

  void deleteByReceivers(
      @Param("receivers") List<String> receivers, @Param("organizationId") String organizationId);
}
