package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalTask;
import cn.cordys.crm.approval.dto.response.ApprovalTodoCountResponse;
import org.apache.ibatis.annotations.Param;


/**
 * 审批任务扩展Mapper
 */
public interface ExtApprovalTaskMapper {
    /**
     * 统计待我审批数量（总数 + 各资源类型）。
     */
    ApprovalTodoCountResponse countPendingByApprover(@Param("approverId") String approverId,
                                                     @Param("pendingStatus") String pendingStatus);

    void updateTaskById(@Param("approvalTask") ApprovalTask approvalTask);
}