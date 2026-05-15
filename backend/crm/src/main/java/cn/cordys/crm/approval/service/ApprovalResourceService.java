package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.ApprovalFlowVersion;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalResourceBaseParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.UserApprovalDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
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
	private ApplicationContext applicationContext;
	@Resource
	private ApprovalFlowService approvalFlowService;
	@Resource
	private ApprovalInstanceService instanceService;
	@Resource
	private ApprovalActionService approvalActionService;

    /**
     * 开启的审批流表单表格映射
     */
    public static final Map<String, String> FORM_APPROVAL_TABLE = new HashMap<>(4);

    static {
        FORM_APPROVAL_TABLE.put(FormKey.QUOTATION.getKey(), "opportunity_quotation");
        FORM_APPROVAL_TABLE.put(FormKey.CONTRACT.getKey(), "contract");
        FORM_APPROVAL_TABLE.put(FormKey.INVOICE.getKey(), "contract_invoice");
        FORM_APPROVAL_TABLE.put(FormKey.ORDER.getKey(), "sales_order");
    }

    public ResourceApprovalResponse resourceDetail(String resourceId) {
        // 初始化响应对象，默认返回空审核人列表。
        ResourceApprovalResponse response = new ResourceApprovalResponse();
        response.setResourceId(resourceId);
        response.setApproveUserList(Collections.emptyList());
        if (StringUtils.isBlank(resourceId)) {
            return response;
        }

        // 查询资源最近一次审批实例。
        LambdaQueryWrapper<ApprovalInstance> instanceWrapper = new LambdaQueryWrapper<>();
        instanceWrapper.eq(ApprovalInstance::getResourceId, resourceId)
                .orderByDesc(ApprovalInstance::getSubmitTime);
        List<ApprovalInstance> instances = approvalInstanceMapper.selectListByLambda(instanceWrapper);
        if (instances.isEmpty()) {
            return response;
        }

        // 设置审批状态并校验当前审批节点。
        ApprovalInstance latestInstance = instances.getFirst();
        response.setApproveStatus(latestInstance.getApprovalStatus());
        if (StringUtils.isBlank(latestInstance.getCurrentNodeId())) {
            return response;
        }

        // 只查询当前实例当前节点对应的审批任务。
        LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId())
                .eq(ApprovalTask::getNodeId, latestInstance.getCurrentNodeId());
        List<ApprovalTask> tasks = approvalTaskMapper.selectListByLambda(taskWrapper);
        if (tasks.isEmpty()) {
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
     */
    public void updateResourceApprovalStatus(FormKey formKey, String resourceId, String approvalStatus) {
		if (formKey == null) {
			throw new GenericException(Translator.get("module.form.illegal"));
		}
        String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal"));
        }
        extApprovalInstanceMapper.updateApprovalStatus(tableName, resourceId, approvalStatus);
		// 存在快照表, 需要同步刷新审批状态
		if (formKey.hasSnapshot()) {
			updateSnapshotApprovalStatus(formKey, resourceId, approvalStatus);
		}
    }

	/**
	 * 更新业务快照审批状态值
	 *
	 * @param formKey        表单类型
	 * @param resourceId     资源ID
	 * @param approvalStatus 审批状态
	 */
	private void updateSnapshotApprovalStatus(FormKey formKey, String resourceId, String approvalStatus) {
		// FormKey 与 Service Bean 名称的映射
		Map<FormKey, String> snapshotServiceMap = Map.of(
			FormKey.INVOICE, "contractInvoiceService",
			FormKey.QUOTATION, "opportunityQuotationService",
			FormKey.CONTRACT, "customerContactService"
		);

		String serviceBeanName = snapshotServiceMap.get(formKey);
		if (StringUtils.isBlank(serviceBeanName)) {
			return;
		}

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
     * 手动提审
     *
     * @param param 提审参数
     */
    public void push(ApprovalResourceBaseParam param, String currentOrgId, String currentUserId) {
		ApprovalFlowVersion approvalFlowVersion = approvalFlowService.getEnabledFlow(param.getFormKey(), currentOrgId);
		if (approvalFlowVersion == null) {
			throw new GenericException(Translator.get("approval_flow.not.exist"));
		}
		// 初始化审批实例
		ApprovalInstance instance = initInstance(approvalFlowVersion, param, currentUserId);
		// 获取第一个节点
		ApprovalNodeResponse firstApprovalNode = approvalFlowService.getResourceApprovalInstanceFirstNode(instance, currentOrgId);
		instance.setCurrentNodeId(firstApprovalNode.getId());
		if (ApprovalNodeTypeEnum.valueOf(firstApprovalNode.getNodeType()) == ApprovalNodeTypeEnum.EXCEPTION) {
			// 异常节点, 目前只有自动拒绝的场景, 直接驳回
			updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.UNAPPROVED.name());
			instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
			instance.setApprovalTime(System.currentTimeMillis());
			approvalInstanceMapper.insert(instance);
			return;
		}
		if (ApprovalNodeTypeEnum.valueOf(firstApprovalNode.getNodeType()) == ApprovalNodeTypeEnum.END) {
			// 直接结束
			updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.APPROVED.name());
			instance.setApprovalStatus(ApprovalStatus.APPROVED.name());
			instance.setApprovalTime(System.currentTimeMillis());
			approvalInstanceMapper.insert(instance);
			return;
		}
		/*
		 * 正常审批流程
		 * 1. 更新业务表审批状态为审批中
		 * 2. 创建审批实例
		 * 3. 创建审批待办任务
		 * 4. 抄送任务
		 */
		updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.APPROVING.name());
		approvalInstanceMapper.insert(instance);
		ApprovalNodeApproverResponse approverNode = (ApprovalNodeApproverResponse) firstApprovalNode;
		List<ApprovalTask> approvalTasks = approvalActionService.getNodeApproverTasks(approverNode, instance.getId(), currentUserId, null);
		List<ApprovalTask> ccTasks = approvalActionService.getNodeCcTasks(approverNode, instance.getId(), currentUserId);
		List<ApprovalTask> allTasks = ListUtils.union(approvalTasks, ccTasks);
		if (CollectionUtils.isNotEmpty(allTasks)) {
			approvalTaskMapper.batchInsert(allTasks);
		}
	}

	public void revoke(ApprovalResourceBaseParam param, String currentUserId) {
		// 更新业务资源审批状态
		updateResourceApprovalStatus(FormKey.ofKey(param.getFormKey()), param.getResourceId(), ApprovalStatus.REVOKED.name());
		// 更新审批实例状态
		ApprovalInstance instance = instanceService.getLatestInstance(param.getResourceId());
		instance.setApprovalStatus(ApprovalStatus.REVOKED.name());
		instance.setApprovalTime(System.currentTimeMillis());
		instance.setUpdateUser(currentUserId);
		instance.setUpdateTime(System.currentTimeMillis());
		approvalInstanceMapper.updateById(instance);
	}

	private ApprovalInstance initInstance(ApprovalFlowVersion flowVersion, ApprovalResourceBaseParam param, String currentUserId) {
		ApprovalInstance instance = new ApprovalInstance();
		instance.setId(IDGenerator.nextStr());
		instance.setFlowVersionId(flowVersion.getId());
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
}
