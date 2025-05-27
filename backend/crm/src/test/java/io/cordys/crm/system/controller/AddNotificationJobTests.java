package io.cordys.crm.system.controller;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.domain.Announcement;
import io.cordys.crm.system.domain.Notification;
import io.cordys.crm.system.dto.AnnouncementReceiveTypeDTO;
import io.cordys.crm.system.dto.response.NotificationDTO;
import io.cordys.crm.system.job.NotifyOnJob;
import io.cordys.crm.system.mapper.ExtNotificationMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddNotificationJobTests {

    @Resource
    private NotifyOnJob notifyOnJob;

    @Resource
    private BaseMapper<Announcement> announcementBaseMapper;

    @Resource
    private ExtNotificationMapper extNotificationMapper;

    void saveNotice() {
        AnnouncementReceiveTypeDTO announcementReceiveTypeDTO = new AnnouncementReceiveTypeDTO();
        announcementReceiveTypeDTO.setUserIds(List.of("aaa","bbb"));
        Announcement announcement = new Announcement();
        announcement.setId("SDDFDJJND");
        announcement.setSubject("Test");
        announcement.setContent("公告".getBytes());
        announcement.setStartTime(1727165733128L);
        announcement.setCreateTime(1727165816848L);
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setEndTime(System.currentTimeMillis()+5000);
        announcement.setReceiver(JSON.toJSONString(List.of("aaa","bbb")).getBytes());
        announcement.setOrganizationId("100001");
        announcement.setNotice(false);
        announcement.setReceiveType(JSON.toJSONBytes(announcementReceiveTypeDTO));
        announcement.setCreateUser("admin");
        announcement.setUpdateUser("admin");
        announcementBaseMapper.insert(announcement);
    }

    @Test
    @Order(0)
    public void cleanupNotificationSuccess(){
        saveNotice();
        List<Announcement> createTimeAsc = announcementBaseMapper.selectAll("create_time asc");
        Assertions.assertTrue(CollectionUtils.isNotEmpty(createTimeAsc));
        notifyOnJob.addNotification();
        Notification notification = new Notification();
        notification.setOrganizationId("100001");
        notification.setResourceId("SDDFDJJND");
        List<NotificationDTO> notifications = extNotificationMapper.selectByAnyOne(notification);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(notifications));
    }
}
