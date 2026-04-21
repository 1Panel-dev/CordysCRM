package cn.cordys.crm.approval.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "approval_flow_blob")
public class ApprovalFlowBlob {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "状态权限配置（JSON格式）")
    private String statusPermissions;

    @Schema(description = "流程描述")
    private String description;
}