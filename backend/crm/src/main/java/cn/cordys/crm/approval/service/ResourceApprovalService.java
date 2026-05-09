package cn.cordys.crm.approval.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.FieldSourceServiceProvider;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.ApprovalFlow;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalPushParam;
import cn.cordys.crm.approval.dto.response.ApprovalNodeApproverResponse;
import cn.cordys.crm.approval.dto.response.ApprovalNodeResponse;
import cn.cordys.crm.approval.dto.response.ResourceApprovalResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.domain.User;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.security.UserApprovalDTO;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResourceApprovalService {

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
	private ApprovalFlowService approvalFlowService;
	@Lazy
	@Resource
	private FieldSourceServiceProvider fieldSourceServiceProvider;

	/**
	 * 开启的审批流表单表格映射
	 */
	private static final Map<String, String> FORM_APPROVAL_TABLE = new HashMap<>(4);

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
	 * 更新业务表的审批状态
	 *
	 * @param formKey        表单类型
	 * @param resourceId     资源ID
	 * @param approvalStatus 审批状态
	 */
	public void updateApprovalStatus(FormKey formKey, String resourceId, String approvalStatus) {
		String tableName = FORM_APPROVAL_TABLE.get(formKey.getKey());
		if (StringUtils.isBlank(tableName)) {
			throw new GenericException(Translator.get("module.form.illegal"));
		}
		extApprovalInstanceMapper.updateApprovalStatus(tableName, resourceId, approvalStatus);
	}

	/**
	 * 手动提审
	 * @param param 提审参数
	 */
	public void push(ApprovalPushParam param, String currentOrgId, String currentUserId) {
		// 更新业务表 approval_status
		String tableName = FORM_APPROVAL_TABLE.get(param.getFormKey());
		if (StringUtils.isBlank(tableName)) {
			throw new GenericException(Translator.get("module.form.illegal"));
		}
		extApprovalInstanceMapper.updateApprovalStatus(tableName, param.getResourceId(), ApprovalStatus.APPROVING.name());
		// 插入审批实例和第一个审批任务
		ApprovalFlow approvalFlow = approvalFlowService.getEnabledFlow(param.getFormKey(), currentOrgId);
		if (approvalFlow == null) {
			throw new GenericException(Translator.get("approval_flow.not.exist"));
		}
		List<BaseModuleFieldValue> fvs = compressResourceDetail(param.getFormKey(), param.getResourceId());
		ApprovalNodeApproverResponse firstApproverNode = approvalFlowService.getResourceApprovalFlowFirstApproverNode(approvalFlow.getId(), fvs);
		ApprovalInstance approvalInstance = new ApprovalInstance();
		approvalInstance.setId(IDGenerator.nextStr());
		approvalInstance.setFlowId(approvalFlow.getId());
		approvalInstance.setType(param.getFormKey());
		approvalInstance.setResourceId(param.getResourceId());
		approvalInstance.setSubmitterId(currentUserId);
		approvalInstance.setSubmitTime(System.currentTimeMillis());
		approvalInstance.setApprovalStatus(ApprovalStatus.APPROVING.name());
		approvalInstance.setCurrentNodeId(firstApproverNode.getId());
		approvalInstance.setCreateTime(System.currentTimeMillis());
		approvalInstance.setCreateUser(currentUserId);
		approvalInstance.setUpdateTime(System.currentTimeMillis());
		approvalInstance.setUpdateUser(currentUserId);
		approvalInstanceMapper.insert(approvalInstance);

		/*
		 * TODO
		 *  1. 获取当前审批节点的审批人, 为多个审批人生成待办任务
		 *  2. 获取当前审批节点的抄送人, 为多个抄送人生成待办任务
		 */
		ApprovalTask task = new ApprovalTask();
		task.setId(IDGenerator.nextStr());
		task.setInstanceId(approvalInstance.getId());
		task.setNodeId(firstApproverNode.getId());
		task.setStatus(ApprovalStatus.APPROVING.name());
		task.setType(ApprovalTaskType.NL.name());
		task.setCreateTime(System.currentTimeMillis());
		task.setCreateUser(currentUserId);
		task.setUpdateTime(System.currentTimeMillis());
		task.setUpdateUser(currentUserId);
	}

	/**
	 * 获取业务数据详情
	 * @param formKey 表单Key
	 * @param resourceId 资源ID
	 * @return 通用的条件值
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private List<BaseModuleFieldValue> compressResourceDetail(String formKey, String resourceId) {
		List<BaseModuleFieldValue> fvs = new ArrayList<>();
		Object resourceDetail = fieldSourceServiceProvider.safeGetSimpleById(formKey, resourceId);
		if (resourceDetail == null) {
			return fvs;
		}
		Map<String, Object> detailMap = JSON.MAPPER.convertValue(resourceDetail, Map.class);
		if (MapUtils.isEmpty(detailMap)) {
			return fvs;
		}

		detailMap.forEach((k, v) -> {
			if (Strings.CI.equals(BaseResourceFieldService.DETAIL_FIELD_PARAM_NAME, k)) {
				List<Map> moduleFieldValues = (List<Map>) v;
				if (CollectionUtils.isNotEmpty(moduleFieldValues)) {
					for (Map mfv : moduleFieldValues) {
						BaseModuleFieldValue bfv = new BaseModuleFieldValue();
						bfv.setFieldId(mfv.get("fieldId").toString());
						bfv.setFieldValue(mfv.get("fieldValue"));
						fvs.add(bfv);
					}
				}
			} else {
				BaseModuleFieldValue bfv = new BaseModuleFieldValue();
				bfv.setFieldId(k);
				bfv.setFieldValue(v);
				fvs.add(bfv);
			}
		});

		return fvs;
	}
}
