package cn.cordys.crm.approval.service;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.domain.*;
import cn.cordys.crm.approval.dto.ApprovalCcNode;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApprovalInstanceService {

	@Resource
	private BaseMapper<ApprovalInstance> approvalInstanceMapper;
	@Resource
	private BaseMapper<ApprovalTask> approvalTaskMapper;
	@Resource
	private BaseMapper<ApprovalAddSignTask> approvalAddSignTaskMapper;
	@Resource
	private BaseMapper<ApprovalCcTask> approvalCcTaskMapper;
	@Resource
	private BaseMapper<ApprovalReturnBackRecord> approvalReturnBackRecordMapper;
	@Resource
	private BaseMapper<ApprovalRecord> approvalRecordMapper;
	@Resource
	private BaseMapper<ApprovalInstanceAttachment> approvalInstanceAttachmentMapper;
	@Resource
	private BaseMapper<Attachment> attachmentMapper;

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
		Map<String, List<ApprovalCcTask>> ccTaskGroupMap = queryCcTasks(approvalTasks);
		Map<String, ApprovalReturnBackRecord> returnRecordMap = queryReturnRecords(approvalTasks);
		Map<String, ApprovalRecord> taskRecordMap = queryTaskRecords(approvalTasks, signTaskGroupMap);
		Map<String, List<Attachment>> elementAttachmentsMap = queryAttachments(approvalTasks, taskRecordMap, signTaskGroupMap);

		instanceDetail.setNodes(buildApprovalRecordNodeList(approvalTasks, signTaskGroupMap, ccTaskGroupMap, returnRecordMap, taskRecordMap, elementAttachmentsMap));
		return instanceDetail;
	}

	/**
	 * 获取最新审批实例
	 */
	private ApprovalInstance getLatestInstance(String resourceId) {
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
		List<String> ids = tasks.stream().filter(ApprovalTask::getIsAddSign).map(ApprovalTask::getId).toList();
		if (CollectionUtils.isEmpty(ids)) {
			return Map.of();
		}
		List<ApprovalAddSignTask> list = approvalAddSignTaskMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalAddSignTask>().in(ApprovalAddSignTask::getTaskId, ids));
		return list.stream().collect(Collectors.groupingBy(ApprovalAddSignTask::getTaskId));
	}

	/**
	 * 查询抄送任务并分组
	 */
	private Map<String, List<ApprovalCcTask>> queryCcTasks(List<ApprovalTask> tasks) {
		List<String> ids = tasks.stream().filter(ApprovalTask::getIsCc).map(ApprovalTask::getId).toList();
		if (CollectionUtils.isEmpty(ids)) {
			return Map.of();
		}
		List<ApprovalCcTask> list = approvalCcTaskMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalCcTask>().in(ApprovalCcTask::getTaskId, ids));
		return list.stream().collect(Collectors.groupingBy(ApprovalCcTask::getTaskId));
	}

	/**
	 * 查询退回记录
	 */
	private Map<String, ApprovalReturnBackRecord> queryReturnRecords(List<ApprovalTask> tasks) {
		List<String> ids = tasks.stream().filter(ApprovalTask::getIsReturn).map(ApprovalTask::getId).toList();
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
		elementIds.addAll(taskRecordMap.values().stream().map(ApprovalRecord::getId).toList());
		elementIds.addAll(signTaskGroupMap.values().stream().flatMap(List::stream)
				.filter(t -> ApprovalStatus.APPROVING.name().equals(t.getStatus())).map(ApprovalAddSignTask::getId).toList());
		elementIds.addAll(tasks.stream().filter(t -> t.getIsReturn() && ApprovalStatus.APPROVING.name().equals(t.getTaskStatus()))
				.map(ApprovalTask::getId).toList());

		if (CollectionUtils.isEmpty(elementIds)) {
			return Map.of();
		}
		List<ApprovalInstanceAttachment> attachments = approvalInstanceAttachmentMapper.selectListByLambda(
				new LambdaQueryWrapper<ApprovalInstanceAttachment>().in(ApprovalInstanceAttachment::getApprovalElementId, elementIds));
		if (CollectionUtils.isEmpty(attachments)) {
			return Map.of();
		}

		List<String> attachmentIds = attachments.stream().map(ApprovalInstanceAttachment::getAttachmentId).distinct().toList();
		List<Attachment> attachmentList = attachmentMapper.selectByIds(attachmentIds);
		Map<String, Attachment> attachmentMap = attachmentList.stream().collect(Collectors.toMap(Attachment::getId, a -> a, (v1, v2) -> v1));

		return attachments.stream().collect(Collectors.groupingBy(ApprovalInstanceAttachment::getApprovalElementId,
				Collectors.mapping(a -> attachmentMap.get(a.getAttachmentId()), Collectors.toList())));
	}

	/**
	 * 构建审批记录节点列表
	 * @param approvalTasks 审批任务列表
	 * @param signTaskGroupMap 加签任务分组
	 * @param ccTaskGroupMap 抄送任务分组
	 * @param returnRecordMap 退回记录映射
	 * @param taskRecordMap 任务执行记录映射
	 * @param elementAttachmentsMap 元素附件映射
	 * @return 审批记录节点列表
	 */
	private List<ApprovalRecordNode> buildApprovalRecordNodeList(List<ApprovalTask> approvalTasks, Map<String, List<ApprovalAddSignTask>> signTaskGroupMap,
																 Map<String, List<ApprovalCcTask>> ccTaskGroupMap, Map<String, ApprovalReturnBackRecord> returnRecordMap,
																 Map<String, ApprovalRecord> taskRecordMap,
										 						 Map<String, List<Attachment>> elementAttachmentsMap) {
		/*
		 * 处理不同类型的任务:
		 * 1. 加签任务: 需单独查询加签任务表, 并按位次配置追加。
		 * 2. 退回任务: 如果为审批中, 需带上退回记录信息。
		 * 3. 抄送任务: 查询该任务节点下所有的抄送任务, 并一起返回。
		 */
		List<ApprovalRecordNode> nodes = new ArrayList<>();
		for (ApprovalTask task : approvalTasks) {
			ApprovalRecordNode node = buildRecordNode(task, taskRecordMap.get(task.getId()), elementAttachmentsMap);
			// 处理加签任务节点
			if (task.getIsAddSign()) {
				List<ApprovalAddSignTask> addSignTasks = signTaskGroupMap.get(task.getId());
				if (CollectionUtils.isNotEmpty(addSignTasks)) {
					// 按加签类型分组, 并按位次排序
					List<ApprovalAddSignTask> beforeTasks = addSignTasks.stream()
							.filter(t -> "before".equals(t.getType()))
							.sorted(Comparator.comparingInt(ApprovalAddSignTask::getPos))
							.toList();
					List<ApprovalAddSignTask> afterTasks = addSignTasks.stream()
							.filter(t -> "after".equals(t.getType()))
							.sorted(Comparator.comparingInt(ApprovalAddSignTask::getPos))
							.toList();

					// 之前的节点
					for (ApprovalAddSignTask signTask : beforeTasks) {
						ApprovalRecordNode beforeNode = buildAddSignNode(signTask, taskRecordMap.get(signTask.getTaskId()), elementAttachmentsMap);
						beforeNode.setAddSignNode(true);
						nodes.add(beforeNode);
					}
					// 当前节点（当普通节点处理，有执行记录则从记录取意见和附件）
					nodes.add(node);
					// 之后节点
					for (ApprovalAddSignTask signTask : afterTasks) {
						ApprovalRecordNode afterNode = buildAddSignNode(signTask, taskRecordMap.get(signTask.getTaskId()), elementAttachmentsMap);
						afterNode.setAddSignNode(true);
						nodes.add(afterNode);
					}
				}
			} else if (task.getIsReturn() && ApprovalStatus.APPROVING.name().equals(task.getTaskStatus())) {
				// 退回任务节点: 仅在审批中时显示为退回节点, 并从退回记录获取意见和附件
				ApprovalReturnBackRecord returnRecord = returnRecordMap.get(task.getId());
				node.setReturnNode(true);
				if (returnRecord != null) {
					node.setComment(returnRecord.getReturnReason());
					node.setAttachments(elementAttachmentsMap.get(task.getId()));
				}
				nodes.add(node);
			} else if (task.getIsCc()) {
				// 处理抄送任务节点: 查询该任务节点下所有的抄送任务
				List<ApprovalCcTask> ccTaskList = ccTaskGroupMap.get(task.getId());
				if (CollectionUtils.isNotEmpty(ccTaskList)) {
					List<ApprovalCcNode> ccNodes = ccTaskList.stream().map(this::buildCcNode).toList();
					node.setCcNodes(ccNodes);
				}
				nodes.add(node);
			} else {
				// 普通任务节点直接添加
				nodes.add(node);
			}
		}

		return nodes;
	}

	/**
	 * 构建审批记录节点
	 */
	private ApprovalRecordNode buildRecordNode(ApprovalTask task, ApprovalRecord record,
												Map<String, List<Attachment>> elementAttachmentsMap) {
		ApprovalRecordNode node = ApprovalRecordNode.builder().taskId(task.getId())
				.nodeId(task.getNodeId()).approverId(task.getApproverId())
				.approvalStatus(task.getTaskStatus()).approvalTime(task.getCreateTime())
				.build();
		if (record != null) {
			node.setRecordId(record.getId());
			node.setComment(record.getComment());
			// 从执行记录获取附件
			node.setAttachments(elementAttachmentsMap.get(record.getId()));
		}
		return node;
	}

	/**
	 * 构建加签任务节点
	 */
	private ApprovalRecordNode buildAddSignNode(ApprovalAddSignTask signTask, ApprovalRecord record,
												Map<String, List<Attachment>> elementAttachmentsMap) {
		ApprovalRecordNode node = ApprovalRecordNode.builder().taskId(signTask.getTaskId()).recordId(record != null ? record.getId() : null)
				.approverId(signTask.getApproverId()).approvalStatus(signTask.getStatus())
				.approvalTime(signTask.getCreateTime())
				.build();
		// 如果有执行记录，从记录中获取意见和附件
		if (record != null) {
			node.setComment(record.getComment());
			node.setAttachments(elementAttachmentsMap.get(record.getId()));
		} else if (ApprovalStatus.APPROVING.name().equals(signTask.getStatus())) {
			// 审批中：从加签信息中取意见
			node.setComment(signTask.getComment());
			// 从加签任务获取附件
			node.setAttachments(elementAttachmentsMap.get(signTask.getId()));
		}
		return node;
	}

	/**
	 * 构建抄送节点
	 */
	private ApprovalCcNode buildCcNode(ApprovalCcTask ccTask) {
		ApprovalCcNode ccNode = new ApprovalCcNode();
		ccNode.setCcUserId(ccTask.getCcUserId());
		// TODO: 需查询用户信息获取名称和头像
		return ccNode;
	}
}
