package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalInstance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApprovalInstanceMapper {

    /**
     * 根据任务锁定所属实例，使同一实例的审批推进按顺序执行。
     */
    ApprovalInstance selectByTaskIdForUpdate(@Param("taskId") String taskId);

    /**
     * 查询当前组织业务的全部审批实例，按提审时间和实例ID倒序。
     */
    List<ApprovalInstance> selectInstancesByResourceId(@Param("resourceId") String resourceId,
            @Param("organizationId") String organizationId);

    /**
     * 判断当前组织的资源是否已有审批中的实例，跨执行时机和流程版本去重。
     */
    boolean existsApprovingInstance(@Param("organizationId") String organizationId, @Param("formKey") String formKey,
            @Param("resourceId") String resourceId);

    /**
     * 更新业务表的审批状态
     *
     * @param sourceTable    主业务表
     * @param resourceId     资源ID
     * @param approvalStatus 审批状态
     */
    void updateApprovalStatus(@Param("sourceTable") String sourceTable, @Param("id") String resourceId,
            @Param("approvalStatus") String approvalStatus);

    /**
     * 更新业务表的 approved 状态
     *
     * @param sourceTable 主业务表
     * @param resourceId  资源ID
     */
    void setApproved(@Param("sourceTable") String sourceTable, @Param("id") String resourceId);

    /**
     * 查询业务表业务名称
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

    String getResourceOwner(@Param("sourceTable") String sourceTable, @Param("id") String id,
                            @Param("organizationId") String organizationId);

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
     * 假删除当前节点截至指定轮次的所有已处理待办任务
     *
     * @param instanceId 审批实例ID
     * @param nodeId     节点ID
     * @param nodeRound  轮次
     */
    void batchClearNotApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId,
            @Param("nodeRound") Integer nodeRound);

    /**
     * 撤销中止审批中的节点轮次任务
     *
     * @param instanceId 审批实例ID
     * @param nodeId     节点ID
     * @param nodeRound  轮次
     */
    void loseApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId,
            @Param("nodeRound") Integer nodeRound);

    /**
     * 撤销中止当前节点截至指定轮次的所有审批中任务
     *
     * @param instanceId 审批实例ID
     * @param nodeId     节点ID
     * @param nodeRound  轮次
     */
    void batchClearApprovingTask(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId,
            @Param("nodeRound") Integer nodeRound);

    /**
     * 假删除当前节点截至指定轮次的所有执行记录
     *
     * @param instanceId 审批实例ID
     * @param nodeId     节点ID
     * @param nodeRound  轮次
     */
    void batchClearRecord(@Param("instanceId") String instanceId, @Param("nodeId") String nodeId,
            @Param("nodeRound") Integer nodeRound);
}
