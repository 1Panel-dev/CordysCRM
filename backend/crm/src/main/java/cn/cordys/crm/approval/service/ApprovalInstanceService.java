package cn.cordys.crm.approval.service;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ApprovalTaskType;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.ApprovalCcNode;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.approval.dto.ApprovalTaskNode;
import cn.cordys.crm.approval.mapper.ExtApprovalInstanceMapper;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.crm.system.service.AttachmentService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.cordys.crm.approval.service.ApprovalResourceService.FORM_APPROVAL_TABLE;

@Service
public class ApprovalInstanceService {

	@Resource
	private BaseMapper<ApprovalInstance> approvalInstanceMapper;
	@Resource
	private BaseMapper<ApprovalTask> approvalTaskMapper;
	@Resource
	private BaseMapper<ApprovalAddSignTask> approvalAddSignTaskMapper;
	@Resource
	private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;
	@Resource
	private BaseMapper<ApprovalRecord> approvalRecordMapper;
	@Resource
	private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
	@Resource
	private BaseMapper<Attachment> attachmentMapper;
	@Resource
	private BaseMapper<ApprovalFlowVersion> flowVersionMapper;
	@Resource
	private ExtApprovalInstanceMapper extApprovalInstanceMapper;
	@Resource
	private AttachmentService attachmentService;

	/**
	 * 获取资源最新审批实例详情
	 * @param resourceId 资源ID
	 * @return 审批实例详情
	 */
	public ApprovalInstanceDetail getLatestApprovalInstanceDetail(String resourceId) {
		ApprovalInstance latestInstance = getLatestInstance(resourceId);
		if (latestInstance == null) {
			return null;
		}

		ApprovalInstanceDetail instanceDetail = BeanUtils.copyBean(new ApprovalInstanceDetail(), latestInstance);
		List<ApprovalTask> approvalTasks = getFilteredTasks(latestInstance);
		if (CollectionUtils.isEmpty(approvalTasks)) {
			instanceDetail.setNodes(null);
			return instanceDetail;
		}

		Map<String, List<ApprovalAddSignTask>> signTaskGroupMap = queryAddSignTasks(approvalTasks);
		Map<String, ApprovalReturnBackRecord> returnRecordMap = queryReturnRecords(approvalTasks);
		Map<String, ApprovalRecord> taskRecordMap = queryTaskRecords(approvalTasks, signTaskGroupMap);
		Map<String, List<Attachment>> elementAttachmentsMap = queryAttachments(approvalTasks, taskRecordMap, signTaskGroupMap);

		instanceDetail.setNodes(buildApprovalRecordNodeList(approvalTasks, signTaskGroupMap, returnRecordMap, taskRecordMap, elementAttachmentsMap));
		return instanceDetail;
	}

	/**
	 * 获取最新审批实例
	 */
	public ApprovalInstance getLatestInstance(String resourceId) {
		LambdaQueryWrapper<ApprovalInstance> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ApprovalInstance::getResourceId, resourceId).orderByDesc(ApprovalInstance::getSubmitTime);
		List<ApprovalInstance> list = approvalInstanceMapper.selectListByLambda(wrapper);
		return CollectionUtils.isEmpty(list) ? null : list.getFirst();
	}

	/**
	 * 获取过滤后的任务列表(按节点分组取最新)
	 */
	private List<ApprovalTask> getFilteredTasks(ApprovalInstance latestInstance) {
		LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId()).orderByDesc(ApprovalTask::getCreateTime);
		List<ApprovalTask> allTasks = approvalTaskMapper.selectListByLambda(wrapper);
		if (CollectionUtils.isEmpty(allTasks)) {
			return null;
		}
		return allTasks.stream().collect(Collectors.groupingBy(ApprovalTask::getNodeId))
				.values().stream().map(List::getFirst)
				.sorted(Comparator.comparing(ApprovalTask::getCreateTime)).toList();
	}

	/**
	 * 查询加签任务并分组
	 */
	private Map<String, List<ApprovalAddSignTask>> queryAddSignTasks(List<ApprovalTask> tasks) {
		List<String> ids = tasks.stream().filter(task -> Strings.CI.equals(task.getType(), ApprovalTaskType.SN.name())).map(ApprovalTask::getId).toList();
		if (CollectionUtils.isEmpty(ids)) {
			return Map.of();
		}
		List<ApprovalAddSignTask> list = approvalAddSignTaskMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalAddSignTask>().in(ApprovalAddSignTask::getTaskId, ids));
		return list.stream().collect(Collectors.groupingBy(ApprovalAddSignTask::getTaskId));
	}

	/**
	 * 查询退回记录
	 */
	private Map<String, ApprovalReturnBackRecord> queryReturnRecords(List<ApprovalTask> tasks) {
		List<String> ids = tasks.stream().filter(task -> Strings.CI.equals(task.getType(), ApprovalTaskType.BK.name())).map(ApprovalTask::getId).toList();
		if (CollectionUtils.isEmpty(ids)) {
			return Map.of();
		}
		List<ApprovalReturnBackRecord> list = approvalReturnBackRecordMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalReturnBackRecord>().in(ApprovalReturnBackRecord::getTaskId, ids));
		return list.stream().collect(Collectors.toMap(ApprovalReturnBackRecord::getTaskId, r -> r, (v1, v2) -> v1));
	}

	/**
	 * 查询任务执行记录
	 */
	private Map<String, ApprovalRecord> queryTaskRecords(List<ApprovalTask> tasks, Map<String, List<ApprovalAddSignTask>> signTaskGroupMap) {
		List<String> taskIds = tasks.stream().map(ApprovalTask::getId).toList();
		List<String> signIds = signTaskGroupMap.values().stream().flatMap(List::stream)
				.map(ApprovalAddSignTask::getTaskId).toList();
		List<ApprovalRecord> records = approvalRecordMapper.selectByIds(ListUtils.union(taskIds, signIds));
		return records.stream().collect(Collectors.toMap(ApprovalRecord::getTaskId, r -> r));
	}

	/**
	 * 查询附件信息并按元素ID分组
	 */
	private Map<String, List<Attachment>> queryAttachments(List<ApprovalTask> tasks, Map<String, ApprovalRecord> taskRecordMap,
															Map<String, List<ApprovalAddSignTask>> signTaskGroupMap) {
		List<String> elementIds = new ArrayList<>();
		// elementIds.addAll(taskRecordMap.values().stream().map(ApprovalRecord::getId).toList());
		// elementIds.addAll(signTaskGroupMap.values().stream().flatMap(List::stream)
		// 		.filter(t -> ApprovalStatus.APPROVING.name().equals(t.getStatus())).map(ApprovalAddSignTask::getId).toList());
		// elementIds.addAll(tasks.stream().filter(t -> t.getIsReturn() && ApprovalStatus.APPROVING.name().equals(t.getTaskStatus()))
		// 		.map(ApprovalTask::getId).toList());

		if (CollectionUtils.isEmpty(elementIds)) {
			return Map.of();
		}
		List<ApprovalInstanceAttachment> attachments = approvalInstanceAttachmentMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalInstanceAttachment>().in(ApprovalInstanceAttachment::getElementId, elementIds));
		if (CollectionUtils.isEmpty(attachments)) {
			return Map.of();
		}

		List<String> attachmentIds = attachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).distinct().toList();
		List<Attachment> attachmentList = attachmentMapper.selectByIds(attachmentIds);
		Map<String, Attachment> attachmentMap = attachmentList.stream().collect(Collectors.toMap(Attachment::getId, a -> a, (v1, v2) -> v1));

		return attachments.stream().collect(Collectors.groupingBy(ApprovalInstanceAttachment::getElementId,
				Collectors.mapping(a -> attachmentMap.get(a.getAttachmentId()), Collectors.toList())));
	}

	/**
	 * 构建审批记录节点列表
	 * @param approvalTasks 审批任务列表
	 * @param signTaskGroupMap 加签任务分组
	 * @param returnRecordMap 退回记录映射
	 * @param taskRecordMap 任务执行记录映射
	 * @param attachmentsMap 元素附件映射
	 * @return 审批记录节点列表
	 */
	private List<ApprovalRecordNode> buildApprovalRecordNodeList(List<ApprovalTask> approvalTasks, Map<String, List<ApprovalAddSignTask>> signTaskGroupMap, Map<String, ApprovalReturnBackRecord> returnRecordMap,
																 Map<String, ApprovalRecord> taskRecordMap,
										 						 Map<String, List<Attachment>> attachmentsMap) {
		/*
		 * 处理不同类型的任务:
		 * 1. 加签任务: 需单独查询加签任务表, 并按位次配置追加。
		 * 2. 退回任务: 如果为审批中, 需带上退回记录信息。
		 * 3. 抄送任务: 查询该任务节点下所有的抄送任务, 并一起返回。
		 */
		List<ApprovalRecordNode> nodes = new ArrayList<>();
		Map<String, List<ApprovalTask>> nodeTasks = approvalTasks.stream().collect(Collectors.groupingBy(ApprovalTask::getNodeId));
		nodeTasks.forEach((nodeId, tasks) -> {
			List<ApprovalTask> ccTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) == ApprovalTaskType.CC).toList();
			List<ApprovalCcNode> ccNodes = ccTasks.stream().map(ccTask -> new ApprovalCcNode(ccTask.getApproverId(), ccTask.getApproverId(), ccTask.getApproverId())).toList();
			List<ApprovalTask> normalTasks = tasks.stream().filter(task -> ApprovalTaskType.valueOf(task.getType()) != ApprovalTaskType.NL).toList();
			List<ApprovalTaskNode> taskNodes = normalTasks.stream().map(task -> buildTaskNode(task, taskRecordMap, attachmentsMap)).toList();
			ApprovalRecordNode recordNode = ApprovalRecordNode.builder().ccNodes(ccNodes).taskNodes(taskNodes).build();
			nodes.add(recordNode);
		});

		return nodes;
	}

	private ApprovalTaskNode buildTaskNode(ApprovalTask task, Map<String, ApprovalRecord> taskRecordMap, Map<String, List<Attachment>> attachmentsMap) {
		ApprovalRecord record = taskRecordMap.get(task.getId());
		ApprovalTaskNode taskNode = ApprovalTaskNode.builder().taskId(task.getId()).approverId(task.getApproverId()).approvalStatus(task.getStatus()).approvalTime(task.getUpdateTime()).build();
		if (record != null) {
			taskNode.setComment(record.getComment());
			taskNode.setAttachments(attachmentsMap.get(record.getId()));
		}
		return taskNode;
	}

	/**
	 * 刷新最终实例状态
	 *
	 * @param instance     审批实例
	 * @param currentUserId  当前用户ID
	 */
	public void rejectApprovalInstance(ApprovalInstance instance, String currentUserId) {
		instance.setApprovalStatus(ApprovalStatus.UNAPPROVED.name());
		instance.setApprovalTime(System.currentTimeMillis());
		instance.setUpdateUser(currentUserId);
		instance.setUpdateTime(System.currentTimeMillis());
		approvalInstanceMapper.updateById(instance);
	}

	/**
	 * 清除审批中的实例数据 (审批流删除时调用)
	 * @param flowId 审批流ID
	 */
	public void clearApprovingInstanceOfFlow(String flowId) {
		ApprovalFlowVersion versionCriteria = new ApprovalFlowVersion();
		versionCriteria.setFlowId(flowId);
		List<ApprovalFlowVersion> versions = flowVersionMapper.select(versionCriteria);
		List<String> flowVersionIds = versions.stream().map(ApprovalFlowVersion::getId).toList();
		// 筛选出审批中的实例
		LambdaQueryWrapper<ApprovalInstance> instanceLambdaQueryWrapper = new LambdaQueryWrapper<ApprovalInstance>()
				.in(ApprovalInstance::getFlowVersionId, flowVersionIds)
				.eq(ApprovalInstance::getApprovalStatus, ApprovalStatus.APPROVING.name());
		List<ApprovalInstance> instances = approvalInstanceMapper.selectListByLambda(instanceLambdaQueryWrapper);
		instances.forEach(instance -> {
			String tableName = FORM_APPROVAL_TABLE.get(instance.getType());
			extApprovalInstanceMapper.updateApprovalStatus(tableName, instance.getResourceId(), ApprovalStatus.NONE.name());
		});
		approvalInstanceMapper.deleteByLambda(instanceLambdaQueryWrapper);
		// 删除审批实例相关数据: 待办任务、加签任务、退回记录、执行记录、实例附件等
		List<String> instanceIds = instances.stream().map(ApprovalInstance::getId).toList();
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalTask>().in(ApprovalTask::getInstanceId, instanceIds));
		List<String> taskIds = approvalTasks.stream().map(ApprovalTask::getId).toList();
		approvalTaskMapper.deleteByIds(taskIds);
		approvalAddSignTaskMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalAddSignTask>().in(ApprovalAddSignTask::getTaskId, taskIds));
		approvalReturnBackRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalReturnBackRecord>().in(ApprovalReturnBackRecord::getTaskId, taskIds));
		approvalRecordMapper.deleteByLambda(new LambdaQueryWrapper<ApprovalRecord>().in(ApprovalRecord::getInstanceId, instanceIds));
		List<ApprovalInstanceAttachment> instanceAttachments = approvalInstanceAttachmentMapper.selectListByLambda(new LambdaQueryWrapper<ApprovalInstanceAttachment>().in(ApprovalInstanceAttachment::getInstanceId, instanceIds));
		approvalInstanceAttachmentMapper.deleteByIds(instanceAttachments.stream().map(ApprovalInstanceAttachment::getId).toList());
		List<String> attachmentIds = instanceAttachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).toList();
		attachmentIds.forEach(id -> attachmentService.delete(id));
		attachmentMapper.deleteByIds(attachmentIds);
	}
}
