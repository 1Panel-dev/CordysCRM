package io.cordys.crm.system.controller;

import io.cordys.common.response.handler.ResultHolder;
import io.cordys.common.util.JSON;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.dto.request.MessageTaskRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageTaskControllerTest extends BaseTest {

    public static final String SAVE_MESSAGE_TASK = "/message/task/save";

    public static final String GET_MESSAGE_TASK = "/message/task/get";

    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;

    @Test
    @Order(1)
    void testSaveMessageTask() throws Exception {
        MessageTaskRequest messageTaskRequest = new MessageTaskRequest();
        messageTaskRequest.setReceiveType("USER");
        messageTaskRequest.setModule("SYSTEM");
        messageTaskRequest.setEvent("NOTICE");
        messageTaskRequest.setReceiverIds(Arrays.asList("1", "2", "3"));
        messageTaskRequest.setUseDefaultTemplate(true);
        messageTaskRequest.setTemplate("test");
        messageTaskRequest.setEnable(true);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(SAVE_MESSAGE_TASK, messageTaskRequest);
        String updateReturnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(updateReturnData, ResultHolder.class);
        MessageTask messageTask = JSON.parseObject(JSON.toJSONString(resultHolder.getData()), MessageTask.class);
        Assertions.assertNotNull(messageTask);
        MessageTask messageTask1 = messageTaskMapper.selectOne(messageTask);
        Assertions.assertNotNull(messageTask1);
        messageTaskRequest = new MessageTaskRequest();
        messageTaskRequest.setReceiveType("USER");
        messageTaskRequest.setModule("SYSTEM");
        messageTaskRequest.setEvent("NOTICE");
        messageTaskRequest.setReceiverIds(Arrays.asList("1", "2", "3"));
        messageTaskRequest.setUseDefaultTemplate(true);
        messageTaskRequest.setTemplate("test");
        messageTaskRequest.setEnable(true);
        messageTaskRequest.setTestId(messageTask.getId());
        MvcResult mvcResult1 = this.requestPostWithOkAndReturn(SAVE_MESSAGE_TASK, messageTaskRequest);
        String updateReturnData1 = mvcResult1.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder1 = JSON.parseObject(updateReturnData1, ResultHolder.class);
        MessageTask messageTask2 = JSON.parseObject(JSON.toJSONString(resultHolder1.getData()), MessageTask.class);
        Assertions.assertNotNull(messageTask2);
        // TODO: Implement test
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
}
