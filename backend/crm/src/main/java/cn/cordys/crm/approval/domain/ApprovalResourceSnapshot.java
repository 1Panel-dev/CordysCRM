package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批编辑快照
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "approval_resource_snapshot")
public class ApprovalResourceSnapshot extends BaseModel {

    @Schema(description = "表单类型")
    private String formKey;

    @Schema(description = "资源ID")
    private String resourceId;

    @Schema(description = "编辑前资源数据快照(JSON)")
    private String snapshotData;
}
