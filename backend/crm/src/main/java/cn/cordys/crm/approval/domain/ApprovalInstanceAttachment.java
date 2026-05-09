package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_instance_attachment")
public class ApprovalInstanceAttachment {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "审批实例ID")
    private String instanceId;

    @Schema(description = "审批节点ID")
    private String elementId;

    @Schema(description = "附件ID")
    private String attachmentId;
}
