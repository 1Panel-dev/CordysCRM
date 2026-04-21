package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.ApprovalInstance;
import cn.cordys.crm.system.domain.ApprovalTask;
import cn.cordys.crm.system.dto.request.ApprovalExecuteParam;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 审批流程执行服务
 */
@Service
public class ApprovalExecuteService {

	@Resource
	private BaseMapper<ApprovalTask> approvalTaskMapper;
	@Resource
	private BaseMapper<ApprovalInstance> approvalInstanceMapper;

	/**
	 * 执行审批流程
	 * @param executeParam 执行参数
	 */
	private void execute(ApprovalExecuteParam executeParam) {
		ApprovalTask currentTask;
		if (StringUtils.isNotEmpty(executeParam.getInstanceId())) {
			ApprovalInstance approvalInstance = approvalInstanceMapper.selectByPrimaryKey(executeParam.getInstanceId());
			currentTask = getCurrentTaskByInstanceNode(executeParam.getInstanceId(), approvalInstance.getCurrentNodeId());
		} else {
			currentTask = getTaskById(executeParam.getTaskId());
		}
		if (currentTask == null) {
			throw new GenericException("审批任务不存在!");
		}

		/*
		 * 1. 审批实例及当前审批任务流转, 并生成审批记录
		 * 2. 追加下一个审批任务
		 * TODO: 涉及到退回, 加签的逻辑后续补充
		 */
		currentTask.setTaskStatus(executeParam.getResult());
		currentTask.setUpdateTime(System.currentTimeMillis());
		currentTask.setUpdateUser(currentTask.getApproverId());
		approvalTaskMapper.update(currentTask);

		ApprovalTask nextTask = new ApprovalTask();
		nextTask.setId(IDGenerator.nextStr());
		nextTask.setInstanceId(executeParam.getInstanceId());
		// TODO: 获取下一个节点信息, 审批人配置信息, 以及额外信息
		nextTask.setCreateTime(System.currentTimeMillis());
		nextTask.setUpdateTime(System.currentTimeMillis());
		approvalTaskMapper.insert(nextTask);
	}


	/**
	 * 根据审批实例ID和当前节点ID获取当前审批任务
	 * @param instanceId 审批实例ID
	 * @param currentNodeId 当前节点ID
	 * @return 审批任务
	 */
	private ApprovalTask getCurrentTaskByInstanceNode(String instanceId, String currentNodeId) {
		ApprovalTask task = new ApprovalTask();
		task.setInstanceId(instanceId);
		task.setNodeId(currentNodeId);
		return approvalTaskMapper.selectOne(task);
	}

	/**
	 * 根据任务ID获取审批任务
	 * @param taskId 任务ID
	 * @return 审批任务
	 */
	private ApprovalTask getTaskById(String taskId) {
		return approvalTaskMapper.selectByPrimaryKey(taskId);
	}
}
