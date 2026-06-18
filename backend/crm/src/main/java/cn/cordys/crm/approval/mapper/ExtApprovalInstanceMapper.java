package cn.cordys.crm.approval.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtApprovalInstanceMapper {

	/**
	 * 更新业务表的审批状态
	 *
	 * @param sourceTable     主业务表
	 * @param resourceId      资源ID
	 * @param approvalStatus  审批状态
	 */
	void updateApprovalStatus(@Param("sourceTable") String sourceTable, @Param("id") String resourceId,
							  @Param("approvalStatus") String approvalStatus);
	/**
	 * 更新业务表的 approved 状态
	 *
	 * @param sourceTable     主业务表
	 * @param resourceId      资源ID
	 */
	void setApproved(@Param("sourceTable") String sourceTable, @Param("id") String resourceId);

	/**
	 * 查询业务表业务名称
	 * @param sourceTable
	 * @param id
	 */
	String selectBusinessName(@Param("sourceTable") String sourceTable, @Param("id") String id);

	/**
	 * 查询业务表的审批状态
	 *
	 * @param sourceTable 主业务表
	 * @param id          资源ID
	 * @return 审批状态
	 */
	String selectApprovalStatus(@Param("sourceTable") String sourceTable, @Param("id") String id);

	String getResourceOwner(@Param("sourceTable")String sourceTable, @Param("id")String id);

	/**
	 * 查询业务资源是否审批通过过
	 *
	 * @param sourceTable 主业务表
	 * @param id          资源ID
	 * @return 是否审批通过过
	 */
	Boolean selectApproved(@Param("sourceTable") String sourceTable, @Param("id") String id);

	/**
	 * 获取节点下一个执行轮次
	 * 从 approval_task 和 approval_record 中一起获取，取最大轮次 +1
	 *
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @return 下一个轮次
	 */
	Integer getNextNodeRound(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId);

	/**
	 * 获取节点下一个执行轮次
	 * 从 approval_task 和 approval_record 中一起获取，取最大轮次 +1
	 *
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @return 下一个轮次
	 */
	Integer getNodeRound(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId);

	/**
	 * 假删除当前节点轮次已处理的待办任务
	 *
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @param nodeRound 轮次
	 */
	void batchClearNotApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId, @Param("nodeRound") Integer nodeRound);

	/**
	 * 撤销中止审批中的节点轮次任务
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @param nodeRound 轮次
	 */
	void loseApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId, @Param("nodeRound") Integer nodeRound);

	/**
	 * 撤销中止审批中的节点轮次任务
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @param nodeRound 轮次
	 */
	void batchClearApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId, @Param("nodeRound") Integer nodeRound);

	/**
	 * 假删除当前节点的执行记录
	 *
	 * @param instanceId 审批实例ID
	 * @param nodeId     节点ID
	 * @param nodeRound 轮次
	 */
	void batchClearRecord(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId, @Param("nodeRound") Integer nodeRound);
}
