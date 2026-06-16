package cn.cordys.crm.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalFlowNodeConfigRequest {

    @Schema(description = "节点配置列表")
    @Valid
    private List<ApprovalNodeRequest> nodes;

    @Schema(description = "节点连接配置列表")
    @Valid
    private List<ApprovalNodeLinkRequest> links;
}
