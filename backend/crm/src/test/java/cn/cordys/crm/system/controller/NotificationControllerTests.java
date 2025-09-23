package cn.cordys.crm.system.controller;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.handler.ResultHolder;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.request.NotificationRequest;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NotificationControllerTests extends BaseTest {

    public static final String NOTIFICATION_LIST_PAGE = "/notification/list/all/page";
    public static final String NOTIFICATION_READ = "/notification/read/";
    public static final String NOTIFICATION_READ_ALL = "/notification/read/all";
    public static final String NOTIFICATION_COUNT = "/notification/count";
    public static final String NOTIFICATION_LAST_LIST = "/notification/last/list";
    public static final String ANNOUNCEMENT_LAST_LIST = "/notification/last/announcement/list";


    @Resource
    private ExtNotificationMapper extNotificationMapper;
    @Resource
    private BaseMapper<Notification> notificationMapper;


    void saveNotice(String type) {
        Notification notification = new Notification();
        notification.setId(IDGenerator.nextStr());
        notification.setSubject("功能用例更新通知");
        notification.setOperator("admin");
        notification.setOperation("UPDATE");
        notification.setResourceType(type);
        notification.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        notification.setResourceName("功能用例导入测4");
        notification.setType("SYSTEM_NOTICE");
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setCreateTime(System.currentTimeMillis());
        notification.setReceiver("admin");
        notification.setContent("nihao".getBytes());
        notification.setCreateUser("admin");
        notification.setUpdateUser("admin");
        notification.setUpdateTime(System.currentTimeMillis());
        notification.setResourceId("dd");
        notificationMapper.insert(notification);
    }

    @Test
    @Order(1)
    void getNotificationSuccess() throws Exception {
        saveNotice("CUSTOMER");
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setPageSize(10);
        notificationRequest.setCurrent(1);
        notificationRequest.setReceiver("admin");
        notificationRequest.setType("SYSTEM_NOTICE");
        notificationRequest.setResourceType("CASE");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(NOTIFICATION_LIST_PAGE, notificationRequest);
        Pager<List<Notification>> tableData = JSON.parseObject(JSON.toJSONString(
                        JSON.parseObject(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(tableData.getCurrent(), notificationRequest.getCurrent());
        Assertions.assertTrue(tableData.getList().isEmpty());
    }

    @Test
    @Order(2)
    void setNotificationReadSuccess() throws Exception {
        Notification notification = new Notification();
        notification.setStatus(NotificationConstants.Status.UNREAD.name());
        notification.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        List<NotificationDTO> notifications = extNotificationMapper.selectByAnyOne(notification);
        this.requestGetWithOkAndReturn(NOTIFICATION_READ + notifications.getFirst().getId());
        notification.setStatus(NotificationConstants.Status.READ.name());
        extNotificationMapper.updateByReceiver(notification);
        List<NotificationDTO> readNotifications = extNotificationMapper.selectByAnyOne(notification);
        Assertions.assertFalse(readNotifications.isEmpty());

    }

    @Test
    @Order(3)
    void setNotificationReadAll() throws Exception {
        saveNotice("CUSTOMER");
        this.requestGetWithOk(NOTIFICATION_READ_ALL);
        Notification notification = new Notification();
        notification.setStatus(NotificationConstants.Status.READ.name());
        notification.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        List<NotificationDTO> notifications = extNotificationMapper.selectByAnyOne(notification);
        Assertions.assertFalse(notifications.isEmpty());

    }

    @Test
    @Order(4)
    void getNotificationCount() throws Exception {
        saveNotice("CUSTOMER");
        saveNotice("CLUE");
        saveNotice("BUSINESS");
        saveNotice("ANNOUNCEMENT");
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setPageSize(10);
        notificationRequest.setCurrent(1);
        notificationRequest.setReceiver("admin");
        notificationRequest.setType("SYSTEM_NOTICE");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(NOTIFICATION_COUNT, notificationRequest);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        List<OptionDTO> optionDTOS = JSON.parseArray(JSON.toJSONString(resultHolder.getData()), OptionDTO.class);
        Assertions.assertFalse(optionDTOS.isEmpty());
    }

    @Test
    @Order(5)
    void getNotificationLast() throws Exception {
        saveNotice("CUSTOMER");
        saveNotice("CLUE");
        saveNotice("BUSINESS");
        saveNotice("ANNOUNCEMENT");
        MvcResult mvcResult = this.requestGetWithOkAndReturn(NOTIFICATION_LAST_LIST);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        List<NotificationDTO> optionDTOS = JSON.parseArray(JSON.toJSONString(resultHolder.getData()), NotificationDTO.class);
        Assertions.assertFalse(optionDTOS.isEmpty());
    }

    @Test
    @Order(6)
    void getNotificationAnnLast() throws Exception {
        saveNotice("ANNOUNCEMENT_NOTICE");
        saveNotice("ANNOUNCEMENT_NOTICE");
        MvcResult mvcResult = this.requestGetWithOkAndReturn(ANNOUNCEMENT_LAST_LIST);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        List<NotificationDTO> optionDTOS = JSON.parseArray(JSON.toJSONString(resultHolder.getData()), NotificationDTO.class);
        Assertions.assertFalse(optionDTOS.isEmpty());
    }


}
