package cn.cordys.crm.system.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.MessageTaskConfigWithNameDTO;
import cn.cordys.crm.system.dto.TimeDTO;
import cn.cordys.crm.system.dto.log.MessageTaskLogDTO;
import cn.cordys.crm.system.dto.request.MessageTaskBatchRequest;
import cn.cordys.crm.system.dto.request.MessageTaskRequest;
import cn.cordys.crm.system.dto.response.MessageTaskDTO;
import cn.cordys.crm.system.dto.response.MessageTaskDetailDTO;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.mapper.ExtRoleMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageNotificationService {

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;
    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtRoleMapper extRoleMapper;
    @Resource
    private LogService logService;

    private List<MessageTaskDTO> templateMessageTasks;

    private static final Map<String, String> TIME_UNIT_MAP = Map.of(
            "SECOND", "time.second",
            "MINUTE", "time.minute",
            "HOUR", "time.hour",
            "DAY", "time.day",
            "WEEK", "time.week",
            "MONTH", "time.month",
            "YEAR", "time.year"
    );

    @PostConstruct
    public void init() {
        this.templateMessageTasks = loadTemplateMessageTasks();
    }

    public MessageTask saveMessageTask(MessageTaskRequest request, String userId, String organizationId) {

        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();

        MessageTask existTask = extMessageTaskMapper.getMessageByModuleAndEvent(
                request.getModule(), request.getEvent(), organizationId);

        if (existTask != null) {
            return updateMessageTasks(request, userId, existTask, eventMap, organizationId);
        }

        MessageTask messageTask = buildMessageTask(request, organizationId, userId);

        String template = MessageTemplateUtils.getTemplate(request.getEvent());
        messageTask.setTemplate(template.getBytes(StandardCharsets.UTF_8));

        messageTaskMapper.insert(messageTask);
        saveConfigIfNeeded(request, organizationId);

        addLog(
                organizationId,
                messageTask.getId(),
                userId,
                eventMap.get(messageTask.getEvent()),
                null,
                buildLogDTO(messageTask, null)
        );

        return messageTask;
    }

    public MessageTask updateMessageTasks(
            MessageTaskRequest request,
            String userId,
            MessageTask oldTask,
            Map<String, String> eventMap,
            String organizationId) {

        MessageTask updateTask = new MessageTask();

        updateTask.setId(oldTask.getId());
        updateTask.setEmailEnable(request.isEmailEnable());
        updateTask.setSysEnable(request.isSysEnable());
        updateTask.setWeComEnable(request.isWeComEnable());
        updateTask.setDingTalkEnable(request.isDingTalkEnable());
        updateTask.setLarkEnable(request.isLarkEnable());
        updateTask.setUpdateUser(userId);
        updateTask.setUpdateTime(System.currentTimeMillis());

        messageTaskMapper.update(updateTask);

        MessageTaskConfigWithNameDTO oldConfig =
                getMessageConfig(request.getModule(), request.getEvent(), organizationId);

        saveOrUpdateConfig(request, organizationId);

        MessageTaskConfigWithNameDTO newConfig =
                buildLogMessageTaskConfigWithNameDTO(request.getConfig());

        addLog(
                organizationId,
                oldTask.getId(),
                userId,
                eventMap.get(request.getEvent()),
                buildLogDTO(oldTask, oldConfig),
                buildLogDTO(updateTaskToLogTask(oldTask, request), newConfig)
        );

        return updateTask;
    }

    public void batchSaveMessageTask(MessageTaskBatchRequest request, String organizationId, String userId) {

        List<MessageTask> oldList = extMessageTaskMapper.getMessageTaskList(organizationId);
        extMessageTaskMapper.updateMessageTask(request, organizationId);

        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();

        List<LogDTO> logs = oldList.stream()
                .map(task -> buildBatchLog(request, organizationId, userId, task, eventMap))
                .collect(Collectors.toList());

        logService.batchAdd(logs);
    }

    public List<MessageTaskDTO> getMessageList(String organizationId) {

        List<MessageTaskDTO> result =
                JSON.parseArray(JSON.toJSONString(templateMessageTasks), MessageTaskDTO.class);

        List<MessageTask> messageTasks = extMessageTaskMapper.getMessageTaskList(organizationId);

        if (CollectionUtils.isEmpty(messageTasks)) return result;

        Map<String, String> moduleMap = MessageTemplateUtils.getModuleMap();
        Map<String, String> eventMap = MessageTemplateUtils.getEventMap();

        Map<String, MessageTask> messageMap =
                messageTasks.stream().collect(Collectors.toMap(MessageTask::getEvent, Function.identity(), (a, b) -> b));

        for (MessageTaskDTO dto : result) {

            dto.setModuleName(moduleMap.get(dto.getModule()));

            for (MessageTaskDetailDTO detail : dto.getMessageTaskDetailDTOList()) {

                detail.setEventName(eventMap.get(detail.getEvent()));

                MessageTask task = messageMap.get(detail.getEvent());
                if (task == null) continue;

                detail.setEmailEnable(task.getEmailEnable());
                detail.setSysEnable(task.getSysEnable());
                detail.setWeComEnable(task.getWeComEnable());
                detail.setDingTalkEnable(task.getDingTalkEnable());
                detail.setLarkEnable(task.getLarkEnable());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MessageTaskConfigWithNameDTO getMessageConfig(String module, String event, String organizationId) {

        List<MessageTaskConfig> configs =
                messageTaskConfigMapper.selectListByLambda(
                        new LambdaQueryWrapper<MessageTaskConfig>()
                                .eq(MessageTaskConfig::getOrganizationId, organizationId)
                                .eq(MessageTaskConfig::getTaskType, module)
                                .eq(MessageTaskConfig::getEvent, event)
                );

        if (CollectionUtils.isEmpty(configs)) return null;

        MessageTaskConfig config = configs.getFirst();

        MessageTaskConfigWithNameDTO dto =
                JSON.parseObject(config.getValue(), MessageTaskConfigWithNameDTO.class);

        setUserNames(dto, dto.getUserIds(), dto.getRoleIds());

        return dto;
    }

    private MessageTask buildMessageTask(MessageTaskRequest request, String organizationId, String userId) {

        MessageTask task = new MessageTask();

        task.setId(IDGenerator.nextStr());
        task.setOrganizationId(organizationId);
        task.setTaskType(request.getModule());
        task.setEvent(request.getEvent());

        task.setCreateUser(userId);
        task.setCreateTime(System.currentTimeMillis());
        task.setUpdateUser(userId);
        task.setUpdateTime(System.currentTimeMillis());

        task.setEmailEnable(request.isEmailEnable());
        task.setSysEnable(request.isSysEnable());
        task.setWeComEnable(request.isWeComEnable());
        task.setDingTalkEnable(request.isDingTalkEnable());
        task.setLarkEnable(request.isLarkEnable());

        return task;
    }

    private void saveConfigIfNeeded(MessageTaskRequest request, String organizationId) {
        if (request.getConfig() != null) saveConfig(request, organizationId);
    }

    private void saveOrUpdateConfig(MessageTaskRequest request, String organizationId) {

        if (request.getConfig() == null) return;

        List<MessageTaskConfig> configs =
                messageTaskConfigMapper.selectListByLambda(
                        new LambdaQueryWrapper<MessageTaskConfig>()
                                .eq(MessageTaskConfig::getOrganizationId, organizationId)
                                .eq(MessageTaskConfig::getTaskType, request.getModule())
                                .eq(MessageTaskConfig::getEvent, request.getEvent())
                );

        checkTimeList(request);

        if (CollectionUtils.isNotEmpty(configs)) {
            MessageTaskConfig config = configs.getFirst();
            config.setValue(JSON.toJSONString(request.getConfig()));
            messageTaskConfigMapper.update(config);
            return;
        }

        saveConfig(request, organizationId);
    }

    private void saveConfig(MessageTaskRequest request, String organizationId) {

        checkTimeList(request);

        MessageTaskConfig config = new MessageTaskConfig();

        config.setId(IDGenerator.nextStr());
        config.setOrganizationId(organizationId);
        config.setTaskType(request.getModule());
        config.setEvent(request.getEvent());
        config.setValue(JSON.toJSONString(request.getConfig()));

        messageTaskConfigMapper.insert(config);
    }

    private void checkTimeList(MessageTaskRequest request) {

        if (request.getConfig() == null || request.getConfig().getTimeList() == null) return;

        List<TimeDTO> timeList = request.getConfig().getTimeList().stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimeValue() != null)
                .collect(Collectors.toList());

        request.getConfig().setTimeList(timeList);
    }

    private MessageTaskConfigWithNameDTO buildLogMessageTaskConfigWithNameDTO(MessageTaskConfigDTO config) {

        if (config == null) return null;

        MessageTaskConfigWithNameDTO dto = new MessageTaskConfigWithNameDTO();

        BeanUtils.copyBean(dto, config);

        setUserNames(dto, config.getUserIds(), config.getRoleIds());

        return dto;
    }

    private void setUserNames(MessageTaskConfigWithNameDTO dto, List<String> userIds, List<String> roleIds) {

        if (CollectionUtils.isNotEmpty(userIds)) {

            List<OptionDTO> users = extUserMapper.selectUserOptionByIds(userIds);

            if (userIds.contains("OWNER")) users.addFirst(new OptionDTO("OWNER", Translator.get("message.owner")));
            if (userIds.contains("CREATE_USER"))
                users.addFirst(new OptionDTO("CREATE_USER", Translator.get("message.create_user")));

            dto.setUserIdNames(users);
        }

        if (CollectionUtils.isEmpty(roleIds)) return;

        Set<String> internalRoleSet = new HashSet<>(extRoleMapper.getInternalRoleIds());

        List<String> internalRoles = roleIds.stream().filter(internalRoleSet::contains).toList();
        List<String> externalRoles = roleIds.stream().filter(id -> !internalRoleSet.contains(id)).toList();

        Map<String, String> roleNameMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(externalRoles)) {
            extRoleMapper.getIdNameByIds(externalRoles)
                    .forEach(role -> roleNameMap.put(role.getId(), role.getName()));
        }

        internalRoles.forEach(role ->
                roleNameMap.put(role, Translator.get("role." + role, role)));

        List<OptionDTO> roleNames = roleIds.stream()
                .map(id -> {
                    String name = roleNameMap.get(id);
                    return name == null ? null : new OptionDTO(id, name);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        dto.setRoleIdNames(roleNames);
    }

    private void addLog(String organizationId, String targetId, String userId,
                        String eventName, Object oldValue, Object newValue) {

        LogDTO logDTO = new LogDTO(
                organizationId,
                targetId,
                userId,
                LogType.UPDATE,
                LogModule.SYSTEM_MESSAGE_MESSAGE,
                eventName
        );

        logDTO.setOriginalValue(oldValue);
        logDTO.setModifiedValue(newValue);

        logService.add(logDTO);
    }

    private LogDTO buildBatchLog(
            MessageTaskBatchRequest request,
            String organizationId,
            String userId,
            MessageTask task,
            Map<String, String> eventMap) {

        MessageTaskLogDTO oldDTO = buildLogDTO(task, null);

        MessageTask newTask = new MessageTask();
        BeanUtils.copyBean(newTask, task);

        if (request.getEmailEnable() != null) newTask.setEmailEnable(request.getEmailEnable());
        if (request.getSysEnable() != null) newTask.setSysEnable(request.getSysEnable());
        if (request.getWeComEnable() != null) newTask.setWeComEnable(request.getWeComEnable());
        if (request.getDingTalkEnable() != null) newTask.setDingTalkEnable(request.getDingTalkEnable());
        if (request.getLarkEnable() != null) newTask.setLarkEnable(request.getLarkEnable());

        MessageTaskLogDTO newDTO = buildLogDTO(newTask, null);

        LogDTO logDTO = new LogDTO(
                organizationId,
                task.getId(),
                userId,
                LogType.UPDATE,
                LogModule.SYSTEM_MESSAGE_MESSAGE,
                eventMap.get(task.getEvent())
        );

        logDTO.setOriginalValue(oldDTO);
        logDTO.setModifiedValue(newDTO);

        return logDTO;
    }

    private static MessageTaskLogDTO buildLogDTO(MessageTask task, MessageTaskConfigWithNameDTO config) {

        MessageTaskLogDTO dto = new MessageTaskLogDTO();

        dto.setEmailEnable(toEnableText(task.getEmailEnable()));
        dto.setSysEnable(toEnableText(task.getSysEnable()));
        dto.setWeComEnable(toEnableText(task.getWeComEnable()));
        dto.setDingTalkEnable(toEnableText(task.getDingTalkEnable()));
        dto.setLarkEnable(toEnableText(task.getLarkEnable()));
        dto.setEvent(MessageTemplateUtils.getEventMap().get(task.getEvent()));

        if (config == null) return dto;

        dto.setOwnerEnable(toEnableText(config.isOwnerEnable()));
        dto.setOwnerLevel(config.getOwnerLevel());
        dto.setRoleEnable(toEnableText(config.isRoleEnable()));
        dto.setUserIdNames(extractNames(config.getUserIdNames()));
        dto.setRoleIdNames(extractNames(config.getRoleIdNames()));
        dto.setTimes(buildTimes(config.getTimeList()));

        return dto;
    }

    private static String toEnableText(Boolean enable) {
        return Boolean.TRUE.equals(enable)
                ? Translator.get("log.enable.true")
                : Translator.get("log.enable.false");
    }

    private static List<String> extractNames(List<OptionDTO> options) {
        if (CollectionUtils.isEmpty(options)) return Collections.emptyList();
        return options.stream().map(OptionDTO::getName).collect(Collectors.toList());
    }

    private static List<String> buildTimes(List<TimeDTO> timeList) {
        if (CollectionUtils.isEmpty(timeList)) return Collections.emptyList();
        return timeList.stream()
                .map(MessageNotificationService::buildTimeText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static String buildTimeText(TimeDTO dto) {

        String key = TIME_UNIT_MAP.get(dto.getTimeUnit().toUpperCase());
        if (key == null) return null;

        return dto.getTimeValue() + Translator.get(key);
    }

    private MessageTask updateTaskToLogTask(MessageTask oldTask, MessageTaskRequest request) {

        MessageTask task = new MessageTask();

        BeanUtils.copyBean(task, oldTask);

        task.setEmailEnable(request.isEmailEnable());
        task.setSysEnable(request.isSysEnable());
        task.setWeComEnable(request.isWeComEnable());
        task.setDingTalkEnable(request.isDingTalkEnable());
        task.setLarkEnable(request.isLarkEnable());

        return task;
    }

    private List<MessageTaskDTO> loadTemplateMessageTasks() {

        StringBuilder json = new StringBuilder();

        try (InputStream inputStream = getClass().getResourceAsStream("/task/message_task.json")) {

            Objects.requireNonNull(inputStream);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return JSON.parseArray(json.toString(), MessageTaskDTO.class);
    }
}