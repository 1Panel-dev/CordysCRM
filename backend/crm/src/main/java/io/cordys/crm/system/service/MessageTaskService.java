package io.cordys.crm.system.service;


import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.dto.log.MessageTaskLogDTO;
import io.cordys.crm.system.dto.request.MessageTaskBatchRequest;
import io.cordys.crm.system.dto.request.MessageTaskRequest;
import io.cordys.crm.system.dto.response.MessageTaskDTO;
import io.cordys.crm.system.dto.response.MessageTaskDetailDTO;
import io.cordys.crm.system.mapper.ExtMessageTaskMapper;
import io.cordys.crm.system.utils.MessageTemplateUtils;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
public class MessageTaskService {

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;

    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;

    @Resource
    private LogService logService;

    public MessageTask saveMessageTask(MessageTaskRequest messageTaskRequest, String userId, String organizationId) {
        //检查设置的通知是否存在，如果存在则更新
        MessageTask messageTask = new MessageTask();
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();
        MessageTask messageByModuleAndEvent = extMessageTaskMapper.getMessageByModuleAndEvent(messageTaskRequest.getModule(), messageTaskRequest.getEvent(), organizationId);
        if (messageByModuleAndEvent != null) {
            return updateMessageTasks(messageTaskRequest, userId, messageByModuleAndEvent, eventMap);
        } else {
            //不存在则新增
            messageTask.setId(IDGenerator.nextStr());
            messageTask.setOrganizationId(organizationId);
            messageTask.setTaskType(messageTaskRequest.getModule());
            messageTask.setEvent(messageTaskRequest.getEvent());
            messageTask.setCreateUser(userId);
            messageTask.setCreateTime(System.currentTimeMillis());
            messageTask.setUpdateUser(userId);
            messageTask.setUpdateTime(System.currentTimeMillis());
            messageTask.setEmailEnable(messageTaskRequest.isEmailEnable());
            messageTask.setSysEnable(messageTaskRequest.isSysEnable());
            messageTask.setWeComEnable(messageTaskRequest.isWeComEnable());
            String template = MessageTemplateUtils.getTemplate(messageTaskRequest.getEvent());
            messageTask.setTemplate(template.getBytes(StandardCharsets.UTF_8));
            messageTaskMapper.insert(messageTask);
            // 添加日志上下文
            MessageTaskLogDTO newDTO = buildLogDTO(messageTask, messageTaskRequest.isEmailEnable(), messageTaskRequest.isSysEnable(), messageTaskRequest.isWeComEnable(), eventMap);
            LogDTO logDTO = new LogDTO(organizationId, messageTask.getId(), userId, LogType.UPDATE, LogModule.SYSTEM_MESSAGE_MESSAGE, eventMap.get(messageTask.getEvent()));
            logDTO.setOriginalValue(null);
            logDTO.setModifiedValue(newDTO);
            logService.add(logDTO);

        }
        return messageTask;
    }

    /**
     * 检查数据库是否有同类型数据，有则更新
     *
     * @param messageTaskRequest 入参
     * @param userId             当前用户ID
     */
    public MessageTask updateMessageTasks(MessageTaskRequest messageTaskRequest, String userId, MessageTask oldMessageTask, Map<String, String> eventMap) {
        MessageTask messageTask = new MessageTask();
        messageTask.setId(oldMessageTask.getId());
        messageTask.setEmailEnable(messageTaskRequest.isEmailEnable());
        messageTask.setSysEnable(messageTaskRequest.isSysEnable());
        messageTask.setWeComEnable(messageTaskRequest.isWeComEnable());
        messageTask.setUpdateUser(userId);
        messageTask.setUpdateTime(System.currentTimeMillis());
        messageTaskMapper.update(messageTask);
        // 添加日志上下文
        MessageTaskLogDTO oldDTO = buildLogDTO(oldMessageTask, oldMessageTask.getEmailEnable(), oldMessageTask.getSysEnable(), messageTaskRequest.isWeComEnable(), eventMap);
        MessageTaskLogDTO newDTO = buildLogDTO(oldMessageTask, messageTaskRequest.isEmailEnable(), messageTaskRequest.isSysEnable(), messageTaskRequest.isWeComEnable(), eventMap);
        LogDTO logDTO = new LogDTO(oldMessageTask.getOrganizationId(), messageTask.getId(), userId, LogType.UPDATE, LogModule.SYSTEM_MESSAGE_MESSAGE, eventMap.get(messageTaskRequest.getEvent()));
        logDTO.setOriginalValue(oldDTO);
        logDTO.setModifiedValue(newDTO);
        logService.add(logDTO);

        return messageTask;
    }

    @NotNull
    private static MessageTaskLogDTO buildLogDTO(MessageTask oldMessageTask, Boolean emailEnable, Boolean sysEnable, Boolean weComEnable, Map<String, String> eventMap) {
        MessageTaskLogDTO newDTO = new MessageTaskLogDTO();
        newDTO.setEvent(eventMap.get(oldMessageTask.getEvent()));
        if (emailEnable != null) {
            newDTO.setEmailEnable(emailEnable ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        } else {
            newDTO.setEmailEnable(oldMessageTask.getEmailEnable() ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        }
        if (sysEnable != null) {
            newDTO.setSysEnable(sysEnable ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        } else {
            newDTO.setSysEnable(oldMessageTask.getSysEnable() ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        }
        if (weComEnable != null) {
            newDTO.setWeComEnable(weComEnable ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        } else {
            newDTO.setWeComEnable(oldMessageTask.getWeComEnable() ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
        }
        return newDTO;
    }

    /**
     * 根据项目id 获取当前项目的消息设置
     *
     * @param organizationId 组织ID
     * @return List<MessageTaskDTO>
     */
    public List<MessageTaskDTO> getMessageList(String organizationId) {
        //获取返回数据结构
        StringBuilder jsonStr = new StringBuilder();
        try (InputStream inputStream = getClass().getResourceAsStream("/task/message_task.json")) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonStr.append(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<MessageTaskDTO> messageTaskDTOList = JSON.parseArray(jsonStr.toString(), MessageTaskDTO.class);
        //查询数据
        List<MessageTask> messageTasks = extMessageTaskMapper.getMessageTaskList(organizationId);
        if (CollectionUtils.isEmpty(messageTasks)) {
            return messageTaskDTOList;
        }
        //获取默认数据
        Map<String, String> moduleMap = MessageTemplateUtils.getModuleMap();
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();
        Map<String, MessageTask> messageMap = messageTasks.stream().collect(Collectors.toMap(MessageTask::getEvent, t -> t));
        for (MessageTaskDTO messageTaskDTO : messageTaskDTOList) {
            messageTaskDTO.setModuleName(moduleMap.get(messageTaskDTO.getModule()));
            for (MessageTaskDetailDTO messageTaskDetailDTO : messageTaskDTO.getMessageTaskDetailDTOList()) {
                messageTaskDetailDTO.setEventName(eventMap.get(messageTaskDetailDTO.getEvent()));
                MessageTask messageTask = messageMap.get(messageTaskDetailDTO.event);
                if (messageTask != null) {
                    messageTaskDetailDTO.setEmailEnable(messageTask.getEmailEnable());
                    messageTaskDetailDTO.setSysEnable(messageTask.getSysEnable());
                    messageTaskDetailDTO.setWeComEnable(messageTask.getWeComEnable());
                }
            }

        }
        return messageTaskDTOList;
    }


    public void batchSaveMessageTask(MessageTaskBatchRequest messageTaskBatchRequest, String organizationId, String userId) {
        List<MessageTask> oldMessageList = extMessageTaskMapper.getMessageTaskList(organizationId);
        extMessageTaskMapper.updateMessageTask(messageTaskBatchRequest, organizationId);
        // 添加日志上下文
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();
        List<LogDTO>logDTOList=new ArrayList<>();
        for (MessageTask messageTask : oldMessageList) {
            MessageTaskLogDTO oldDTO = buildLogDTO(messageTask, messageTask.getEmailEnable(), messageTask.getSysEnable(), messageTaskBatchRequest.getWeComEnable(), eventMap);
            MessageTaskLogDTO newDTO = buildLogDTO(messageTask, messageTaskBatchRequest.getEmailEnable(), messageTaskBatchRequest.getSysEnable(), messageTaskBatchRequest.getWeComEnable(), eventMap);
            LogDTO logDTO = new LogDTO(organizationId, messageTask.getId(), userId, LogType.UPDATE, LogModule.SYSTEM_MESSAGE_MESSAGE, eventMap.get(messageTask.getEvent()));
            logDTO.setOriginalValue(oldDTO);
            logDTO.setModifiedValue(newDTO);
            logDTOList.add(logDTO);
        }
        logService.batchAdd(logDTOList);
    }
}
