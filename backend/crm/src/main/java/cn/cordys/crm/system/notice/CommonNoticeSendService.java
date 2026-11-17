package cn.cordys.crm.system.notice;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.message.ReceiveMethodEnum;
import cn.cordys.crm.system.constants.message.ScopeIdTypeEnum;
import cn.cordys.crm.system.constants.message.TemplateEnum;
import cn.cordys.crm.system.constants.message.TriggerTypeEnum;
import cn.cordys.crm.system.domain.DepartmentCommander;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.MessageTaskConfig;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.dto.MessageTaskConfigDTO;
import cn.cordys.crm.system.dto.message.TriggerTypeConfigDTO;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.crm.system.utils.MessageTemplateUtils;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CommonNoticeSendService {
    @Resource
    private NoticeSendService noticeSendService;
    @Resource
    private BaseMapper<User> userBaseMapper;
    @Resource
    private BaseMapper<DepartmentCommander> departmentCommanderMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<MessageTask> messageTaskMapper;
    @Resource
    private BaseMapper<MessageTaskConfig> messageTaskConfigMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private BaseMapper<CustomerCollaboration> customerCollaborationMapper;
    @Resource
    private BaseMapper<CustomerPool> customerPoolMapper;
    @Resource
    private BaseMapper<CluePool> cluePoolMapper;
    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;

    private static final int RANGE_SIZE = 2;
    private static final String OWNER_FIELD = "owner";
    private static final String POOL_ID_FIELD = "poolId";
    private static final String ID_FIELD = "id";
    private static final String NAME_FIELD = "name";

    private static void setLanguage(String language) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        if (Strings.CI.contains(language, "US")) {
            locale = Locale.US;
        } else if (Strings.CI.contains(language, "TW")) {
            locale = Locale.TAIWAN;
        }
        LocaleContextHolder.setLocale(locale);
    }

    @Async("threadPoolTaskExecutor")
    public void sendNotice(String module, String event, List<Map> resources, String userId,
            String currentOrganizationId) {
        User operator = userBaseMapper.selectByPrimaryKey(userId);
        setLanguage(operator.getLanguage());
        // 有批量操作发送多次
        for (Map resource : resources) {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
            paramMap.put("Language", operator.getLanguage());
            paramMap.putAll(resource);
            paramMap.putIfAbsent("organizationId", currentOrganizationId);

            String context = getContext(event);
            List<String> relatedUsers = getRelatedUsers(resource.get("relatedUsers"));
            List<Receiver> receivers = getReceivers(relatedUsers);
            NoticeModel noticeModel = NoticeModel.builder()
                    .operator(operator.getId())
                    .context(context)
                    .paramMap(paramMap)
                    .event(event)
                    .status((String) paramMap.get("status"))
                    .excludeSelf(true)
                    .receivers(receivers)
                    .build();
            noticeSendService.send(module, noticeModel);
        }
    }

    private List<Receiver> getReceivers(List<String> relatedUsers) {
        List<Receiver> receivers = new ArrayList<>();
        for (String relatedUserId : relatedUsers) {
            receivers.add(new Receiver(relatedUserId, NotificationConstants.Type.SYSTEM_NOTICE.name()));
        }
        return receivers;
    }

    /**
     * 发送通知
     *
     * @param taskType     发送类型
     * @param event        发送事件
     * @param resourceName 资源名称
     * @param currentOrgId 当前组织id
     * @param users        需要通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, String resourceName, String operatorId, String currentOrgId,
            List<String> users, boolean excludeSelf) {
        sendNotice(taskType, event, Map.of("name", resourceName), operatorId, currentOrgId, users, excludeSelf);
    }

    /**
     * 发送通知
     *
     * @param taskType     发送类型
     * @param event        发送事件
     * @param resource     消息变量的名称 以及其他变量 eg: xxxx 的名称，resource.put("name", "名称");
     * @param currentOrgId 当前组织id
     * @param users        需要通知的用户
     * @param excludeSelf  是否排除自己
     */
    public void sendNotice(String taskType, String event, Map<String, Object> resource, String operatorId,
            String currentOrgId,
            List<String> users, boolean excludeSelf) {
        Map<String, Object> paramMap = new HashMap<>();
        User operator = userBaseMapper.selectByPrimaryKey(operatorId);
        if (operator != null) {
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
            paramMap.put("Language", operator.getLanguage());
        } else {
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, Translator.get("setting"));
            paramMap.put("Language", "zh-CN");
        }

        paramMap.putAll(resource);
        paramMap.putIfAbsent("organizationId", currentOrgId);

        String context = getContext(event);

        List<Receiver> receivers = getReceivers(users);
        NoticeModel noticeModel = NoticeModel.builder()
                .operator(operatorId)
                .context(context)
                .paramMap(paramMap)
                .event(event)
                .status((String) paramMap.get("status"))
                .excludeSelf(excludeSelf)
                .receivers(receivers)
                .build();
        noticeSendService.sendOther(taskType, noticeModel, excludeSelf);
    }

    private List<String> getRelatedUsers(Object relatedUsers) {
        String relatedUser = (String) relatedUsers;
        List<String> relatedUserList = new ArrayList<>();
        if (StringUtils.isNotBlank(relatedUser)) {
            relatedUserList = Arrays.asList(relatedUser.split(";"));
        }
        return relatedUserList;
    }

    private String getContext(String event) {
        if (StringUtils.isBlank(event)) {
            return null;
        }
        Map<String, String> defaultTemplateMap = MessageTemplateUtils.getDefaultTemplateMap();
        return defaultTemplateMap.get(event + "_TEXT");
    }

    /**
     * 获取消息接收用户ID列表
     *
     * @param messageTaskConfigDTO 消息任务配置DTO
     * @param createUser           创建人ID
     * @param owner                责任人ID
     * @param orgId                组织ID
     * @return 消息接收用户ID列表
     */
    public List<String> getNoticeReceiveUserIds(MessageTaskConfigDTO messageTaskConfigDTO, String createUser,
            String owner, String orgId) {
        List<String> receiveUserIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(messageTaskConfigDTO.getUserIds())) {
            if (messageTaskConfigDTO.getUserIds().contains(NotificationConstants.RelatedUser.CREATE_USER)) {
                receiveUserIds.add(createUser);
            }
            if (messageTaskConfigDTO.getUserIds().contains(NotificationConstants.RelatedUser.OWNER)) {
                receiveUserIds.add(owner);
            }
            List<String> normalUserIds = messageTaskConfigDTO.getUserIds().stream()
                    .filter(userId -> !userId.equals(NotificationConstants.RelatedUser.CREATE_USER)
                            && !userId.equals(NotificationConstants.RelatedUser.OWNER))
                    .toList();
            receiveUserIds.addAll(normalUserIds);
        }
        if (messageTaskConfigDTO.isRoleEnable() && CollectionUtils.isNotEmpty(messageTaskConfigDTO.getRoleIds())) {
            receiveUserIds.addAll(extUserMapper.selectUserIdsByRoleIds(messageTaskConfigDTO.getRoleIds()));
        }

        if (!messageTaskConfigDTO.isOwnerEnable()) {
            return receiveUserIds;
        }

        if (StringUtils.isBlank(orgId)) {
            return receiveUserIds;
        }

        String departmentId = extOrganizationUserMapper.getDepartmentByUserId(owner);

        if (StringUtils.isBlank(departmentId)) {
            return receiveUserIds;
        }

        List<String> departmentIds = new ArrayList<>();
        departmentIds.add(departmentId);
        int ownerLevel = messageTaskConfigDTO.getOwnerLevel();

        if (ownerLevel > 0) {
            String currentDepartmentId = departmentId;
            for (int i = 1; i < ownerLevel; i++) {
                String parentId = extDepartmentMapper.getParentIdById(currentDepartmentId);
                if (StringUtils.isBlank(parentId)) {
                    break;
                }
                departmentIds.add(parentId);
                currentDepartmentId = parentId;
            }
        }
        if (CollectionUtils.isEmpty(departmentIds)) {
            return receiveUserIds;
        }
        List<DepartmentCommander> departmentCommanderList = departmentCommanderMapper
                .selectListByLambda(new LambdaQueryWrapper<DepartmentCommander>()
                        .in(DepartmentCommander::getDepartmentId, departmentIds));
        if (CollectionUtils.isEmpty(departmentCommanderList)) {
            return receiveUserIds;
        }
        List<String> deptCommanderUserIds = departmentCommanderList.stream()
                .map(DepartmentCommander::getUserId)
                .toList();
        receiveUserIds.addAll(deptCommanderUserIds);

        return receiveUserIds.stream().distinct().toList();
    }

    /**
     * 新版通知：按任务类型取出全部启用的消息任务，逐一判断本次操作是否触发后发送。
     * <p>
     * 动作触发(ACTION)：任务配置的动作与本次动作一致时发送；
     * 字段触发(FIELD)：更新后命中条件且更新前不命中时发送；
     * 定时提醒(SCHEDULE)：不处理。
     *
     * @param context 发送上下文
     */
    public void sendNoticeByConfig(NoticeSendContext context) {
        if (context == null || StringUtils.isBlank(context.getTaskType())
                || StringUtils.isBlank(context.getOrganizationId())) {
            return;
        }
        try {
            LambdaQueryWrapper<MessageTask> taskQuery = new LambdaQueryWrapper<MessageTask>()
                    .eq(MessageTask::getTaskType, context.getTaskType())
                    .eq(MessageTask::getOrganizationId, context.getOrganizationId())
                    .eq(MessageTask::getEnable, true);
            // 仅按本次事件发送：直接过滤出 event 与本次动作一致的任务
            if (context.isEventOnly()) {
                if (StringUtils.isBlank(context.getAction())) {
                    return;
                }
                taskQuery.eq(MessageTask::getEvent, context.getAction());
            }
            List<MessageTask> tasks = messageTaskMapper.selectListByLambda(taskQuery);
            if (CollectionUtils.isEmpty(tasks)) {
                return;
            }

            // 事件直发：不再比较触发配置，命中即发送
            if (context.isEventOnly()) {
                tasks.forEach(task -> sendTaskNotice(task, context));
                return;
            }

            List<String> taskIds = tasks.stream().map(MessageTask::getId).toList();
            Map<String, MessageTaskConfig> configMap = messageTaskConfigMapper.selectListByLambda(
                            new LambdaQueryWrapper<MessageTaskConfig>()
                                    .in(MessageTaskConfig::getMessageTaskId, taskIds))
                    .stream()
                    .collect(Collectors.toMap(MessageTaskConfig::getMessageTaskId, Function.identity(), (a, b) -> a));

            for (MessageTask task : tasks) {
                MessageTaskConfig config = configMap.get(task.getId());
                if (config != null && matchTrigger(config, context)) {
                    sendTaskNotice(task, context);
                }
            }
        } catch (Exception e) {
            log.error("sendNoticeNew error, taskType: {}, action: {}", context.getTaskType(), context.getAction(), e);
        }
    }

    /**
     * 通用发送入口：业务方只传新旧实体和自定义字段值，内部据此构建上下文。
     * 新增传 newRecord/newFields、删除传 oldRecord/oldFields，编辑两者都传。
     *
     * @param eventOnly 是否仅按传入的 event 发送：true 只发送与 action(event) 一致的任务，
     *                  不再比较所有触发配置；false 比较所有触发配置后发送
     */
    public void sendNoticeByConfig(String taskType, String action, String operatorId, String organizationId,
            boolean excludeSelf, boolean eventOnly,
            Object oldRecord, List<BaseModuleFieldValue> oldFields,
            Object newRecord, List<BaseModuleFieldValue> newFields) {
        Map<String, Object> oldValues = buildFieldValues(oldRecord, oldFields);
        Map<String, Object> newValues = buildFieldValues(newRecord, newFields);
        Map<String, Object> resourceValues = newRecord != null ? newValues : oldValues;
        if (resourceValues.isEmpty()) {
            return;
        }
        sendNoticeByConfig(NoticeSendContext.builder()
                .taskType(taskType)
                .action(action)
                .operatorId(operatorId)
                .organizationId(organizationId)
                .resourceId(getStringValue(resourceValues, ID_FIELD))
                .resourceName(getStringValue(resourceValues, NAME_FIELD))
                .oldValues(oldValues)
                .newValues(newValues)
                .excludeSelf(excludeSelf)
                .eventOnly(eventOnly)
                .build());
    }

    private Map<String, Object> buildFieldValues(Object entity, List<BaseModuleFieldValue> moduleFields) {
        Map<String, Object> values = new HashMap<>();
        if (entity != null) {
            for (Field field : FieldUtils.getAllFields(entity.getClass())) {
                try {
                    values.put(field.getName(), FieldUtils.readField(field, entity, true));
                } catch (IllegalAccessException e) {
                    log.warn("read field {} failed", field.getName(), e);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(moduleFields)) {
            moduleFields.forEach(fieldValue -> values.put(fieldValue.getFieldId(), fieldValue.getFieldValue()));
        }
        return values;
    }

    /**
     * 判断任务配置是否被本次操作触发
     */
    private boolean matchTrigger(MessageTaskConfig config, NoticeSendContext context) {
        if (Strings.CI.equals(config.getTriggerType(), TriggerTypeEnum.SCHEDULE.name())) {
            return false;
        }
        TriggerTypeConfigDTO triggerConfig = JSON.parseObject(config.getValue(), TriggerTypeConfigDTO.class);
        if (triggerConfig == null) {
            return false;
        }
        if (Strings.CI.equals(config.getTriggerType(), TriggerTypeEnum.ACTION.name())) {
            return StringUtils.isNotBlank(context.getAction())
                    && Strings.CS.equals(triggerConfig.getActionTypeConfig(), context.getAction());
        }
        if (Strings.CI.equals(config.getTriggerType(), TriggerTypeEnum.FIELD.name())) {
            // 字段触发需要比较新旧值，只有改动类操作(更新/转移等)才有；新增、删除不参与字段条件
            Map<String, Object> oldValues = context.getOldValues();
            Map<String, Object> newValues = context.getNewValues();
            if (oldValues == null || oldValues.isEmpty() || newValues == null || newValues.isEmpty()) {
                return false;
            }
            List<FilterCondition> conditions = triggerConfig.getFieldTypeConfig();
            return CollectionUtils.isNotEmpty(conditions)
                    && matchConditions(conditions, newValues)
                    && !matchConditions(conditions, oldValues);
        }
        return false;
    }

    private void sendTaskNotice(MessageTask task, NoticeSendContext context) {
        List<ReceiverGroup> receiverGroups = resolveReceiverGroups(task.getScopeId(), context);
        if (CollectionUtils.isEmpty(receiverGroups)) {
            return;
        }

        User operator = userBaseMapper.selectByPrimaryKey(context.getOperatorId());
        Map<String, Object> paramMap = new HashMap<>();
        if (operator != null) {
            setLanguage(operator.getLanguage());
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, operator.getName());
            paramMap.put("Language", operator.getLanguage());
        } else {
            paramMap.put(NotificationConstants.RelatedUser.OPERATOR, Translator.get("setting"));
            paramMap.put("Language", "zh-CN");
        }
        if (context.getParamMap() != null) {
            paramMap.putAll(context.getParamMap());
        }
        if (StringUtils.isNotBlank(context.getResourceName())) {
            paramMap.putIfAbsent("name", context.getResourceName());
        }
        if (StringUtils.isNotBlank(context.getResourceId())) {
            paramMap.putIfAbsent("resourceId", context.getResourceId());
        }
        paramMap.putIfAbsent("organizationId", context.getOrganizationId());

        // 一个任务可能配置多条模板，如转移通知分别给原负责人、新负责人各一条；
        // 按接收人类型匹配各自模板，未匹配到类型的模板作为默认内容发给其余接收人
        List<String> templateNames = parseStringList(task.getTemplate());
        Map<ScopeIdTypeEnum, List<String>> templateByScope = groupTemplateByScope(templateNames);
        List<String> defaultTemplates = templateByScope.remove(null);

        for (ReceiverGroup group : receiverGroups) {
            List<String> groupTemplates = group.scopeType() != null && templateByScope.containsKey(group.scopeType())
                    ? templateByScope.get(group.scopeType())
                    : (CollectionUtils.isNotEmpty(defaultTemplates) ? defaultTemplates : templateNames);
            String content = buildContent(groupTemplates, paramMap);
            if (StringUtils.isBlank(content)) {
                continue;
            }
            sendTaskNoticeToReceivers(task, context, paramMap, content, group.receiverIds());
        }
    }

    private void sendTaskNoticeToReceivers(MessageTask task, NoticeSendContext context, Map<String, Object> paramMap,
            String content, List<String> receivers) {
        MessageDetailDTO messageDetail = new MessageDetailDTO();
        messageDetail.setId(task.getId());
        messageDetail.setEvent(task.getEvent());
        messageDetail.setTaskType(task.getTaskType());
        messageDetail.setOrganizationId(task.getOrganizationId());
        messageDetail.setTemplate(content);
        applyReceiveMethod(messageDetail, task.getReceiveMethod());

        NoticeModel noticeModel = NoticeModel.builder()
                .operator(context.getOperatorId())
                .paramMap(paramMap)
                .event(task.getEvent())
                .excludeSelf(context.isExcludeSelf())
                .receivers(getReceivers(receivers))
                .build();
        noticeSendService.sendNotificationAsync(messageDetail, noticeModel);
    }

    /**
     * 按接收人类型归类模板：模板名以 _接收人类型_TEXT 结尾时归到该类型(如 ..._OLD_OWNER_TEXT -> OLD_OWNER)，
     * 其余模板归到 null 作为默认内容
     */
    private Map<ScopeIdTypeEnum, List<String>> groupTemplateByScope(List<String> templateNames) {
        Map<ScopeIdTypeEnum, List<String>> templateByScope = new HashMap<>();
        for (String templateName : templateNames) {
            templateByScope.computeIfAbsent(matchScopeTypeByTemplate(templateName), k -> new ArrayList<>())
                    .add(templateName);
        }
        return templateByScope;
    }

    /**
     * 模板名匹配接收人类型，取匹配到的类型中名称最长者，避免 OWNER 误匹配 OLD_OWNER
     */
    private ScopeIdTypeEnum matchScopeTypeByTemplate(String templateName) {
        if (StringUtils.isBlank(templateName)) {
            return null;
        }
        ScopeIdTypeEnum matched = null;
        for (ScopeIdTypeEnum scopeType : ScopeIdTypeEnum.values()) {
            if (templateName.endsWith("_" + scopeType.name() + "_TEXT")
                    && (matched == null || scopeType.name().length() > matched.name().length())) {
                matched = scopeType;
            }
        }
        return matched;
    }

    /**
     * 解析接收人并按其类型分组：特殊接收人(ScopeIdTypeEnum)各自成组，其余(用户/角色/部门)合并为普通接收组
     */
    private List<ReceiverGroup> resolveReceiverGroups(String scopeIdJson, NoticeSendContext context) {
        List<String> scopeIds = parseStringList(scopeIdJson);
        if (CollectionUtils.isEmpty(scopeIds)) {
            return List.of();
        }
        Map<ScopeIdTypeEnum, List<String>> specialGroups = new LinkedHashMap<>();
        List<String> normalIds = new ArrayList<>();
        for (String scopeId : scopeIds) {
            ScopeIdTypeEnum scopeType = ScopeIdTypeEnum.get(scopeId);
            if (scopeType == null) {
                if (StringUtils.isNotBlank(scopeId)) {
                    normalIds.add(scopeId);
                }
                continue;
            }
            specialGroups.computeIfAbsent(scopeType, k -> new ArrayList<>())
                    .addAll(resolveSpecialReceivers(scopeType, context));
        }
        List<ReceiverGroup> groups = new ArrayList<>();
        specialGroups.forEach((scopeType, ids) -> {
            List<String> receivers = ids.stream().filter(StringUtils::isNotBlank).distinct().toList();
            if (CollectionUtils.isNotEmpty(receivers)) {
                groups.add(new ReceiverGroup(scopeType, receivers));
            }
        });
        if (CollectionUtils.isNotEmpty(normalIds)) {
            List<String> receivers = userExtendService.getScopeOwnerIds(normalIds, context.getOrganizationId())
                    .stream().filter(StringUtils::isNotBlank).distinct().toList();
            if (CollectionUtils.isNotEmpty(receivers)) {
                groups.add(new ReceiverGroup(null, receivers));
            }
        }
        return groups;
    }

    private record ReceiverGroup(ScopeIdTypeEnum scopeType, List<String> receiverIds) {
    }

    private List<String> resolveSpecialReceivers(ScopeIdTypeEnum scopeType, NoticeSendContext context) {
        String owner = getStringValue(context.getNewValues(), OWNER_FIELD);
        String oldOwner = getStringValue(context.getOldValues(), OWNER_FIELD);
        List<String> users = new ArrayList<>();
        switch (scopeType) {
            case OWNER -> addIfNotBlank(users, owner);
            case OLD_OWNER -> addIfNotBlank(users, oldOwner);
            case COMMANDER -> users.addAll(getDepartmentCommanders(owner));
            case SUPERVISOR -> users.addAll(getSupervisors(owner, context.getOrganizationId()));
            case COLLABORATION -> users.addAll(getCollaborationUserIds(context));
            case SUBMITTER -> users.addAll(getApprovalSubmitters(context.getResourceId()));
            case CUSTOMER_SEAS_OWNER -> addIfNotBlank(users, getPoolOwner(context, true));
            case CLUE_SEAS_OWNER -> addIfNotBlank(users, getPoolOwner(context, false));
            default -> {
            }
        }
        return users;
    }

    /**
     * 部门负责人：记录负责人所在部门的负责人
     */
    private List<String> getDepartmentCommanders(String owner) {
        if (StringUtils.isBlank(owner)) {
            return List.of();
        }
        String departmentId = extOrganizationUserMapper.getDepartmentByUserId(owner);
        if (StringUtils.isBlank(departmentId)) {
            return List.of();
        }
        return departmentCommanderMapper.selectListByLambda(new LambdaQueryWrapper<DepartmentCommander>()
                        .eq(DepartmentCommander::getDepartmentId, departmentId))
                .stream().map(DepartmentCommander::getUserId).filter(StringUtils::isNotBlank).toList();
    }

    /**
     * 直属上级：记录负责人在组织内的直属上级
     */
    private List<String> getSupervisors(String owner, String organizationId) {
        if (StringUtils.isBlank(owner)) {
            return List.of();
        }
        return extOrganizationUserMapper.selectUserByUserIds(List.of(owner)).stream()
                .filter(orgUser -> Strings.CS.equals(orgUser.getOrganizationId(), organizationId))
                .map(OrganizationUser::getSupervisorId)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    private List<String> getCollaborationUserIds(NoticeSendContext context) {
        if (!Strings.CS.equals(context.getTaskType(), NotificationConstants.Module.CUSTOMER)
                || StringUtils.isBlank(context.getResourceId())) {
            return List.of();
        }
        return customerCollaborationMapper.selectListByLambda(new LambdaQueryWrapper<CustomerCollaboration>()
                        .eq(CustomerCollaboration::getCustomerId, context.getResourceId()))
                .stream().map(CustomerCollaboration::getUserId).filter(StringUtils::isNotBlank).toList();
    }

    /**
     * 审批发起人：取该资源最近一次审批实例的提交人
     */
    private List<String> getApprovalSubmitters(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            return List.of();
        }
        ApprovalInstance instance = approvalInstanceMapper.selectListByLambda(
                        new LambdaQueryWrapper<ApprovalInstance>().eq(ApprovalInstance::getResourceId, resourceId))
                .stream()
                .max(Comparator.comparing(ApprovalInstance::getCreateTime, Comparator.nullsFirst(Long::compareTo)))
                .orElse(null);
        return instance == null || StringUtils.isBlank(instance.getSubmitterId())
                ? List.of() : List.of(instance.getSubmitterId());
    }

    /**
     * 公海管理员：优先取更新后的公海ID，取不到再取更新前的
     */
    private String getPoolOwner(NoticeSendContext context, boolean customerPool) {
        String poolId = getStringValue(context.getNewValues(), POOL_ID_FIELD);
        if (StringUtils.isBlank(poolId)) {
            poolId = getStringValue(context.getOldValues(), POOL_ID_FIELD);
        }
        if (StringUtils.isBlank(poolId)) {
            return null;
        }
        if (customerPool) {
            CustomerPool pool = customerPoolMapper.selectByPrimaryKey(poolId);
            return pool == null ? null : pool.getOwnerId();
        }
        CluePool pool = cluePoolMapper.selectByPrimaryKey(poolId);
        return pool == null ? null : pool.getOwnerId();
    }

    private String getStringValue(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value == null ? null : value.toString();
    }

    private void addIfNotBlank(List<String> receivers, Object userId) {
        if (userId != null && StringUtils.isNotBlank(userId.toString())) {
            receivers.add(userId.toString());
        }
    }

    private void applyReceiveMethod(MessageDetailDTO messageDetail, String receiveMethodJson) {
        Set<String> methods = new HashSet<>(parseStringList(receiveMethodJson));
        messageDetail.setSysEnable(methods.contains(ReceiveMethodEnum.SYSTEM.name()));
        messageDetail.setEmailEnable(methods.contains(ReceiveMethodEnum.EMAIL.name()));
        messageDetail.setWeComEnable(methods.contains(ReceiveMethodEnum.WECOM.name()));
        messageDetail.setDingTalkEnable(methods.contains(ReceiveMethodEnum.DINGTALK.name()));
        messageDetail.setLarkEnable(methods.contains(ReceiveMethodEnum.LARK.name()));
    }

    private String buildContent(String templateJson, Map<String, Object> paramMap) {
        return buildContent(parseStringList(templateJson), paramMap);
    }

    private String buildContent(List<String> templateNames, Map<String, Object> paramMap) {
        if (CollectionUtils.isEmpty(templateNames)) {
            return null;
        }
        String content = templateNames.stream()
                .map(name -> {
                    TemplateEnum templateEnum = TemplateEnum.get(name);
                    return templateEnum == null ? null
                            : MessageTemplateUtils.getContent(Translator.get(templateEnum.getDescription()), paramMap);
                })
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));
        return StringUtils.isBlank(content) ? null : content;
    }

    private List<String> parseStringList(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        List<String> list = JSON.parseArray(json, String.class);
        return list == null ? new ArrayList<>() : list;
    }

    private boolean matchConditions(List<FilterCondition> conditions, Map<String, Object> values) {
        List<FilterCondition> validConditions = conditions.stream().filter(FilterCondition::valid).toList();
        if (CollectionUtils.isEmpty(validConditions) || values == null) {
            return false;
        }
        return validConditions.stream().allMatch(condition -> matchCondition(condition, values));
    }

    private boolean matchCondition(FilterCondition condition, Map<String, Object> values) {
        Object actualValue = values.get(condition.getName());
        FilterCondition.CombineConditionOperator operator;
        try {
            operator = FilterCondition.CombineConditionOperator.valueOf(condition.getCombineOperator());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return matchValue(actualValue, condition.getCombineValue(), operator);
    }

    private boolean matchValue(Object actualValue, Object expectedValue,
            FilterCondition.CombineConditionOperator operator) {
        if (operator == FilterCondition.CombineConditionOperator.EMPTY) {
            return actualValue == null;
        }
        if (operator == FilterCondition.CombineConditionOperator.NOT_EMPTY) {
            return actualValue != null;
        }
        if (actualValue == null) {
            return false;
        }
        try {
            return switch (operator) {
                case EQUALS -> matchEquals(actualValue, expectedValue);
                case NOT_EQUALS -> !matchEquals(actualValue, expectedValue);
                case CONTAINS -> matchContains(actualValue, expectedValue);
                case NOT_CONTAINS -> !matchContains(actualValue, expectedValue);
                case IN -> matchIn(actualValue, expectedValue);
                case NOT_IN -> !matchIn(actualValue, expectedValue);
                case GT -> matchCompare(actualValue, expectedValue) > 0;
                case LT -> matchCompare(actualValue, expectedValue) < 0;
                case GE -> matchCompare(actualValue, expectedValue) >= 0;
                case LE -> matchCompare(actualValue, expectedValue) <= 0;
                case BETWEEN -> matchBetween(actualValue, expectedValue);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchEquals(Object actualValue, Object expectedValue) {
        if (actualValue instanceof List<?> actualList && expectedValue instanceof List<?> expectedList) {
            return actualList.equals(expectedList);
        }
        return Objects.equals(actualValue, expectedValue);
    }

    private boolean matchContains(Object actualValue, Object expectedValue) {
        return String.valueOf(actualValue).contains(String.valueOf(expectedValue));
    }

    private boolean matchIn(Object actualValue, Object expectedValue) {
        if (expectedValue instanceof List<?> expectedList) {
            if (actualValue instanceof List<?> actualList) {
                return actualList.stream().anyMatch(expectedList::contains);
            }
            return expectedList.contains(actualValue);
        }
        return false;
    }

    private int matchCompare(Object actualValue, Object expectedValue) {
        if (actualValue instanceof Number actualNum && expectedValue instanceof Number expectedNum) {
            return Double.compare(actualNum.doubleValue(), expectedNum.doubleValue());
        }
        try {
            double actual = Double.parseDouble(String.valueOf(actualValue));
            double expected = Double.parseDouble(String.valueOf(expectedValue));
            return Double.compare(actual, expected);
        } catch (NumberFormatException e) {
            return String.valueOf(actualValue).compareTo(String.valueOf(expectedValue));
        }
    }

    private boolean matchBetween(Object actualValue, Object expectedValue) {
        if (expectedValue instanceof List<?> rangeList && rangeList.size() == RANGE_SIZE) {
            Object min = rangeList.getFirst();
            Object max = rangeList.getLast();
            return matchCompare(actualValue, min) >= 0 && matchCompare(actualValue, max) <= 0;
        }
        return false;
    }

}
