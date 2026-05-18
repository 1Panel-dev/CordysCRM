package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import org.apache.ibatis.annotations.Param;


/**
 * 审批任务扩展Mapper
 */
public interface ExtApprovalTaskMapper {
    /**
     * 分页查询待我审批任务，按创建时间和ID倒序。
     */
    java.util.List<ApprovalTodoItemResponse> selectPendingTasks(@Param("approverId") String approverId,
                                                                @Param("pendingStatus") String pendingStatus,
                                                                @Param("resourceType") String resourceType,
                                                                @Param("resourceName") String resourceName);

    java.util.List<ApprovalTodoItemResponse> selectProcessedTasks(@Param("approverId") String approverId,
                                                                  @Param("pendingStatus") String pendingStatus,
                                                                  @Param("keyword") String keyword);

    java.util.List<ApprovalTodoItemResponse> selectInitiatedTasks(@Param("submitterId") String submitterId,
                                                                  @Param("keyword") String keyword);

    java.util.List<ApprovalTodoItemResponse> selectCcTasks(@Param("approverId") String approverId,
                                                           @Param("keyword") String keyword);

    /**
     * 统计待我审批数量（总数 + 各资源类型）。
     */
    ApprovalTodoCountResponse countPendingByApprover(@Param("approverId") String approverId,
                                                     @Param("pendingStatus") String pendingStatus);

    void updateTaskById(@Param("approvalTask") ApprovalTask approvalTask);
}
