package io.cordys.crm.system.service;


import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.MessageTask;
import io.cordys.crm.system.domain.MessageTaskBlob;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.MessageTaskRequest;
import io.cordys.crm.system.dto.response.MessageTaskDTO;
import io.cordys.crm.system.dto.response.MessageTaskDetailDTO;
import io.cordys.crm.system.dto.response.ProjectRobotConfigDTO;
import io.cordys.crm.system.mapper.ExtMessageTaskBlobMapper;
import io.cordys.crm.system.mapper.ExtMessageTaskMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.utils.MessageTemplateUtils;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageTaskService {

@Resource
private ExtMessageTaskMapper extMessageTaskMapper;

@Resource
private ExtMessageTaskBlobMapper extMessageTaskBlobMapper;

@Resource
private BaseMapper<MessageTask> messageTaskMapper;

@Resource
private BaseMapper<MessageTaskBlob> messageTaskBlobMapper;

@Resource
private ExtUserMapper extUserMapper;

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_NOTICE, type = LogType.ADD,  operator = "{#userId}")
    public MessageTask saveMessageTask(MessageTaskRequest messageTaskRequest, String userId, String organizationId) {
        //检查设置的通知是否存在，如果存在则更新
        MessageTask messageTask = new MessageTask();
        List<MessageTask> messageTasks = extMessageTaskMapper.getMessageTaskByReceiveTypeAndTaskType(messageTaskRequest.getReceiveType(), messageTaskRequest.getModule(),organizationId);
        if (CollectionUtils.isNotEmpty(messageTasks)) {
            return updateMessageTasks(messageTaskRequest, userId, messageTasks.getFirst());
        } else {
            //不存在则新增
            if (CollectionUtils.isEmpty(messageTasks)) {
                messageTask.setId(IDGenerator.nextStr());
                messageTask.setOrganizationId(organizationId);
                messageTask.setTaskType(messageTaskRequest.getModule());
                messageTask.setEvent(messageTaskRequest.getEvent());
                messageTask.setReceiveType(messageTaskRequest.getReceiveType());
                messageTask.setCreateUser(userId);
                messageTask.setCreateTime(System.currentTimeMillis());
                messageTask.setUpdateUser(userId);
                messageTask.setUpdateTime(System.currentTimeMillis());
                messageTask.setEnable(messageTaskRequest.getEnable());
                messageTask.setUseDefaultTemplate(messageTaskRequest.getUseDefaultTemplate());
                messageTask.setReceivers(JSON.toJSONString(messageTaskRequest.getReceiverIds()));
                messageTaskMapper.insert(messageTask);
                MessageTaskBlob messageTaskBlob = new MessageTaskBlob();
                messageTaskBlob.setId(messageTask.getId());
                messageTaskBlob.setTemplate(messageTaskRequest.getTemplate().getBytes());
                messageTaskBlobMapper.insert(messageTaskBlob);
                // 添加日志上下文
                OperationLogContext.setContext(LogContextInfo.builder()
                        .originalValue(null)
                        .resourceId(messageTask.getEvent())
                        .resourceName(messageTask.getTaskType())
                        .modifiedValue(messageTask)
                        .build());
            }
        }
        return messageTask;
    }

    /**
     * 检查数据库是否有同类型数据，有则更新
     *
     * @param messageTaskRequest 入参
     * @param userId             当前用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM_NOTICE, type = LogType.UPDATE,operator = "{{#userId}}")
    public MessageTask updateMessageTasks(MessageTaskRequest messageTaskRequest, String userId, MessageTask oldMessageTask) {
        MessageTask messageTask = new MessageTask();
        messageTask.setId(oldMessageTask.getId());
        messageTask.setReceivers(JSON.toJSONString(messageTaskRequest.getReceiverIds()));
        messageTask.setEnable(messageTaskRequest.getEnable());
        messageTask.setUpdateUser(userId);
        messageTask.setUpdateTime(System.currentTimeMillis());
        messageTask.setUseDefaultTemplate(messageTaskRequest.getUseDefaultTemplate());
        messageTaskMapper.update(messageTask);
        MessageTaskBlob messageTaskBlob = new MessageTaskBlob();
        messageTaskBlob.setId(messageTask.getId());
        messageTaskBlob.setTemplate(messageTaskRequest.getTemplate().getBytes());
        messageTaskBlobMapper.update(messageTaskBlob);
        // 添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .resourceId(messageTask.getId())
                .resourceName(messageTask.getTaskType())
                .originalValue(oldMessageTask)
                .modifiedValue(messageTask)
                .build());

        return messageTask;
    }

    /**
     * 根据项目id 获取当前项目的消息设置
     *
     * @param organizationId 组织ID
     * @return List<MessageTaskDTO>
     */
    public List<MessageTaskDTO> getMessageList(String organizationId) throws IOException {
        //获取返回数据结构
        StringBuilder jsonStr = new StringBuilder();
        try {
            InputStream inputStream = getClass().getResourceAsStream("/task/message_task.json");
            assert inputStream != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStr.append(line);
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<MessageTaskDTO> messageTaskDTOList = JSON.parseArray(jsonStr.toString(), MessageTaskDTO.class);
        //查询数据
        List<MessageTask> messageTasks = extMessageTaskMapper.getMessageTaskList(organizationId);
        if (CollectionUtils.isEmpty(messageTasks)) {
            return messageTaskDTOList;
        }
        List<String> messageTaskIds = messageTasks.stream().map(MessageTask::getId).toList();
        List<MessageTaskBlob> messageTaskBlobs = extMessageTaskBlobMapper.getExtMessageTaskBlobByMessageTaskIds(messageTaskIds);
        List<String> userIds = new ArrayList<>();
        messageTasks.forEach(t->{
            userIds.addAll(JSON.parseArray(t.getReceivers(),String.class));
        });

        List<User> users = extUserMapper.getOrgUserByUserIds(organizationId,userIds);
        Map<String, String> userNameMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
        Map<String, String> defaultRelatedUserMap = MessageTemplateUtils.getDefaultRelatedUserMap();
        userNameMap.putAll(defaultRelatedUserMap);
        //开始准备数据
        Map<String, MessageTaskBlob> messageTaskBlobMap = messageTaskBlobs.stream().collect(Collectors.toMap(MessageTaskBlob::getId, item -> item));
        //获取默认数据
        Map<String, String> moduleMap = MessageTemplateUtils.getModuleMap();
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        Map<String, List<MessageTask>> messageEventMap = messageTasks.stream().collect(Collectors.groupingBy(MessageTask::getEvent));
        for (MessageTaskDTO messageTaskDTO : messageTaskDTOList) {
            messageTaskDTO.setName(moduleMap.get(messageTaskDTO.getType()));
            for (MessageTaskDetailDTO messageTaskDetailDTO : messageTaskDTO.getMessageTaskDetailDTOList()) {
                messageTaskDetailDTO.setEventName(eventMap.get(messageTaskDetailDTO.getEvent()));
                List<MessageTask> messageTaskList = messageEventMap.get(messageTaskDetailDTO.getEvent());
                List<OptionDTO> receivers = new ArrayList<>();
                String defaultTemplate = defaultTemplateMap.get(messageTaskDetailDTO.getEvent()+"_TEXT");
                Map<String,ProjectRobotConfigDTO> projectRobotConfigMap = new LinkedHashMap<>();
                if (CollectionUtils.isNotEmpty(messageTaskList)) {
                    for (MessageTask messageTask : messageTaskList) {
                        MessageTaskBlob messageTaskBlob = messageTaskBlobMap.get(messageTask.getId());
                        List<String> receiverIds =JSON.parseArray(messageTask.getReceivers(),String.class) ;
                        for (String receiverId : receiverIds) {
                            if (userNameMap.get(receiverId)!=null) {
                                OptionDTO optionDTO = new OptionDTO();
                                optionDTO.setId(receiverId);
                                optionDTO.setName(userNameMap.get(receiverId));
                                receivers.add(optionDTO);
                            }
                        }
                        String platform = messageTask.getReceiveType();
                        String defaultSubject = StringUtils.EMPTY;
                        if (StringUtils.equalsIgnoreCase(platform, NotificationConstants.SendType.MAIL)) {
                            defaultSubject = "Cordys 通知";
                        }
                        ProjectRobotConfigDTO projectRobotConfigDTO = new ProjectRobotConfigDTO();
                        projectRobotConfigDTO.setEnable(messageTask.getEnable());
                        projectRobotConfigDTO.setUseDefaultTemplate(messageTask.getUseDefaultTemplate());
                        projectRobotConfigDTO.setDefaultTemplate(defaultTemplate);
                        projectRobotConfigDTO.setTemplate(new String(messageTaskBlob.getTemplate()));
                        projectRobotConfigDTO.setSubject(defaultSubject);
                        projectRobotConfigDTO.setReceiveType(messageTask.getReceiveType());
                        projectRobotConfigMap.put(messageTask.getReceiveType(),projectRobotConfigDTO);
                    }
                }
                List<OptionDTO> distinctReceivers = receivers.stream().distinct().toList();
                messageTaskDetailDTO.setReceivers(distinctReceivers);
                messageTaskDetailDTO.setProjectRobotConfigMap(projectRobotConfigMap);
            }

        }
        return messageTaskDTOList;
    }



}
