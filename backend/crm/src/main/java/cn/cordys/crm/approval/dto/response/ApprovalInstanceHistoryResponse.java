package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalInstanceHistoryResponse {

    @Schema(description = "审批实例ID")
    private String id;

    @Schema(description = "审批状态")
    private String approvalStatus;

    @Schema(description = "本轮结束时的节点ID")
    private String currentNodeId;

    @Schema(description = "本轮结束时的节点名称")
    private String currentNodeName;
}
