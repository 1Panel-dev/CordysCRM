package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.ScopeKey;
import cn.cordys.crm.system.constants.message.ReceiveMethodEnum;
import cn.cordys.crm.system.constants.message.ScopeIdTypeEnum;
import cn.cordys.crm.system.constants.message.TemplateEnum;
import cn.cordys.crm.system.constants.message.TriggerTypeEnum;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.dto.MessageTaskConfigWithNameDTO;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.crm.system.dto.log.MessageTaskLogDTO;
import cn.cordys.crm.system.dto.message.MessageAddRequest;
import cn.cordys.crm.system.dto.message.MessageBatchRequest;
import cn.cordys.crm.system.dto.message.MessageLogDTO;
import cn.cordys.crm.system.dto.message.MessageRequest;
import cn.cordys.crm.system.dto.message.MessageTaskCountDTO;
import cn.cordys.crm.system.dto.message.MessageTaskResponse;
import cn.cordys.crm.system.dto.message.MessageUpdateRequest;
import cn.cordys.crm.system.dto.message.ScheduleTypeConfig;
import cn.cordys.crm.system.dto.message.TriggerTypeConfigDTO;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageNotificationService {

    // 系统通知类型展示顺序：线索、客户、商机、合同、订单、代办(审批)、跟进
    private static final List<String> SYS_TASK_TYPE_ORDER = List.of(
            NotificationConstants.Module.CLUE,
            NotificationConstants.Module.CUSTOMER,
            NotificationConstants.Module.OPPORTUNITY,
            NotificationConstants.Module.CONTRACT,
            NotificationConstants.Module.ORDER,
            NotificationConstants.Module.APPROVAL,
            NotificationConstants.Module.FOLLOW_UP);

    @Resource
    private ExtMessageTaskMapper extMessageTaskMapper;
    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;
    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private BaseService baseService;

    /**
     * 获取系统类型通知和数量
     */
    @Transactional(readOnly = true)
    public List<MessageTaskCountDTO> getSysTaskTypeCount(String organizationId) {
        List<MessageTaskCountDTO> taskTypeCount = extMessageTaskMapper.getTaskTypeCount(organizationId, "SYSTEM");

        Map<String, String> moduleMap = MessageTemplateUtils.getModuleMap();
        taskTypeCount.forEach(dto -> dto.setName(moduleMap.get(dto.getTaskType())));

        return taskTypeCount.stream()
                .sorted(Comparator.comparingInt((MessageTaskCountDTO dto) -> {
                    int index = SYS_TASK_TYPE_ORDER.indexOf(dto.getTaskType());
                    return index < 0 ? Integer.MAX_VALUE : index;
                }))
                .toList();
    }

    /**
     * 获取自定义类型通知和数量
     */
    @Transactional(readOnly = true)
    public List<MessageTaskCountDTO> getCustomTaskTypeCount(String organizationId) {
        List<MessageTaskCountDTO> taskTypeCount = extMessageTaskMapper.getTaskTypeCount(organizationId, "CUSTOM");

        if (CollectionUtils.isEmpty(taskTypeCount)) {
            return taskTypeCount;
        }

        List<String> formIds = taskTypeCount.stream().map(MessageTaskCountDTO::getTaskType).toList();
        Map<String, String> formNameMap = customFormMapper.selectByIds(formIds).stream()
                .collect(Collectors.toMap(CustomForm::getId, CustomForm::getName));

        taskTypeCount.forEach(dto -> dto.setName(formNameMap.get(dto.getTaskType())));

        return taskTypeCount;
    }

    /**
     * 获取通知场景列表
     */
    @Transactional(readOnly = true)
    public List<MessageTaskResponse> getMessageList(MessageRequest request) {
        List<MessageTaskResponse> messageList = extMessageTaskMapper.getMessageList(request);

        List<String> createUserIds = messageList.stream()
                .map(MessageTaskResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = messageList.stream()
                .map(MessageTaskResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
        for (MessageTaskResponse response : messageList) {
            response.setMembers(buildMembers(response.getScopeId()));
            response.setConfig(JSON.parseObject(response.getValue(), TriggerTypeConfigDTO.class));
            response.setTemplateValue();
            response.setCreateUserName(userNameMap.get(response.getCreateUser()));
            response.setUpdateUserName(userNameMap.get(response.getUpdateUser()));
        }
        return messageList;
    }

    /**
     * 接收人转换：特殊接收人(负责人/协作人...)直接取枚举翻译，其余按用户/角色/部门解析
     */
    private List<ScopeNameDTO> buildMembers(String scopeId) {
        List<String> scopeIds = JSON.parseArray(scopeId, String.class);
        if (CollectionUtils.isEmpty(scopeIds)) {
            return new ArrayList<>();
        }

        Map<String, ScopeNameDTO> scopeMap = userExtendService
                .getScope(scopeIds.stream().filter(id -> ScopeIdTypeEnum.get(id) == null).toList())
                .stream()
                .collect(Collectors.toMap(ScopeNameDTO::getId, Function.identity(), (a, b) -> b));

        List<ScopeNameDTO> members = new ArrayList<>();

        for (String id : scopeIds) {
            ScopeIdTypeEnum scopeIdType = ScopeIdTypeEnum.get(id);
            if (scopeIdType != null) {
                members.add(ScopeNameDTO.builder()
                        .id(id)
                        .scope(ScopeKey.USER.name())
                        .name(Translator.get(scopeIdType.getDescription()))
                        .build());
            } else if (scopeMap.containsKey(id)) {
                members.add(scopeMap.get(id));
            }
        }

        return members;
    }

    /**
     * 添加通知场景
     */
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_MESSAGE, type = LogType.ADD, operator = "{#userId}")
    public void add(MessageAddRequest request, String userId, String orgId) {
        checkNameUnique(orgId, request.getTaskType(), request.getName(), null);
        String taskId = IDGenerator.nextStr();
        MessageTask task = new MessageTask();
        task.setId(taskId);
        task.setEvent("CUSTOM_EVENT_" + taskId);
        task.setName(request.getName());
        task.setTaskType(request.getTaskType());
        task.setSysEnable(false);
        task.setOrganizationId(orgId);
        task.setTemplate(request.getTemplate());
        task.setCreateUser(userId);
        task.setCreateTime(System.currentTimeMillis());
        task.setUpdateUser(userId);
        task.setUpdateTime(System.currentTimeMillis());
        task.setType(request.getType());
        task.setEnable(true);
        task.setScopeId(JSON.toJSONString(request.getScopeId()));
        task.setReceiveMethod(request.getReceiveMethod());
        task.setFormType(request.getFormType());
        messageTaskMapper.insert(task);

        MessageTaskConfig taskConfig = new MessageTaskConfig();
        taskConfig.setId(IDGenerator.nextStr());
        taskConfig.setOrganizationId(orgId);
        taskConfig.setEvent("CUSTOM_EVENT_" + taskId);
        taskConfig.setTriggerType(request.getTriggerType());
        taskConfig.setMessageTaskId(taskId);
        taskConfig.setValue(JSON.toJSONString(request.getConfig()));
        messageTaskConfigMapper.insert(taskConfig);

        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(null)
                .modifiedValue(task)
                .resourceName(task.getName())
                .resourceId(task.getId())
                .build());

    }

    /**
     * 编辑通知场景
     */
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_MESSAGE, type = LogType.UPDATE, operator = "{#userId}")
    public void update(MessageUpdateRequest request, String userId, String orgId) {
        MessageTask oldTask = messageTaskMapper.selectByPrimaryKey(request.getId());
        String taskType = StringUtils.isNotBlank(request.getTaskType()) ? request.getTaskType() : oldTask.getTaskType();
        checkNameUnique(orgId, taskType, request.getName(), request.getId());
        MessageTaskConfig oldConfig = messageTaskConfigMapper.selectListByLambda(
                new LambdaQueryWrapper<MessageTaskConfig>()
                        .eq(MessageTaskConfig::getMessageTaskId, request.getId())).getFirst();
        MessageLogDTO messageLogDTO = new MessageLogDTO();
        messageLogDTO.setTask(oldTask);
        messageLogDTO.setTaskConfig(oldConfig);

        MessageTask task = BeanUtils.copyBean(new MessageTask(), request);
        // 接收人在请求里是 List<String>，与实体 scopeId(String) 类型不一致，copyBean 不会拷贝，需显式序列化
        if (request.getScopeId() != null) {
            task.setScopeId(JSON.toJSONString(request.getScopeId()));
        }
        task.setUpdateTime(System.currentTimeMillis());
        task.setUpdateUser(userId);
        messageTaskMapper.update(task);

        MessageTaskConfig config = messageTaskConfigMapper.selectListByLambda(
                new LambdaQueryWrapper<MessageTaskConfig>()
                        .eq(MessageTaskConfig::getMessageTaskId, request.getId())).getFirst();
        config.setValue(JSON.toJSONString(request.getConfig()));
        config.setTriggerType(request.getTriggerType());
        messageTaskConfigMapper.update(config);

        MessageTask newTask = messageTaskMapper.selectByPrimaryKey(request.getId());
        MessageTaskConfig newConfig = messageTaskConfigMapper.selectListByLambda(
                new LambdaQueryWrapper<MessageTaskConfig>()
                        .eq(MessageTaskConfig::getMessageTaskId, request.getId())).getFirst();
        MessageLogDTO newLogDTO = new MessageLogDTO();
        newLogDTO.setTask(newTask);
        newLogDTO.setTaskConfig(newConfig);

        //添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(messageLogDTO)
                .resourceName(oldTask.getName())
                .modifiedValue(newLogDTO)
                .resourceId(oldTask.getId())
                .build());
    }

    /**
     * 校验同一模块(taskType)下通知场景名称唯一，excludeId 用于编辑时排除自身
     */
    private void checkNameUnique(String organizationId, String taskType, String name, String excludeId) {
        if (StringUtils.isAnyBlank(organizationId, taskType, name)) {
            return;
        }
        boolean duplicate = messageTaskMapper.selectListByLambda(
                        new LambdaQueryWrapper<MessageTask>()
                                .eq(MessageTask::getOrganizationId, organizationId)
                                .eq(MessageTask::getTaskType, taskType)
                                .eq(MessageTask::getName, name))
                .stream()
                .anyMatch(task -> !StringUtils.equals(task.getId(), excludeId));
        if (duplicate) {
            throw new GenericException(Translator.get("message.name_repeat"));
        }
    }

    /**
     * 删除
     */
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_MESSAGE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        MessageTask task = messageTaskMapper.selectByPrimaryKey(id);

        if (task == null) {
            return;
        }
        messageTaskMapper.deleteByPrimaryKey(id);

        LambdaQueryWrapper<MessageTaskConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MessageTaskConfig::getMessageTaskId, id);
        messageTaskConfigMapper.deleteByLambda(queryWrapper);

        // 设置日志上下文
        OperationLogContext.setResourceName(task.getName());
    }

    /**
     * 删除自定义表单下的全部消息通知(任务及配置)。自定义表单以表单ID作为任务类型。
     */
    public void deleteCustomFormMessage(String customFormId) {
        List<MessageTask> tasks = messageTaskMapper.selectListByLambda(
                new LambdaQueryWrapper<MessageTask>().eq(MessageTask::getTaskType, customFormId));
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        List<String> taskIds = tasks.stream().map(MessageTask::getId).toList();
        messageTaskConfigMapper.deleteByLambda(
                new LambdaQueryWrapper<MessageTaskConfig>().in(MessageTaskConfig::getMessageTaskId, taskIds));
        messageTaskMapper.deleteByLambda(new LambdaQueryWrapper<MessageTask>().in(MessageTask::getId, taskIds));
    }

    /**
     * 新版本消息通知
     * 处理初始化数据
     * <p>
     * 该初始化在应用启动时执行，不在 HTTP 请求线程中，无法通过 Accept-Language 解析语言。
     * 因此显式指定简体中文，避免任务名称按 JVM 默认语言（通常为英文）入库。
     */
    public void modifyMessageNotification() {
        Locale previousLocale = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            doModifyMessageNotification();
        } finally {
            LocaleContextHolder.setLocale(previousLocale);
        }
    }

    private void doModifyMessageNotification() {
        // 删除旧通知
        List<String> deleteEvents = List.of("CLUE_FOLLOW_UP_RECORD_COMMENT_MENTIONED",
                "CUSTOMER_FOLLOW_UP_PLAN_DUE",
                "CLUE_FOLLOW_UP_PLAN_DUE",
                "BUSINESS_FOLLOW_UP_PLAN_DUE",
                "SYNC_ORGANIZATION_STRUCTURE",
                "CUSTOM_FORM_DATA_APPROVAL",
                "CLUE_FOLLOW_UP_PLAN_COMMENT_ADDED",
                "CLUE_FOLLOW_UP_PLAN_COMMENT_MENTIONED",
                "CLUE_FOLLOW_UP_RECORD_COMMENT_ADDED",
                "OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_ADDED",
                "OPPORTUNITY_FOLLOW_UP_PLAN_COMMENT_MENTIONED",
                "OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_ADDED",
                "OPPORTUNITY_FOLLOW_UP_RECORD_COMMENT_MENTIONED",
                "CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED",
                "CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED",
                "CUSTOMER_FOLLOW_UP_RECORD_COMMENT_ADDED",
                "CUSTOMER_FOLLOW_UP_RECORD_COMMENT_MENTIONED",
                "FOLLOW_UP_RECORD_COMMENT_MENTIONED",
                "FOLLOW_UP_PLAN_COMMENT_MENTIONED",
                "FOLLOW_UP_PLAN_COMMENT_ADDED",
                "FOLLOW_UP_RECORD_COMMENT_ADDED",
                "CLUE_IMPORT",
                "BUSINESS_IMPORT",
                "APPROVAL_COMMENT_MENTIONED",
                "CONTRACT_PAYMENT_EXPIRED",
                "CONTRACT_PAYMENT_EXPIRING"
        );
        messageTaskMapper.deleteByLambda(new LambdaQueryWrapper<MessageTask>()
                .in(MessageTask::getEvent, deleteEvents));

        messageTaskConfigMapper.deleteByLambda(new LambdaQueryWrapper<MessageTaskConfig>()
                .in(MessageTaskConfig::getEvent, List.of("CONTRACT_PAYMENT_EXPIRED", "CONTRACT_PAYMENT_EXPIRING")));

        List<MessageTask> messageTasks = messageTaskMapper.selectAll(null);
        Map<String, String> eventNameMap = MessageTemplateUtils.getEventMap();

        String receiveMethod = JSON.toJSONString(
                List.of(ReceiveMethodEnum.SYSTEM.name(), ReceiveMethodEnum.WECOM.name(),
                        ReceiveMethodEnum.DINGTALK.name(), ReceiveMethodEnum.LARK.name()));

        //3 匹配每个 Event，逐条更新对应的 task
        for (MessageTask messageTask : messageTasks) {
            switch (messageTask.getEvent()) {
                // -----客户管理----

                // 新建客户
                case NotificationConstants.Event.CUSTOMER_ADD ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_ADD_TEXT.name()), eventNameMap,
                                messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_ADD), "CUSTOMER");
                // 删除客户
                case NotificationConstants.Event.CUSTOMER_DELETED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_DELETED_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_DELETED), "CUSTOMER");
                // 新建协作人(系统)
                case NotificationConstants.Event.CUSTOMER_COLLABORATION_ADD ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_COLLABORATION_ADD_TEXT.name()),
                                eventNameMap, messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_COLLABORATION_ADD),
                                "CUSTOMER");
                // 转移客户
                case NotificationConstants.Event.CUSTOMER_TRANSFERRED_CUSTOMER ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_TRANSFERRED_CUSTOMER_OWNER_TEXT.name(),
                                        TemplateEnum.CUSTOMER_TRANSFERRED_CUSTOMER_OLD_OWNER_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_TRANSFERRED_CUSTOMER),"CUSTOMER");
                // 客户自动移入公海（到时间）
                case NotificationConstants.Event.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS_TEXT.name()),
                                eventNameMap, messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS),"CUSTOMER");
                // 客户被动移入公海
                case NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_MOVED_HIGH_SEAS_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS),"CUSTOMER");
                // 公海客户被分配
                case NotificationConstants.Event.HIGH_SEAS_CUSTOMER_DISTRIBUTED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.HIGH_SEAS_CUSTOMER_DISTRIBUTED_TEXT.name()),
                                eventNameMap, messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.HIGH_SEAS_CUSTOMER_DISTRIBUTED),"CUSTOMER");
                // 客户审批
                case NotificationConstants.Event.CUSTOMER_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"CUSTOMER");
                // 新建联系人
                case NotificationConstants.Event.CUSTOMER_CONCAT_ADD ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CUSTOMER_CONCAT_ADD_TEXT.name()), eventNameMap,
                                messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_CONCAT_ADD),"CUSTOMER_CONTACT");

                // -----线索管理----
                // 新建线索
                case NotificationConstants.Event.CLUE_ADD ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_ADD_TEXT.name()), eventNameMap, messageTask,
                                List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_ADD),"CLUE");
                // 删除线索
                case NotificationConstants.Event.CLUE_DELETED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_DELETED_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_DELETED),"CLUE");
                // 移入线索池(系统)
                case NotificationConstants.Event.CLUE_AUTOMATIC_MOVE_POOL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_AUTOMATIC_MOVE_POOL_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_AUTOMATIC_MOVE_POOL),"CLUE");
                // 移入线索池(手动)
                case NotificationConstants.Event.CLUE_MOVED_POOL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_MOVED_POOL_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_MOVED_POOL),"CLUE");
                // 转为客户
                case NotificationConstants.Event.CLUE_CONVERT_CUSTOMER -> updateTaskAndAddConfig(
                        List.of(TemplateEnum.CLUE_CONVERT_CUSTOMER_ADD_TEXT.name(),
                                TemplateEnum.CLUE_CONVERT_CUSTOMER_RELATE_TEXT.name()), eventNameMap, messageTask,
                        List.of("OLD_OWNER"), receiveMethod,
                        TriggerTypeEnum.ACTION.name(),
                        buildActionConfigValue(NotificationConstants.Event.CLUE_CONVERT_CUSTOMER),"CLUE");
                // 转为商机
                case NotificationConstants.Event.CLUE_CONVERT_BUSINESS ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_CONVERT_BUSINESS_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_CONVERT_BUSINESS),"CLUE");
                // 转移线索
                case NotificationConstants.Event.TRANSFER_CLUE -> updateTaskAndAddConfig(
                        List.of(TemplateEnum.TRANSFER_CLUE_OWNER_TEXT.name(),
                                TemplateEnum.TRANSFER_CLUE_OLD_OWNER_TEXT.name()), eventNameMap, messageTask,
                        List.of("OLD_OWNER", "OWNER"), receiveMethod,
                        TriggerTypeEnum.ACTION.name(),
                        buildActionConfigValue(NotificationConstants.Event.TRANSFER_CLUE),"CLUE");
                // 分配线索
                case NotificationConstants.Event.CLUE_DISTRIBUTED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_DISTRIBUTED_TEXT.name()), eventNameMap,
                                messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.CLUE_DISTRIBUTED),"CLUE");
                // 线索审批
                case NotificationConstants.Event.CLUE_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CLUE_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"CLUE");

                // -----商机-----
                // 新建商机
                case NotificationConstants.Event.BUSINESS_ADD ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.BUSINESS_ADD_TEXT.name()), eventNameMap,
                                messageTask, List.of("OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.BUSINESS_ADD),"OPPORTUNITY");
                // 商机删除
                case NotificationConstants.Event.BUSINESS_DELETED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.BUSINESS_DELETED_TEXT.name()), eventNameMap,
                                messageTask, List.of("OLD_OWNER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.BUSINESS_DELETED),"OPPORTUNITY");
                // 商机转移
                case NotificationConstants.Event.BUSINESS_TRANSFER -> updateTaskAndAddConfig(
                        List.of(TemplateEnum.BUSINESS_TRANSFER_OLD_OWNER_TEXT.name(),
                                TemplateEnum.BUSINESS_TRANSFER_OWNER_TEXT.name()), eventNameMap, messageTask,
                        List.of("OLD_OWNER", "OWNER"), receiveMethod,
                        TriggerTypeEnum.ACTION.name(),
                        buildActionConfigValue(NotificationConstants.Event.BUSINESS_TRANSFER),"OPPORTUNITY");
                // 商机审批
                case NotificationConstants.Event.BUSINESS_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.BUSINESS_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"OPPORTUNITY");
                // 删除报价
                case NotificationConstants.Event.BUSINESS_QUOTATION_DELETED ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_DELETED_TEXT.name()),
                                eventNameMap, messageTask, List.of("CREATE_USER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.BUSINESS_QUOTATION_DELETED),"OPPORTUNITY_QUOTATION");
                // 报价到期
                case NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(dto.getUserIds())) {
                        scopeId.addAll(dto.getUserIds());
                    }
                    if (CollectionUtils.isNotEmpty(dto.getRoleIds())) {
                        scopeId.addAll(dto.getRoleIds());
                    }
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    scopeId.add("CREATE_USER");
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_EXPIRED_TEXT.name()), config,
                            eventNameMap, messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("untilTime", "0", "08：00：00", "ONLY_ONE")),"OPPORTUNITY_QUOTATION");

                }
                // 报价即将到期
                case NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    scopeId.add("CREATE_USER");
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_EXPIRING_TEXT.name()), config,
                            eventNameMap, messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("untilTime", "0", "08：00：00", "ONLY_ONE")),"OPPORTUNITY_QUOTATION");

                }
                // 报价审批
                case NotificationConstants.Event.BUSINESS_QUOTATION_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_APPROVAL_TEXT.name()),
                                eventNameMap, messageTask, List.of("SUBMITTER", "CREATE_USER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"OPPORTUNITY_QUOTATION");

                //-------订单-----
                // 审批订单
                case NotificationConstants.Event.ORDER_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.ORDER_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"ORDER");

                //-----合同-----
                // 合同作废
                case NotificationConstants.Event.CONTRACT_VOID -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_VOID));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_VOID_TEXT.name()), config, eventNameMap,
                            messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.FIELD.name(),
                            buildFieldConfigValue(buildCondition("stage", List.of("VOID"), true, "IN",
                                    "SELECT_MULTIPLE")),"CONTRACT");
                }
                case NotificationConstants.Event.CONTRACT_ARCHIVED -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_ARCHIVED));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_ARCHIVED_TEXT.name()), config, eventNameMap,
                            messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.FIELD.name(),
                            buildFieldConfigValue(buildCondition("stage", List.of("ARCHIVED"), true, "IN",
                                    "SELECT_MULTIPLE")),"CONTRACT");
                }
                // 合同到期
                case NotificationConstants.Event.CONTRACT_EXPIRED -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_EXPIRED));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_EXPIRED_TEXT.name()), config, eventNameMap,
                            messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("endTime", "0", "08：00：00", "ONLY_ONE")),"CONTRACT");
                }
                // 合同即将到期
                case NotificationConstants.Event.CONTRACT_EXPIRING -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_EXPIRING));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    messageTask.setSysEnable(false);
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_EXPIRING_TEXT.name()), config, eventNameMap,
                            messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("endTime", "0", "08：00：00", "ONLY_ONE")),"CONTRACT");
                }
                // 合同审批
                case NotificationConstants.Event.CONTRACT_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.CONTRACT_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"CONTRACT");
                // 回款计划到期
                /*case NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_PAYMENT_EXPIRED_TEXT.name()), config,
                            eventNameMap, messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("plan_end_time", "0", "08：00：00", "ONLY_ONE")));
                }*/
                // 回款计划即将到期
                /*case NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING -> {
                    List<MessageTaskConfig> configs = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .eq(MessageTaskConfig::getEvent,
                                            NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING));
                    MessageTaskConfig config = configs.getFirst();
                    MessageTaskConfigWithNameDTO dto = JSON.parseObject(config.getValue(),
                            MessageTaskConfigWithNameDTO.class);
                    List<String> scopeId = new ArrayList<>();
                    scopeId.addAll(dto.getUserIds());
                    scopeId.addAll(dto.getRoleIds());
                    if (dto.getOwnerLevel() != 0) {
                        scopeId.add("SUPERVISOR");
                    }
                    updateTaskAndUpdateConfig(List.of(TemplateEnum.CONTRACT_PAYMENT_EXPIRING_TEXT.name()), config,
                            eventNameMap, messageTask, scopeId, receiveMethod,
                            TriggerTypeEnum.SCHEDULE.name(),
                            buildScheduleConfigValue(buildScheduleValue("plan_end_time", "0", "08：00：00", "ONLY_ONE")));
                }*/
                // 审批发票
                case NotificationConstants.Event.INVOICE_APPROVAL ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.INVOICE_APPROVAL_TEXT.name()), eventNameMap,
                                messageTask, List.of("SUBMITTER", "OWNER"), receiveMethod,
                                TriggerTypeEnum.FIELD.name(),
                                buildFieldConfigValue(
                                        buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                                                "SELECT_MULTIPLE")),"CONTRACT_INVOICE");

                //-----跟进-------

                //-----代办------
                // 代办提醒
                case NotificationConstants.Event.APPROVAL_TODO ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.APPROVAL_TODO_TEXT.name()), eventNameMap,
                                messageTask, List.of("APPROVER"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.APPROVAL_TODO),"APPROVAL");
                // 抄送代办
                case NotificationConstants.Event.APPROVAL_CC ->
                        updateTaskAndAddConfig(List.of(TemplateEnum.APPROVAL_CC_TEXT.name()), eventNameMap, messageTask,
                                List.of("CC"), receiveMethod,
                                TriggerTypeEnum.ACTION.name(),
                                buildActionConfigValue(NotificationConstants.Event.APPROVAL_CC),"APPROVAL");

                default -> {
                }
            }
        }

        // 补充默认初始化配置
        // ------客户-------
        // 初始化：编辑客户 通知
        addTaskAndConfig(List.of(TemplateEnum.CUSTOMER_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CUSTOMER_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CUSTOMER, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CUSTOMER_UPDATE),"CUSTOMER");
        // 领取客户
        addTaskAndConfig(List.of(TemplateEnum.CUSTOMER_RECEIVE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CUSTOMER_RECEIVE, List.of("CUSTOMER_SEAS_OWNER"),
                NotificationConstants.Module.CUSTOMER, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CUSTOMER_RECEIVE),"CUSTOMER");
        // 编辑联系人
        addTaskAndConfig(List.of(TemplateEnum.CUSTOMER_CONCAT_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CUSTOMER_CONCAT_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CUSTOMER, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_CONCAT_UPDATE),"CUSTOMER_CONTACT");
        // 删除联系人
        addTaskAndConfig(List.of(TemplateEnum.CUSTOMER_CONCAT_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CUSTOMER_CONCAT_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.CUSTOMER, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_CONCAT_DELETE),"CUSTOMER_CONTACT");
        // 新建协作人(手动)
        addTaskAndConfig(List.of(TemplateEnum.CUSTOMER_COLLABORATION_ADD_MANUAL_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CUSTOMER_COLLABORATION_ADD_MANUAL, List.of("COLLABORATION"),
                NotificationConstants.Module.CUSTOMER, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CUSTOMER_COLLABORATION_ADD_MANUAL),"CUSTOMER");

        //------线索 -----
        // 编辑线索
        addTaskAndConfig(List.of(TemplateEnum.CLUE_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CLUE_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CLUE, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CLUE_UPDATE),"CLUE");
        // 领取线索
        addTaskAndConfig(List.of(TemplateEnum.CLUE_RECEIVE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CLUE_RECEIVE, List.of("CLUE_SEAS_OWNER"),
                NotificationConstants.Module.CLUE, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CLUE_RECEIVE),"CLUE");

        //------商机------
        // 编辑商机
        addTaskAndConfig(List.of(TemplateEnum.BUSINESS_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.BUSINESS_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.OPPORTUNITY, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.BUSINESS_UPDATE),"OPPORTUNITY");
        // 新建报价
        addTaskAndConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.BUSINESS_QUOTATION_ADD, List.of("CREATE_USER"),
                NotificationConstants.Module.OPPORTUNITY, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.BUSINESS_QUOTATION_ADD),"OPPORTUNITY_QUOTATION");
        // 编辑报价
        addTaskAndConfig(List.of(TemplateEnum.BUSINESS_QUOTATION_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.BUSINESS_QUOTATION_UPDATE, List.of("CREATE_USER"),
                NotificationConstants.Module.OPPORTUNITY, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.BUSINESS_QUOTATION_UPDATE),"OPPORTUNITY_QUOTATION");

        //------订单------
        // 新建订单
        addTaskAndConfig(List.of(TemplateEnum.ORDER_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.ORDER_ADD, List.of("OWNER"),
                NotificationConstants.Module.ORDER, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.ORDER_ADD),"ORDER");
        // 编辑订单
        addTaskAndConfig(List.of(TemplateEnum.ORDER_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.ORDER_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.ORDER, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.ORDER_UPDATE),"ORDER");
        // 删除订单
        addTaskAndConfig(List.of(TemplateEnum.ORDER_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.ORDER_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.ORDER, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.ORDER_DELETE),"ORDER");

        //------合同-------
        // 新建合同
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_ADD, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CONTRACT_ADD),"CONTRACT");
        // 编辑合同
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CONTRACT_UPDATE),"CONTRACT");
        // 删除合同
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CONTRACT_DELETE),"CONTRACT");
        // 新建回款计划
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_PAYMENT_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_PAYMENT_ADD, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CONTRACT_PAYMENT_ADD),"CONTRACT_PAYMENT");
        // 编辑回款计划
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_PAYMENT_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_PAYMENT_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CONTRACT_PAYMENT_UPDATE),"CONTRACT_PAYMENT");
        // 删除回款计划
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_PAYMENT_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_PAYMENT_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CONTRACT_PAYMENT_DELETE),"CONTRACT_PAYMENT");
        // 新建回款记录
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_RECORD_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_RECORD_ADD, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.CONTRACT_RECORD_ADD),"CONTRACT_RECORD");
        // 编辑回款记录
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_RECORD_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_RECORD_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CONTRACT_RECORD_UPDATE),"CONTRACT_RECORD");
        // 删除回款记录
        addTaskAndConfig(List.of(TemplateEnum.CONTRACT_RECORD_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.CONTRACT_RECORD_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.CONTRACT_RECORD_DELETE),"CONTRACT_RECORD");
        // 新建发票记录
        addTaskAndConfig(List.of(TemplateEnum.INVOICE_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.INVOICE_ADD, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.INVOICE_ADD),"CONTRACT_INVOICE");
        // 编辑发票记录
        addTaskAndConfig(List.of(TemplateEnum.INVOICE_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.INVOICE_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.INVOICE_UPDATE),"CONTRACT_INVOICE");
        // 删除发票记录
        addTaskAndConfig(List.of(TemplateEnum.INVOICE_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.INVOICE_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.CONTRACT, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.INVOICE_DELETE),"CONTRACT_INVOICE");

        //-------跟进--------
        // 新建跟进记录
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_ADD, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_ADD),"FOLLOW_UP_RECORD");
        // 编辑跟进记录
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_UPDATE),"FOLLOW_UP_RECORD");
        // 删除跟进记录
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_DELETE),"FOLLOW_UP_RECORD");
        // 跟进记录被评论
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_COMMENT_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT),"FOLLOW_UP_RECORD");
        // 评论被回复
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_COMMENT_REPALY_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY,
                List.of("REPLY_USER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY),"FOLLOW_UP_RECORD");
        // 评论@我
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_RECORD_COMMENT_MENTIONED_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED,
                List.of("MENTION_USER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED),"FOLLOW_UP_RECORD");
        // 新建跟进计划
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_ADD_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_ADD, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_ADD),"FOLLOW_UP_PLAN");
        // 编辑跟进计划
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_UPDATE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_UPDATE, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_UPDATE),"FOLLOW_UP_PLAN");
        // 删除跟进计划
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_DELETE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_DELETE, List.of("OLD_OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_DELETE),"FOLLOW_UP_PLAN");
        // 跟进计划被评论
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_COMMENT_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT),"FOLLOW_UP_PLAN");
        // 计划评论被回复
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_COMMENT_REPALY_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_REPALY, List.of("REPLY_USER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_REPALY),"FOLLOW_UP_PLAN");
        // 计划评论@我
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_COMMENT_MENTIONED_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_MENTIONED,
                List.of("MENTION_USER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_MENTIONED),"FOLLOW_UP_PLAN");
        // 跟进计划到期提醒
        addTaskAndConfig(List.of(TemplateEnum.FOLLOW_UP_PLAN_DUE_TEXT.name()), eventNameMap,
                NotificationConstants.Event.FOLLOW_UP_PLAN_DUE, List.of("OWNER"),
                NotificationConstants.Module.FOLLOW_UP, receiveMethod,
                TriggerTypeEnum.SCHEDULE.name(),
                buildScheduleConfigValue(buildScheduleValue("estimatedTime", "0", "08：00：00", "ONLY_ONE")),"FOLLOW_UP_PLAN");

        //-----代办-------
        // 单据被评论
        addTaskAndConfig(List.of(TemplateEnum.APPROVAL_COMMENT_TEXT.name()), eventNameMap,
                NotificationConstants.Event.APPROVAL_COMMENT, List.of("OWNER"),
                NotificationConstants.Module.APPROVAL, receiveMethod,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue(NotificationConstants.Event.APPROVAL_COMMENT),"APPROVAL");
        // 回复
        addTaskAndConfig(List.of(TemplateEnum.APPROVAL_COMMENT_REPALY_TEXT.name()), eventNameMap,
                NotificationConstants.Event.APPROVAL_COMMENT_REPALY, List.of("REPLY_USER"),
                NotificationConstants.Module.APPROVAL, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.APPROVAL_COMMENT_REPALY),"APPROVAL");
        // 评论@
        addTaskAndConfig(List.of(TemplateEnum.APPROVAL_COMMENT_MENTIONED_TEXT.name()), eventNameMap,
                NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED, List.of("MENTION_USER"),
                NotificationConstants.Module.APPROVAL, receiveMethod,
                TriggerTypeEnum.ACTION.name(),
                buildActionConfigValue(NotificationConstants.Event.APPROVAL_COMMENT_MENTIONED),"APPROVAL");

        //自定义表单
        List<CustomForm> customForms = customFormMapper.selectAll(null);
        Map<String, MessageTask> eventMaps = messageTasks.stream()
                .collect(Collectors.toMap(MessageTask::getEvent, Function.identity()));

        for (CustomForm customForm : customForms) {
            if (eventMaps.containsKey(customForm.getId())) {
                continue;
            }
            initCustomFormMessage(customForm, InternalUser.ADMIN.getValue(), "100001");
        }
    }

    /**
     * 自定义表单默认消息通知初始化：新建/编辑/删除/审批
     */
    public void initCustomFormMessage(CustomForm customForm, String userId, String orgId) {
        String receiveMethod = JSON.toJSONString(List.of(ReceiveMethodEnum.SYSTEM.name(),
                ReceiveMethodEnum.WECOM.name(), ReceiveMethodEnum.DINGTALK.name(), ReceiveMethodEnum.LARK.name()));
        // 动作配置值与运行时发送的 action 保持一致(均为 event: create_<formId> 等), 否则 matchTrigger 精确比较无法命中
        //新建
        buildCustomFormMessage(List.of(TemplateEnum.CUSTOM_ADD_TEXT.name()), customForm,
                "create", List.of("OWNER"), receiveMethod, orgId, userId,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue("create_" + customForm.getId()));
        //编辑
        buildCustomFormMessage(List.of(TemplateEnum.CUSTOM_UPDATE_TEXT.name()), customForm,
                "update", List.of("OWNER"), receiveMethod, orgId, userId,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue("update_" + customForm.getId()));
        //删除
        buildCustomFormMessage(List.of(TemplateEnum.CUSTOM_DELETED_TEXT.name()), customForm,
                "delete", List.of("OLD_OWNER"), receiveMethod, orgId, userId,
                TriggerTypeEnum.ACTION.name(), buildActionConfigValue("delete_" + customForm.getId()));
        //审批
        buildCustomFormMessage(List.of(TemplateEnum.CUSTOM_APPROVAL_TEXT.name()), customForm,
                "approve", List.of("SUBMITTER", "OWNER"), receiveMethod, orgId, userId,
                TriggerTypeEnum.FIELD.name(),
                buildFieldConfigValue(buildCondition("approvalStatus", List.of("APPROVED", "UNAPPROVED"), true, "IN",
                        "SELECT_MULTIPLE")));
    }

    /**
     * 自定义表单 初始化 消息
     */
    public void buildCustomFormMessage(List<String> templates, CustomForm customForm, String event,
            List<String> owners,
            String receiveMethod, String organizationId, String userId, String triggerType, String value) {
        String taskId = IDGenerator.nextStr();
        MessageTask messageTask = new MessageTask();
        messageTask.setId(taskId);
        messageTask.setName(Translator.get("message." + event, Locale.CHINA) + customForm.getName());
        messageTask.setEnable(true);
        messageTask.setSysEnable(true);
        messageTask.setScopeId(JSON.toJSONString(owners));
        messageTask.setOrganizationId(organizationId);
        messageTask.setReceiveMethod(receiveMethod);
        messageTask.setTaskType(customForm.getId());
        messageTask.setType("CUSTOM");
        messageTask.setEvent(event + "_" + customForm.getId());
        messageTask.setCreateUser(userId);
        messageTask.setCreateTime(System.currentTimeMillis());
        messageTask.setUpdateUser(userId);
        messageTask.setUpdateTime(System.currentTimeMillis());
        messageTask.setTemplate(JSON.toJSONString(templates));
        messageTask.setFormType(customForm.getId());
        messageTaskMapper.insert(messageTask);

        MessageTaskConfig messageTaskConfig = new MessageTaskConfig();
        messageTaskConfig.setId(IDGenerator.nextStr());
        messageTaskConfig.setMessageTaskId(taskId);
        messageTaskConfig.setEvent(event + "_" + customForm.getId());
        messageTaskConfig.setOrganizationId(organizationId);
        messageTaskConfig.setTriggerType(triggerType);
        messageTaskConfig.setValue(value);
        messageTaskConfigMapper.insert(messageTaskConfig);
    }

    /**
     * 保存任务及初始化配置
     *
     * @param templates     模板枚举名称
     * @param eventNameMap  事件与任务名称的映射
     * @param event         通知事件类型（新建客户、更新客户等）
     * @param owners        接收人
     * @param taskType      任务类型（客户、商机等）
     * @param receiveMethod 接收方式
     * @param triggerType   触发类型（动作、定时、字段）
     * @param value         通知配置值
     */
    private void addTaskAndConfig(List<String> templates, Map<String, String> eventNameMap, String event,
            List<String> owners, String taskType,
            String receiveMethod, String triggerType, String value,String formType) {
        String taskId = IDGenerator.nextStr();
        MessageTask messageTask = new MessageTask();
        messageTask.setId(taskId);
        messageTask.setName(eventNameMap.get(event));
        messageTask.setEnable(true);
        messageTask.setSysEnable(true);
        messageTask.setScopeId(JSON.toJSONString(owners));
        messageTask.setOrganizationId("100001");
        messageTask.setReceiveMethod(receiveMethod);
        messageTask.setTaskType(taskType);
        messageTask.setType("SYSTEM");
        messageTask.setEvent(event);
        messageTask.setCreateUser(InternalUser.ADMIN.getValue());
        messageTask.setCreateTime(System.currentTimeMillis());
        messageTask.setUpdateUser(InternalUser.ADMIN.getValue());
        messageTask.setUpdateTime(System.currentTimeMillis());
        messageTask.setTemplate(JSON.toJSONString(templates));
        messageTask.setFormType(formType);
        messageTaskMapper.insert(messageTask);

        MessageTaskConfig messageTaskConfig = new MessageTaskConfig();
        messageTaskConfig.setId(IDGenerator.nextStr());
        messageTaskConfig.setMessageTaskId(taskId);
        messageTaskConfig.setEvent(event);
        messageTaskConfig.setOrganizationId("100001");
        messageTaskConfig.setTriggerType(triggerType);
        messageTaskConfig.setValue(value);
        messageTaskConfigMapper.insert(messageTaskConfig);
    }

    /**
     * 更新任务并保存任务配置
     *
     * @param templates     模板枚举名称
     * @param eventNameMap  任务名称
     * @param messageTask   任务
     * @param owners        接收人
     * @param receiveMethod 接收方式（站内、邮件、企微等）
     * @param triggerType   触发类型（动作、定时、条件）
     * @param value         通知配置值
     */
    private void updateTaskAndAddConfig(List<String> templates, Map<String, String> eventNameMap,
            MessageTask messageTask, List<String> owners,
            String receiveMethod, String triggerType, String value, String formType) {
        messageTask.setName(eventNameMap.get(messageTask.getEvent()));
        messageTask.setEnable(true);
        messageTask.setType("SYSTEM");
        messageTask.setScopeId(JSON.toJSONString(owners));
        messageTask.setReceiveMethod(receiveMethod);
        messageTask.setTemplate(JSON.toJSONString(templates));
        messageTask.setFormType(formType);
        messageTaskMapper.update(messageTask);

        MessageTaskConfig config = new MessageTaskConfig();
        config.setId(IDGenerator.nextStr());
        config.setMessageTaskId(messageTask.getId());
        config.setEvent(messageTask.getEvent());
        config.setOrganizationId(messageTask.getOrganizationId());
        config.setTriggerType(triggerType);
        config.setValue(value);
        messageTaskConfigMapper.insert(config);
    }

    /**
     * 更新任务及任务配置
     *
     * @param templates     模板枚举名称
     * @param config        消息通知配置
     * @param eventNameMap  任务名称
     * @param messageTask   任务
     * @param owners        接收人
     * @param receiveMethod 接收方式（站内、邮件、企微等）
     * @param triggerType   触发类型（动作、定时、条件）
     * @param value         通知配置值
     */
    private void updateTaskAndUpdateConfig(List<String> templates, MessageTaskConfig config,
            Map<String, String> eventNameMap,
            MessageTask messageTask, List<String> owners, String receiveMethod, String triggerType, String value,String formType) {
        messageTask.setName(eventNameMap.get(messageTask.getEvent()));
        messageTask.setEnable(true);
        messageTask.setType("SYSTEM");
        messageTask.setScopeId(JSON.toJSONString(owners));
        messageTask.setReceiveMethod(receiveMethod);
        messageTask.setTemplate(JSON.toJSONString(templates));
        messageTask.setFormType(formType);
        messageTaskMapper.update(messageTask);

        config.setMessageTaskId(messageTask.getId());
        config.setTriggerType(triggerType);
        config.setValue(value);
        messageTaskConfigMapper.update(config);
    }

    /**
     * 动作触发：value 配置值
     *
     * @param action 依据动作
     */
    private String buildActionConfigValue(String action) {
        TriggerTypeConfigDTO triggerTypeConfigDTO = new TriggerTypeConfigDTO();
        triggerTypeConfigDTO.setActionTypeConfig(action);
        return JSON.toJSONString(triggerTypeConfigDTO);
    }

    /**
     * 字段条件：value 配置值
     */
    private String buildFieldConfigValue(List<FilterCondition> filterConditions) {
        TriggerTypeConfigDTO triggerTypeConfigDTO = new TriggerTypeConfigDTO();
        triggerTypeConfigDTO.setFieldTypeConfig(filterConditions);
        return JSON.toJSONString(triggerTypeConfigDTO);
    }

    /**
     * 定时提醒：value 配置值
     */
    private String buildScheduleConfigValue(ScheduleTypeConfig scheduleTypeConfig) {
        TriggerTypeConfigDTO triggerTypeConfigDTO = new TriggerTypeConfigDTO();
        triggerTypeConfigDTO.setScheduleTypeConfig(scheduleTypeConfig);
        return JSON.toJSONString(triggerTypeConfigDTO);
    }

    /**
     * 构建字段条件
     */
    private List<FilterCondition> buildCondition(String name, Object value, boolean multipleValue, String operator,
            String type) {
        List<FilterCondition> filterConditions = new ArrayList<>();
        FilterCondition filterCondition = new FilterCondition();
        filterCondition.setName(name);
        filterCondition.setValue(value);
        filterCondition.setMultipleValue(multipleValue);
        filterCondition.setOperator(operator);
        filterCondition.setType(type);
        filterConditions.add(filterCondition);
        return filterConditions;
    }

    /**
     * 构建定时条件
     */
    private ScheduleTypeConfig buildScheduleValue(String fieldId, String day, String time, String remindType) {
        ScheduleTypeConfig config = new ScheduleTypeConfig();
        config.setFieldId(fieldId);
        config.setDay(day);
        config.setTime(time);
        config.setRemindType(remindType);
        return config;
    }

    /**
     * 启用/禁用
     */
    @OperationLog(module = LogModule.SYSTEM_MESSAGE_MESSAGE, type = LogType.UPDATE, resourceId = "{#id}")
    public void enable(String id, String userId, String orgId) {
        MessageTask messageTask = messageTaskMapper.selectByPrimaryKey(id);
        if (messageTask != null) {
            MessageTask task = new MessageTask();
            task.setId(id);
            task.setUpdateUser(userId);
            task.setUpdateTime(System.currentTimeMillis());
            task.setEnable(!messageTask.getEnable());
            messageTaskMapper.update(task);

            Map<String, Object> oldMap = new HashMap<>();
            oldMap.put("enable", Translator.get("log.enable." + messageTask.getEnable()));
            Map<String, Object> newMap = new HashMap<>();
            newMap.put("enable", Translator.get("log.enable." + !messageTask.getEnable()));
            OperationLogContext.setContext(
                    LogContextInfo.builder()
                            .resourceName(messageTask.getName())
                            .originalValue(oldMap)
                            .modifiedValue(newMap)
                            .build());
        }
    }

    /**
     * 批量更新接收方式
     */
    public void batchUpdate(MessageBatchRequest request, String userId, String orgId) {
        if (CollectionUtils.isEmpty(request.getIds()) || StringUtils.isBlank(request.getReceiveMethod())) {
            return;
        }
        List<MessageTask> oldMessageTasks = messageTaskMapper.selectByIds(request.getIds());
        List<LogDTO> logs = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (MessageTask oldMessageTask : oldMessageTasks) {
            MessageTask task = BeanUtils.copyBean(new MessageTask(), oldMessageTask);
            task.setReceiveMethod(request.getReceiveMethod());
            task.setUpdateUser(userId);
            task.setUpdateTime(now);
            messageTaskMapper.update(task);
            LogDTO logDTO = new LogDTO(orgId, task.getId(), userId, LogType.UPDATE, LogModule.SYSTEM_MESSAGE_MESSAGE,
                    task.getName());
            logDTO.setOriginalValue(oldMessageTask);
            logDTO.setModifiedValue(task);
            logs.add(logDTO);
        }
        logService.batchAdd(logs);
    }
}
