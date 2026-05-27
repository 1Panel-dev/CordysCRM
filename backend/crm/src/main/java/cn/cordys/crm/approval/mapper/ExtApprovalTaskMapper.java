package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import org.apache.ibatis.annotations.Param;


/**
 * 审批任务扩展Mapper
 */
public interface ExtApprovalTaskMapper {
    /**
     * 查询资源最近一次审批实例（按提交时间和ID倒序）。
     */
    ApprovalInstance selectLatestInstanceByResourceId(@Param("resourceId") String resourceId);

    /**
     * 分页查询待我审批任务，按创建时间和ID倒序。
     */
    java.util.List<ApprovalTodoItemResponse> selectPendingTasks(@Param("approverId") String approverId,
                                                                @Param("pendingStatus") String pendingStatus,
                                                                @Param("resourceType") String resourceType,
                                                                @Param("resourceName") String resourceName);

    java.util.List<ApprovalTodoItemResponse> selectProcessedTasks(@Param("approverId") String approverId,
                                                                  @Param("resourceType") String resourceType,
                                                                  @Param("keyword") String keyword);

    java.util.List<ApprovalTodoItemResponse> selectInitiatedTasks(@Param("submitterId") String submitterId,
                                                                  @Param("resourceType") String resourceType,
                                                                  @Param("keyword") String keyword);

    java.util.List<ApprovalTodoItemResponse> selectCcTasks(@Param("approverId") String approverId,
                                                           @Param("resourceType") String resourceType,
                                                           @Param("keyword") String keyword);

    /**
     * 统计待我审批数量（总数 + 各资源类型）。
     */
    ApprovalTodoCountResponse countPendingByApprover(@Param("approverId") String approverId,
                                                     @Param("pendingStatus") String pendingStatus);

    void updateTaskById(@Param("approvalTask") ApprovalTask approvalTask);

	/**
	 * 移动加签的根节点
	 * @param oldRootId 旧的根节点ID
	 * @param newRootId 新的根节点ID
	 */
	void moveAddSignRoot(@Param("oldRootId") String oldRootId, @Param("newRootId") String newRootId);

	/**
	 * 更新加签根节点的下一个指向
	 * @param oldRootId 旧的根节点ID
	 * @param newRootId 新的根节点ID
	 */
	void updateRootNext(@Param("oldRootId") String oldRootId, @Param("newRootId") String newRootId);

    void updateApprover(@Param("approverId")String approverId);
}
