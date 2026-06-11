package cn.cordys.crm.approval.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.*;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.integration.common.utils.HttpClientUtils;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.field.PriceSubField;
import cn.cordys.crm.system.dto.field.ProductSubField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.UserApprovalDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ApprovalResourceService {

    @Resource
    private BaseMapper<ApprovalInstance> approvalInstanceMapper;
    @Resource
    private BaseMapper<ApprovalTask> approvalTaskMapper;
    @Resource
    private BaseMapper<ApprovalRecord> approvalRecordMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private ExtApprovalInstanceMapper extApprovalInstanceMapper;
    @Resource
    private ExtApprovalTaskMapper extApprovalTaskMapper;
    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private ApprovalFlowService approvalFlowService;
    @Resource
    private ApprovalInstanceService instanceService;
    @Resource
    private ApprovalActionService approvalActionService;
    @Resource
    private LogService logService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;

    /**
     * 开启的审批流表单表格映射
     */
    public static final Map<String, String> FORM_APPROVAL_TABLE = new HashMap<>(4);
    public static final Map<FormKey, String> FORM_SERVICE = new HashMap<>(4);
    public static final String NULL_POST_CONFIG = "null";

    static {
        FORM_APPROVAL_TABLE.put(FormKey.QUOTATION.getKey(), "opportunity_quotation");
        FORM_APPROVAL_TABLE.put(FormKey.CONTRACT.getKey(), "contract");
        FORM_APPROVAL_TABLE.put(FormKey.INVOICE.getKey(), "contract_invoice");
        FORM_APPROVAL_TABLE.put(FormKey.ORDER.getKey(), "sales_order");

        FORM_SERVICE.put(FormKey.QUOTATION, "opportunityQuotationService");
        FORM_SERVICE.put(FormKey.CONTRACT, "contractService");
        FORM_SERVICE.put(FormKey.INVOICE, "contractInvoiceService");
        FORM_SERVICE.put(FormKey.ORDER, "orderService");
        FORM_SERVICE.put(FormKey.ORDER, "orderService");
    }

    public ResourceApprovalResponse resourceDetail(String resourceId) {
        // 初始化响应对象，默认返回空审核人列表。
        ResourceApprovalResponse response = new ResourceApprovalResponse();
        response.setResourceId(resourceId);
        response.setApproveUserList(Collections.emptyList());
        if (StringUtils.isBlank(resourceId)) {
            return response;
        }

        // 查询资源最近一次审批实例（由SQL保证倒序并限制1条）。
        ApprovalInstance latestInstance = extApprovalTaskMapper.selectLatestInstanceByResourceId(resourceId);
        if (latestInstance == null) {
            return response;
        }

        // 设置审批状态并校验当前审批节点。
        response.setApproveStatus(latestInstance.getApprovalStatus());
        if (StringUtils.isBlank(latestInstance.getCurrentNodeId())) {
            return response;
        }

        // 只查询当前实例当前节点对应的审批任务。
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId())
                .eq(ApprovalTask::getStatus, ApprovalStatus.UNAPPROVED.name())
                .eq(ApprovalTask::getNodeId, latestInstance.getCurrentNodeId());
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        LambdaQueryWrapper<ApprovalRecord> autoRecordWrapper = new LambdaQueryWrapper<>();
        autoRecordWrapper.eq(ApprovalRecord::getInstanceId, latestInstance.getId())
                .eq(ApprovalRecord::getNodeId, latestInstance.getCurrentNodeId());
        List<ApprovalRecord> allRecords = approvalRecordMapper.selectListByLambda(autoRecordWrapper);
        List<ApprovalRecord> autoRecords = allRecords.stream().filter(record -> StringUtils.isBlank(record.getTaskId())
                && Strings.CI.equalsAny(record.getResult(), ApprovalStatus.AUTO_APPROVED.name(), ApprovalStatus.AUTO_UNAPPROVED.name())).toList();
        if (tasks.isEmpty() && autoRecords.isEmpty()) {
            return response;
        }

        // 批量加载审批人基础信息。
        List<String> approverIds = tasks.stream()
                .map(ApprovalTask::getApproverId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, User> userMap = approverIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByIds(approverIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (prev, next) -> prev));

        // 批量加载任务对应审批记录（用于审批意见）。
        List<String> taskIds = tasks.stream()
                .map(ApprovalTask::getId)
                .filter(StringUtils::isNotBlank)
                .toList();
        Map<String, ApprovalRecord> taskRecordMap = Collections.emptyMap();
        if (!taskIds.isEmpty()) {
            LambdaQueryWrapper<ApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.in(ApprovalRecord::getTaskId, taskIds)
                    .orderByAsc(ApprovalRecord::getCreateTime);
            List<ApprovalRecord> records = approvalRecordMapper.selectListByLambda(recordWrapper);
            taskRecordMap = records.stream()
                    .filter(record -> StringUtils.isNotBlank(record.getTaskId()))
                    .collect(Collectors.toMap(ApprovalRecord::getTaskId, Function.identity(), (prev, next) -> next));
        }

        // 组装审核人明细并按审批人去重返回。
        Map<String, UserApprovalDTO> approveUserMap = new LinkedHashMap<>();
        for (ApprovalTask task : tasks) {
            String approverId = task.getApproverId();
            if (StringUtils.isBlank(approverId)) {
                continue;
            }
            UserApprovalDTO userApprove = new UserApprovalDTO();
            userApprove.setId(approverId);
            User user = userMap.get(approverId);
            if (user != null) {
                userApprove.setName(user.getName());
                userApprove.setEmail(user.getEmail());
            }

            userApprove.setApproveResult(StringUtils.defaultIfBlank(task.getStatus(), ApprovalStatus.PENDING.name()));
            ApprovalRecord record = taskRecordMap.get(task.getId());
            if (record != null) {
                userApprove.setApproveReason(record.getComment());
            }
            approveUserMap.put(approverId, userApprove);
        }

        // 追加自动审批的节点
        for (ApprovalRecord autoRecord : autoRecords) {
            UserApprovalDTO userApprove = new UserApprovalDTO();
            userApprove.setId("Cbot");
            userApprove.setName("Cbot");
            userApprove.setApproveResult(autoRecord.getResult());
            approveUserMap.put("auto", userApprove);
        }

        // 回填最终审核人列表。
        response.setApproveUserList(new ArrayList<>(approveUserMap.values()));
        return response;
    }

    /**
     * 更新业务表及快照的审批状态
     *
     * @param formKey        表单类型
     * @param resourceId     资源ID
     * @param approvalStatus 审批状态
     * @param userId         操作人ID
     * @param orgId          组织ID
     */
    public void updateResourceApprovalStatus(FormKey formKey, String resourceId, String approvalStatus, String userId, String orgId) {
        if (formKey == null) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        // 查询旧的审批状态用于日志记录
        String oldApprovalStatus = extApprovalInstanceMapper.selectApprovalStatus(tableName, resourceId);
        if (Strings.CS.equals(approvalStatus, oldApprovalStatus)) {
            return;
        }
        extApprovalInstanceMapper.updateApprovalStatus(tableName, resourceId, approvalStatus);
        // 存在快照表, 需要同步刷新审批状态
        if (formKey.hasSnapshot()) {
            updateSnapshotApprovalStatus(formKey, resourceId, approvalStatus);
        }
        // 记录审批状态变更日志
        saveApprovalStatusLog(formKey, resourceId, oldApprovalStatus, approvalStatus, userId, orgId);
    }

    /**
     * 保存审批状态变更日志
     *
     * @param formKey           表单类型
     * @param resourceId        资源ID
     * @param oldApprovalStatus 旧审批状态
     * @param newApprovalStatus 新审批状态
     * @param userId            操作人ID
     * @param orgId             组织ID
     */
    private void saveApprovalStatusLog(FormKey formKey, String resourceId, String oldApprovalStatus, String newApprovalStatus, String userId, String orgId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(orgId)) {
            return;
        }
        String resourceName = getInstanceResourceName(formKey, resourceId);
        if (StringUtils.isBlank(resourceName)) {
            return;
        }
        String logModule = getLogModuleOfFormKey(formKey);
        if (StringUtils.isBlank(logModule)) {
            return;
        }
        LogDTO logDTO = new LogDTO(orgId, resourceId, userId, LogType.UPDATE, logModule, resourceName);
        Map<String, String> oldMap = new HashMap<>(1);
        oldMap.put("approvalStatus", StringUtils.defaultString(oldApprovalStatus));
        logDTO.setOriginalValue(oldMap);
        Map<String, String> newMap = new HashMap<>(1);
        newMap.put("approvalStatus", StringUtils.defaultString(newApprovalStatus));
        logDTO.setModifiedValue(newMap);
        logService.add(logDTO);
    }

    /**
     * 表单类型 => 日志模块
     *
     * @param formKey 表单Key
     * @return 日志模块
     */
    private String getLogModuleOfFormKey(FormKey formKey) {
        if (formKey == null) {
            return null;
        }
        return switch (formKey) {
            case QUOTATION -> LogModule.OPPORTUNITY_QUOTATION;
            case CONTRACT -> LogModule.CONTRACT_INDEX;
            case INVOICE -> LogModule.CONTRACT_INVOICE;
            case ORDER -> LogModule.ORDER_INDEX;
            default -> null;
        };
    }

    /**
     * 清除资源审批记录
     *
     * @param resourceId 资源ID
     */
    public void clearResourceApprovalDetail(String resourceId) {
        List<ApprovalInstance> approvalInstances = approvalInstanceMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalInstance>().eq(ApprovalInstance::getResourceId, resourceId));
        if (CollectionUtils.isNotEmpty(approvalInstances)) {
            List<String> instanceIds = approvalInstances.stream().map(ApprovalInstance::getId).toList();
            approvalTaskMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalTask>().in(ApprovalTask::getInstanceId, instanceIds));
            approvalRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalRecord>().in(ApprovalRecord::getInstanceId, instanceIds));
            approvalInstanceMapper.deleteByIds(instanceIds);
        }
    }

    /**
     * 获取审批实例资源名称
     *
     * @param formKey    表单类型
     * @param resourceId 资源ID
     */
    public String getInstanceResourceName(FormKey formKey, String resourceId) {
        if (formKey == null) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        return extApprovalInstanceMapper.selectBusinessName(tableName, resourceId);
    }

    /**
     * 更新业务快照审批状态值
     *
     * @param formKey        表单类型
     * @param resourceId     资源ID
     * @param approvalStatus 审批状态
     */
    private void updateSnapshotApprovalStatus(FormKey formKey, String resourceId, String approvalStatus) {
        if (formKey == null || !FORM_SERVICE.containsKey(formKey)) {
            return;
        }
        String serviceBeanName = FORM_SERVICE.get(formKey);
        Object service = applicationContext.getBean(serviceBeanName);
        try {
            Method method = service.getClass().getMethod("updateSnapshotApprovalStatus", ResourceSnapshotApprovalParam.class);
            ResourceSnapshotApprovalParam param = ResourceSnapshotApprovalParam.builder().resourceId(resourceId).approvalStatus(approvalStatus).build();
            method.invoke(service, param);
        } catch (Exception e) {
            log.error("更新业务数据快照失败", e);
        }
    }

	/**
	 * 审批后置字段更新
	 * @param formKey 表单Key
	 * @param resourceId 资源ID
	 * @param postConfig 后置配置
	 */
	public void updateApprovalPostField(FormKey formKey, String resourceId, String postConfig, String currentUserId) {
		if (formKey == null || !FORM_SERVICE.containsKey(formKey)) {
			return;
		}
		List<ResourceApprovalFieldUpdateParam> fields = getUpdateFieldOfPostConfig(postConfig);
		fields = fields.stream().filter(ResourceApprovalFieldUpdateParam::isEnable).filter(f -> f.getFieldValue() != null).toList();
		if (CollectionUtils.isEmpty(fields)) {
			return;
		}
		String serviceBeanName = FORM_SERVICE.get(formKey);
		Object service = applicationContext.getBean(serviceBeanName);
		try {
			Method method = service.getClass().getMethod("updateApprovalPostField", ResourceApprovalPostUpdateParam.class);
			ResourceApprovalPostUpdateParam postUpdateParam = ResourceApprovalPostUpdateParam.builder().fields(fields).resourceId(resourceId).operator(currentUserId).build();
			method.invoke(service, postUpdateParam);
		} catch (Exception e) {
			log.error("更新业务数据失败", e);
		}
	}

    /**
     * 获取审批流字段更新配置
     *
     * @param postConfig 配置
     * @return 更新配置
     */
    private List<ResourceApprovalFieldUpdateParam> getUpdateFieldOfPostConfig(String postConfig) {
        if (StringUtils.isBlank(postConfig) || Strings.CI.equals(NULL_POST_CONFIG, postConfig)) {
            return new ArrayList<>();
        }
        Map<String, Object> postConfigMap = JSON.parseToMap(postConfig);
        return JSON.parseArray(JSON.toJSONString(postConfigMap.get("fieldUpdateConfigs")), ResourceApprovalFieldUpdateParam.class);
    }

    /**
     * 手动提审
     *
     * @param param 提审参数
     */
    public void push(ApprovalResourceBaseParam param, String currentOrgId, String currentUserId) {
        ApprovalFlow approvalFlow = approvalFlowService.getEnabledFlow(param.getFormKey(), currentOrgId);
        if (approvalFlow == null) {
            throw new GenericException(Translator.get("approval_flow.not.exist"));
        }
        // 初始化审批实例
        ApprovalInstance instance = initInstance(approvalFlow, param, currentUserId);
        // 获取第一个节点
        ApprovalNodeResponse firstApprovalNode = approvalFlowService.getResourceApprovalInstanceFirstNode(instance, currentOrgId);
        instance.setCurrentNodeId(firstApprovalNode.getId());
        if (ApprovalNodeTypeEnum.valueOf(firstApprovalNode.getNodeType()) == ApprovalNodeTypeEnum.EXCEPTION) {
            // 异常节点, 目前只有自动拒绝的场景, 直接驳回
            updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.UNAPPROVED.name(), currentUserId, currentOrgId);
            instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
            instance.setApprovalTime(System.currentTimeMillis());
            approvalInstanceMapper.insert(instance);
            return;
        }
        if (ApprovalNodeTypeEnum.valueOf(firstApprovalNode.getNodeType()) == ApprovalNodeTypeEnum.END) {
            // 直接结束
            updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.APPROVED.name(), currentUserId, currentOrgId);
            instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
            instance.setApprovalTime(System.currentTimeMillis());
            approvalInstanceMapper.insert(instance);
            String resourceName = getInstanceResourceName(FormKey.ofKey(instance.getType()), instance.getResourceId());
            if (StringUtils.isBlank(resourceName)) {
                return;
            }
            approvalActionService.sendFinishNotice(instance, resourceName, currentUserId, currentOrgId);
            return;
        }
        /*
         * 正常审批流程
         * 1. 更新业务表审批状态为审批中
         * 2. 插入审批实例, 审批待办任务
         */
        updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.APPROVING.name(), currentUserId, currentOrgId);
        approvalInstanceMapper.insert(instance);
        ApprovalNodeApproverResponse approverNode = (ApprovalNodeApproverResponse) firstApprovalNode;
        approvalActionService.handlerNextNodeApproverTasks(approverNode, instance, null, currentUserId, null, currentOrgId);
    }

    /**
     * 撤回审批
     *
     * @param param         参数
     * @param currentUserId 当前用户ID
     * @param currentOrgId  当前组织ID
     */
    public void revoke(ApprovalResourceBaseParam param, String currentUserId, String currentOrgId) {
        // 更新业务资源审批状态
        updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.REVOKED.name(), currentUserId, currentOrgId);
        // 更新审批实例状态
        ApprovalInstance instance = instanceService.getLatestInstance(param.getResourceId());
        if (instance == null) {
            throw new GenericException(Translator.get("no.approval.instance"));
        }
        instance.setApprovalStatus(ApprovalStatus.REVOKED.name());
        instance.setApprovalTime(System.currentTimeMillis());
        instance.setUpdateUser(currentUserId);
        instance.setUpdateTime(System.currentTimeMillis());
        approvalInstanceMapper.updateById(instance);
        approvalActionService.loseCurrentNode(instance.getId(), instance.getCurrentNodeId());
    }

    /**
     * 初始化审批实例
     *
     * @param flow          审批流
     * @param param         参数
     * @param currentUserId 当前用户ID
     * @return 审批实例
     */
    private ApprovalInstance initInstance(ApprovalFlow flow, ApprovalResourceBaseParam param, String currentUserId) {
        ApprovalInstance instance = new ApprovalInstance();
        instance.setId(IDGenerator.nextStr());
        instance.setFlowVersionId(flow.getCurrentVersionId());
        instance.setType(param.getFormKey());
        instance.setApprovalStatus(ApprovalStatus.APPROVING.name());
        instance.setResourceId(param.getResourceId());
        instance.setSubmitterId(currentUserId);
        instance.setSubmitTime(System.currentTimeMillis());
        instance.setCreateUser(currentUserId);
        instance.setCreateTime(System.currentTimeMillis());
        instance.setUpdateUser(currentUserId);
        instance.setUpdateTime(System.currentTimeMillis());
        return instance;
    }

    /**
     * 批量编辑触发审批流
     * 用于批量编辑多个业务资源时，判断是否开启了编辑触发审批流，
     * 如果开启，则将所有资源的状态改为PENDING
     *
     * @param resourceIds    资源ID集合
     * @param formKey        业务模块（表单类型）
     * @param organizationId 组织ID
     * @param userId         操作人ID
     */
    public void batchEditTriggerApproval(List<String> resourceIds, FormKey formKey, String organizationId, String userId) {
        if (CollectionUtils.isEmpty(resourceIds) || formKey == null || StringUtils.isBlank(organizationId)) {
            return;
        }

        // 检查是否命中审批流
        if (!checkHitApprovalFlowEditTrigger(formKey, organizationId)) {
            return;
        }

        // 批量更新资源审批状态为待提审
        for (String resourceId : resourceIds) {
            clearResourceApprovalDetail(resourceId);
            updateResourceApprovalStatus(formKey, resourceId, ApprovalStatus.PENDING.name(), userId, organizationId);
        }
    }

    /**
     * 检查是否命中审批流触发时机
     *
     * @param formKey        表单类型
     * @param organizationId 组织ID
     * @return 是否命中审批流
     */
    private boolean checkHitApprovalFlowEditTrigger(FormKey formKey, String organizationId) {
        try {
            // 查询当前组织表单审批流配置
            ApprovalFlow flow = approvalFlowService.getEnabledFlow(formKey.getKey(), organizationId);
            if (flow == null) {
                return false;
            }
            return flow.getUpdateExecute();
        } catch (Exception e) {
            log.error("检查是否命中审批流失败, formKey:{}, error:{}", formKey, e.getMessage(), e);
            return false;
        }
    }


    /**
     * 异步发送webhook
     *
     * @param postConfig
     */
    @Async
    public void sendWebHook(String postConfig, List<BaseModuleFieldValue> fieldValues, ApprovalInstance instance) {
        ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(instance.getType(), OrganizationContext.getOrganizationId());
        ObjectMapper mapper = new ObjectMapper();
        WebHookConfig webHookConfig;
        try {
            webHookConfig = mapper.readTree(postConfig)
                    .get("webHookConfig")
                    .traverse(mapper)
                    .readValueAs(WebHookConfig.class);
        } catch (Exception e) {
            throw new GenericException(e.getMessage());
        }
        if (webHookConfig != null && webHookConfig.getWebHookEnable()) {
            switch (webHookConfig.getWebHookMethod()) {
                case "POST":
                    sendPost(webHookConfig, formConfig.getFields(), fieldValues, false);
                case "GET":
                    sendGet(webHookConfig, formConfig.getFields(), fieldValues, false);
                default:
                    break;
            }
        }
    }


    /**
     * 发送get 请求
     *
     * @param webHookConfig
     * @param moduleFields
     * @param fieldValues
     * @param isTest
     * @return
     */
    private HashMap<String, Object> sendGet(WebHookConfig webHookConfig, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues, Boolean isTest) {
        Map<String, String> headers = new HashMap<>();
        String url = "";
        try {
            if (StringUtils.isNotBlank(webHookConfig.getWebHookHeader())) {
                headers = JSON.parseMap(webHookConfig.getWebHookHeader());
            }
            if (!isTest) {
                url = getDataParse(webHookConfig, moduleFields, fieldValues);
            }
            log.info("GET请求原始参数：{}", webHookConfig.getWebHookUrl());
            log.info("GET请求解析参数：{}", url);
            String body = HttpClientUtils.sendGetRequest(StringUtils.isNotBlank(url) ? url : webHookConfig.getWebHookUrl(), headers);
            return JSON.parseObject(body, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("GET调用WebHook异常", e);
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 解析get请求 url参数/query参数
     *
     * @param webHookConfig
     * @param moduleFields
     * @param fieldValues
     * @return
     */
    private String getDataParse(WebHookConfig webHookConfig, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(webHookConfig.getWebHookUrl());
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object value = getFieldValueRecursive(
                    placeholder.split("\\."),
                    1,
                    moduleFields,
                    fieldValues
            );

            String param = URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(param));
        }

        matcher.appendTail(sb);
        return sb.toString();


    }


    /**
     * 发送post请求
     *
     * @param webHookConfig
     * @param moduleFields
     * @param fieldValues
     * @param isTest
     * @return
     */
    private HashMap<String, Object> sendPost(WebHookConfig webHookConfig, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues, Boolean isTest) {
        Map<String, String> headers = new HashMap<>();
        String queryBody = "";
        try {
            if (StringUtils.isNotBlank(webHookConfig.getWebHookHeader())) {
                headers = JSON.parseMap(webHookConfig.getWebHookHeader());
            }
            if (!isTest) {
                queryBody = postDataParse(webHookConfig, moduleFields, fieldValues);
            }
            log.info("POST请求原始参数：{}", webHookConfig.getWebHookBody());
            log.info("POST请求解析参数：{}", queryBody);
            String body = HttpClientUtils.sendPostRequest(webHookConfig.getWebHookUrl(), queryBody, headers);
            return JSON.parseObject(body, new TypeReference<HashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.error("POST调用WebHook异常：{}", e);
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * post请求体数据解析
     *
     * @param webHookConfig
     * @param moduleFields
     * @param fieldValues
     * @throws Exception
     */
    private String postDataParse(WebHookConfig webHookConfig, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues) throws Exception {
        String webHookBody = webHookConfig.getWebHookBody();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(webHookBody);
        replacePlaceholders(rootNode, moduleFields, fieldValues);
        String resultJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        return resultJson;
    }


    /**
     * 映射${aa.bb}具体值的类型
     *
     * @param node
     * @param moduleFields
     * @param fieldValues
     */
    private static void replacePlaceholders(JsonNode node, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode childNode = entry.getValue();
                if (childNode.isTextual()) {
                    String text = childNode.asText();
                    Object value = replaceText(text, moduleFields, fieldValues);
                    if (value instanceof Integer) {
                        objNode.put(entry.getKey(), (Integer) value);
                    } else if (value instanceof Long) {
                        objNode.put(entry.getKey(), (Long) value);
                    } else if (value instanceof Boolean) {
                        objNode.put(entry.getKey(), (Boolean) value);
                    } else {
                        objNode.put(entry.getKey(), String.valueOf(value));
                    }
                }
            }
        }
    }


    /**
     * 获取${aa.bb}中的具体值并替换
     *
     * @param text
     * @param moduleFields
     * @param fieldValues
     * @return
     */
    private static Object replaceText(String text, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String[] parts = placeholder.split("\\.");
            return getFieldValueRecursive(parts, 1, moduleFields, fieldValues);
        }
        return null;
    }


    /**
     * 递归获取${xx.xx}中的具体值
     *
     * @param parts
     * @param index
     * @param moduleFields
     * @param fieldValues
     * @return
     */
    private static Object getFieldValueRecursive(String[] parts, int index, List<BaseField> moduleFields, List<BaseModuleFieldValue> fieldValues) {
        if (index >= parts.length || moduleFields == null || moduleFields.isEmpty()) {
            return null;
        }
        String key = parts[index];
        // 找到当前层级的字段
        BaseField field = moduleFields.stream()
                .filter(f -> Strings.CI.equals(f.getName(), key))
                .findFirst()
                .orElse(null);

        if (field == null) {
            return null;
        }

        // 如果还有下一层，递归去 subFields
        if (index < parts.length - 1) {
            SubField subField = Strings.CI.equals(field.getType(), FieldType.SUB_PRICE.name()) ? (PriceSubField) field : (ProductSubField) field;
            BaseModuleFieldValue baseModuleFieldValue = fieldValues.stream().filter(baseField -> Strings.CI.equals(baseField.getFieldId(), subField.getId())).findFirst().orElse(null);
            return getFieldValueRecursiveSub(parts, index + 1, subField.getSubFields(), baseModuleFieldValue);
        } else {
            // 最后一层，取对应 fieldValue
            BaseModuleFieldValue moduleFieldValue;
            if (field.hasBusinessKey()) {
                moduleFieldValue = fieldValues.stream()
                        .filter(fv -> Strings.CI.equals(fv.getFieldId(), field.getBusinessKey()))
                        .findFirst().orElse(null);
            } else {
                moduleFieldValue = fieldValues.stream()
                        .filter(fv -> Strings.CI.equals(fv.getFieldId(), field.getId()))
                        .findFirst().orElse(null);
            }
            if (moduleFieldValue != null) {
                return moduleFieldValue.getFieldValue();
            }
        }

        return null;
    }

    private static Object getFieldValueRecursiveSub(String[] parts, int index, List<BaseField> subFields, BaseModuleFieldValue baseModuleFieldValue) {
        if (baseModuleFieldValue == null) {
            return null;
        }
        String key = parts[index];

        // 找到当前层级的字段
        BaseField field = subFields.stream()
                .filter(f -> Strings.CI.equals(f.getName(), key))
                .findFirst()
                .orElse(null);

        if (field == null) {
            return null;
        }

        if (baseModuleFieldValue.getFieldValue() instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    if (map.containsKey(field.getId())) {
                        Object result = map.get(field.getId());
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 测试连接
     *
     * @param webHookConfig
     */
    public void testConnect(WebHookConfig webHookConfig) {
        if (webHookConfig != null && webHookConfig.getWebHookEnable()) {
            HashMap<String, Object> resultObj = new HashMap<>();
            switch (webHookConfig.getWebHookMethod()) {
                case "POST":
                    resultObj = sendPost(webHookConfig, null, null, true);
                    if (!Strings.CI.equals(resultObj.get("code").toString(), "200")) {
                        throw new GenericException("测试连接不通过");
                    }
                case "GET":
                    resultObj = sendGet(webHookConfig, null, null, true);
                    if (!Strings.CI.equals(resultObj.get("code").toString(), "200")) {
                        throw new GenericException("测试连接不通过");
                    }
                default:
                    break;
            }
        }
    }
}
