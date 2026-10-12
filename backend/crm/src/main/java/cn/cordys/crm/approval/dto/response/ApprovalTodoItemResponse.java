package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalTodoItemResponse {

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "资源名称")
    private String resourceName;

    @Schema(description = "资源类型")
    private String resourceType;

    @Schema(description = "资源是否在公海（客户）/线索池（线索）中，仅资源类型为客户、线索时返回，其他类型为空")
    private Boolean inSharedPool;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "提交时间")
    private Long submitTime;

    @Schema(description = "审批操作")
    private String approvalOperation;

    @Schema(description = "数据结果")
    private String dataResult;

    @Schema(description = "审批任务ID")
    private String approvalTaskId;

    @Schema(description = "审批节点ID")
    private String approvalNodeId;

    @Schema(description = "审批实例ID")
    private String approvalInstanceId;

    @Schema(description = "审批人ID")
    private String approvalId;

    @Schema(description = "审批流ID")
    private String approvalFlowId;

    @Schema(description = "审批流版本ID")
    private String approvalFlowVersionId;

    @Schema(description = "执行时机：CREATE/UPDATE/DELETE")
    private String executeTime;
}
