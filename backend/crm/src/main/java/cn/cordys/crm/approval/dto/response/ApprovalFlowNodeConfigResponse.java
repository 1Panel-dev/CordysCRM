package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ApprovalFlowNodeConfigResponse {

    @Schema(description = "节点配置列表")
    private List<ApprovalNodeResponse> nodes;

    @Schema(description = "节点连接配置列表")
    private List<ApprovalNodeLinkResponse> links;
}
