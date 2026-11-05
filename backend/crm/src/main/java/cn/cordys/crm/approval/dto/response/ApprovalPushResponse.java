package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ApprovalPushResponse {

    @Schema(description = "指定重审节点已失效，本次已回退整单重审；未指定重审节点或指定节点有效时为 false")
    private boolean resubmitNodeInvalid;
}
