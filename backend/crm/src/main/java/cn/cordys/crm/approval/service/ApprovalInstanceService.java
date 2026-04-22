package cn.cordys.crm.approval.service;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.approval.domain.ApprovalAddSignTask;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalRecord;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.ApprovalInstanceDetail;
import cn.cordys.crm.approval.dto.ApprovalRecordNode;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
	private BaseMapper<ApprovalRecord> approvalRecordMapper;

	/**
	 * 获取资源最新审批实例详情
	 * @param resourceId 资源ID
	 * @return 审批实例详情
	 */
	public ApprovalInstanceDetail getLatestApprovalInstanceDetail(String resourceId) {
		// 获取最新审批实例
		LambdaQueryWrapper<ApprovalInstance> instanceWrapper = new LambdaQueryWrapper<>();
		instanceWrapper.eq(ApprovalInstance::getResourceId, resourceId)
				.orderByDesc(ApprovalInstance::getSubmitTime);
		List<ApprovalInstance> approvalInstances = approvalInstanceMapper.selectListByLambda(instanceWrapper);
		if (CollectionUtils.isEmpty(approvalInstances)) {
			return null;
		}
		ApprovalInstance latestInstance = approvalInstances.getFirst();
		ApprovalInstanceDetail instanceDetail = BeanUtils.copyBean(new ApprovalInstanceDetail(), latestInstance);

		// 获取所有审批任务 TODO: (只保留实例下节点最新的任务, 退回及撤回后会生成新的节点任务)
		LambdaQueryWrapper<ApprovalTask> taskWrapper = new LambdaQueryWrapper<>();
		taskWrapper.eq(ApprovalTask::getInstanceId, latestInstance.getId())
				.orderByAsc(ApprovalTask::getCreateTime);
		List<ApprovalTask> approvalTasks = approvalTaskMapper.selectListByLambda(taskWrapper);
		if (CollectionUtils.isNotEmpty(approvalTasks)) {
			instanceDetail.setNodes(null);
			return instanceDetail;
		}

		List<String> signTaskIds = approvalTasks.stream().filter(ApprovalTask::getIsAddSign).map(ApprovalTask::getId).toList();
		LambdaQueryWrapper<ApprovalAddSignTask> signTaskWrapper = new LambdaQueryWrapper<>();
		signTaskWrapper.in(ApprovalAddSignTask::getTaskId, signTaskIds);
		List<ApprovalAddSignTask> signTasks = approvalAddSignTaskMapper.selectListByLambda(signTaskWrapper);
		Map<String, List<ApprovalAddSignTask>> signTaskGroupMap = signTasks.stream().collect(Collectors.groupingBy(ApprovalAddSignTask::getTaskId));

		List<String> taskIds = approvalTasks.stream().map(ApprovalTask::getId).toList();
		List<String> signIds = signTasks.stream().map(ApprovalAddSignTask::getTaskId).toList();
		List<ApprovalRecord> approvalRecords = approvalRecordMapper.selectByIds(ListUtils.union(taskIds, signIds));
		Map<String, ApprovalRecord> taskRecordMap = approvalRecords.stream().collect(Collectors.toMap(ApprovalRecord::getTaskId, record -> record));

		/*
		 * 处理不同类型的任务:
		 * 1. 加签任务: 需单独查询加签任务表, 并按位次配置追加。
		 * 2. 退回任务: 如果为审批中, 需带上退回记录信息。
		 * 3. 抄送任务: 查询该任务节点下所有的抄送任务, 并一起返回。
		 */
		List<ApprovalRecordNode> nodes = new ArrayList<>();
		for (ApprovalTask task : approvalTasks) {
			if (task.getIsAddSign()) {
				// TODO: 处理加签任务节点

			}

			if (task.getIsReturn()) {
				// TODO: 处理退回任务节点

			}

			if (task.getIsCc()) {
				// TODO: 处理抄送任务节点

			}
		}

		instanceDetail.setNodes(nodes);
		return instanceDetail;
	}
}
