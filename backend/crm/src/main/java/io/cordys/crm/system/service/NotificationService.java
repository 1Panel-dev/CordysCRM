package io.cordys.crm.system.service;



import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.request.NotificationRequest;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.dto.response.OptionDTO;
import io.cordys.crm.system.mapper.ExtNotificationMapper;
import io.cordys.mybatis.BaseMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class NotificationService {

    @Resource
    private BaseMapper<Notification> notificationMapper;
    @Resource
    private ExtNotificationMapper extNotificationMapper;

    public List<NotificationDTO> listNotification(NotificationRequest notificationRequest, String userId) {
        buildParam(notificationRequest, userId);
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest);
        notifications.forEach(notification -> {
            notification.setContentText(new String(notification.getContent()));
        });
        return notifications;
    }

    public int read(String id, String userId) {
        Notification record = new Notification();
        record.setId(id);
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setReceiver(userId);
        return notificationMapper.update(record);
    }

    public int readAll(String organizationId, String userId) {
        Notification record = new Notification();
        record.setStatus(NotificationConstants.Status.READ.name());
        record.setOrganizationId(organizationId);
        record.setReceiver(userId);
        return extNotificationMapper.updateByReceiver(record);
    }

    public List<OptionDTO> countNotification(NotificationRequest notificationRequest, String userId) {
        List<OptionDTO> optionDTOS = new ArrayList<>();
        buildParam(notificationRequest, userId);
        notificationRequest.setResourceType(StringUtils.EMPTY);
        notificationRequest.setStatus(NotificationConstants.Status.UNREAD.name());
        List<NotificationDTO> notifications = extNotificationMapper.listNotification(notificationRequest);
        OptionDTO totalOptionDTO = new OptionDTO();
        totalOptionDTO.setId("total");
        totalOptionDTO.setName(String.valueOf(notifications.size()));
        optionDTOS.add(totalOptionDTO);
        return optionDTOS;
    }

    private static void buildParam(NotificationRequest notificationRequest, String userId) {
        if (StringUtils.isNotBlank(notificationRequest.getSubject())) {
            notificationRequest.setSubject("%" + notificationRequest.getSubject() + "%");
        }
        if (StringUtils.isNotBlank(notificationRequest.getResourceType())) {
            notificationRequest.setResourceType("%" + notificationRequest.getResourceType() + "%");
        }
        if (StringUtils.isBlank(notificationRequest.getReceiver())) {
            notificationRequest.setReceiver(userId);
        }
    }


    public Integer getUnRead(String organizationId, String userId) {
        if (StringUtils.isBlank(organizationId)) {
            return 0;
        }
        Notification record = new Notification();
        record.setOrganizationId(organizationId);
        record.setStatus(NotificationConstants.Status.UNREAD.name());
        record.setReceiver(userId);
        return extNotificationMapper.countByReceiver(record);
    }
}
