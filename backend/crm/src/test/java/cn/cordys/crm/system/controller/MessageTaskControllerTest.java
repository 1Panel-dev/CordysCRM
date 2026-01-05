package cn.cordys.crm.system.controller;

import cn.cordys.common.response.handler.ResultHolder;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.MessageTaskConfigWithNameDTO;
import cn.cordys.crm.system.dto.TimeDTO;
import cn.cordys.crm.system.dto.request.MessageTaskBatchRequest;
import cn.cordys.crm.system.dto.request.MessageTaskConfigRequest;
import cn.cordys.crm.system.dto.request.MessageTaskRequest;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageTaskControllerTest extends BaseTest {

    public static final String SAVE_MESSAGE_TASK = "/message/task/save";
    public static final String BATCH_SAVE_MESSAGE_TASK = "/message/task/batch/save";

    public static final String GET_MESSAGE_TASK = "/message/task/get";

    public static final String GET_MESSAGE_TASK_CONFIG = "/message/task/config/query";


    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;

    @Test
    @Order(1)
    void testSaveMessageTask() throws Exception {
        MessageTaskRequest messageTaskRequest = new MessageTaskRequest();
        messageTaskRequest.setModule(NotificationConstants.Module.CUSTOMER);
        messageTaskRequest.setEvent(NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS);
        messageTaskRequest.setEmailEnable(true);
        messageTaskRequest.setSysEnable(true);
        MessageTaskConfigDTO messageTaskConfigDTO = getMessageTaskConfigDTO();
        messageTaskRequest.setConfig(messageTaskConfigDTO);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(SAVE_MESSAGE_TASK, messageTaskRequest);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        MessageTask messageTask = JSON.parseObject(JSON.toJSONString(resultHolder.getData()), MessageTask.class);
        Assertions.assertNotNull(messageTask);
        MessageTask messageTask1 = extMessageTaskMapper.getMessageByModuleAndEvent(messageTaskRequest.getModule(), messageTaskRequest.getEvent(), DEFAULT_ORGANIZATION_ID);
        Assertions.assertNotNull(messageTask1);
        messageTaskRequest = new MessageTaskRequest();
        messageTaskRequest.setModule(NotificationConstants.Module.CUSTOMER);
        messageTaskRequest.setEvent(NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS);
        messageTaskRequest.setEmailEnable(false);
        messageTaskRequest.setSysEnable(true);
        MvcResult mvcResult1 = this.requestPostWithOkAndReturn(SAVE_MESSAGE_TASK, messageTaskRequest);
        String updateReturnData1 = mvcResult1.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder1 = JSON.parseObject(updateReturnData1, ResultHolder.class);
        MessageTask messageTask2 = JSON.parseObject(JSON.toJSONString(resultHolder1.getData()), MessageTask.class);
        Assertions.assertNotNull(messageTask2);
        Map<String, Object> resource = new HashMap<>();
        resource.put("relatedUsers", "aa");
        resource.put("name", "cc");
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER, NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS, resource, "admin", DEFAULT_ORGANIZATION_ID, List.of("aa"), true);

        messageTaskRequest.setSysEnable(false);
        messageTaskRequest.setWeComEnable(true);
        mvcResult1 = this.requestPostWithOkAndReturn(SAVE_MESSAGE_TASK, messageTaskRequest);
        updateReturnData1 = mvcResult1.getResponse().getContentAsString(StandardCharsets.UTF_8);
        resultHolder1 = JSON.parseObject(updateReturnData1, ResultHolder.class);
        messageTask2 = JSON.parseObject(JSON.toJSONString(resultHolder1.getData()), MessageTask.class);
        Assertions.assertNotNull(messageTask2);
        resource = new HashMap<>();
        resource.put("relatedUsers", "aa");
        resource.put("name", "cc");
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER, NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS, resource, "admin", DEFAULT_ORGANIZATION_ID, List.of("aa"), true);


    }

    private @NotNull MessageTaskConfigDTO getMessageTaskConfigDTO() {
        MessageTaskConfigDTO messageTaskConfigDTO = new MessageTaskConfigDTO();
        TimeDTO timeDTO = new TimeDTO();
        timeDTO.setTimeValue(3);
        timeDTO.setTimeUnit("DAY");
        List<TimeDTO> timeDTOList = List.of(timeDTO);
        messageTaskConfigDTO.setTimeList(timeDTOList);
        messageTaskConfigDTO.setOwnerEnable(true);
        messageTaskConfigDTO.setOwnerLevel(0);
        messageTaskConfigDTO.setUserIds(List.of("OWNER"));
        messageTaskConfigDTO.setRoleEnable(true);
        messageTaskConfigDTO.setRoleIds(List.of("org_admin"));
        return messageTaskConfigDTO;
    }

    @Test
    @Order(2)
    void testGetMessageTask() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(GET_MESSAGE_TASK);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        List<MessageTask> messageTasks = JSON.parseArray(JSON.toJSONString(resultHolder.getData()), MessageTask.class);
        Assertions.assertFalse(messageTasks.isEmpty());
    }

    @Test
    @Order(3)
    void testBatchSaveMessageTask() throws Exception {
        MessageTaskBatchRequest messageTaskRequest = new MessageTaskBatchRequest();
        messageTaskRequest.setEmailEnable(true);
        messageTaskRequest.setSysEnable(true);
        this.requestPostWithOk(BATCH_SAVE_MESSAGE_TASK, messageTaskRequest);
        MessageTask messageTask1 = extMessageTaskMapper.getMessageByModuleAndEvent(NotificationConstants.Module.CUSTOMER, NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS, DEFAULT_ORGANIZATION_ID);
        Assertions.assertTrue(messageTask1.getEmailEnable());
    }

    @Test
    @Order(4)
    void testGetMessageTaskConfig() throws Exception {
        MessageTaskConfigRequest messageTaskConfigRequest = new MessageTaskConfigRequest();
        messageTaskConfigRequest.setModule(NotificationConstants.Module.CUSTOMER);
        messageTaskConfigRequest.setEvent(NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(GET_MESSAGE_TASK_CONFIG, messageTaskConfigRequest);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        MessageTaskConfigWithNameDTO messageTaskConfigDTO = JSON.parseObject(JSON.toJSONString(resultHolder.getData()), MessageTaskConfigWithNameDTO.class);
        Assertions.assertNotNull(messageTaskConfigDTO);
        System.out.println(JSON.toJSONString(messageTaskConfigDTO));
    }
}
