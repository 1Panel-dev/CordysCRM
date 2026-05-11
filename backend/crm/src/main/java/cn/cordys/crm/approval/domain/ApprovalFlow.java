package cn.cordys.crm.approval.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "approval_flow")
public class ApprovalFlow extends BaseModel {

    @Schema(description = "当前版本ID")
    private String currentVersionId;

    @Schema(description = "流程编码")
    private String number;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "表单类型：quotation/contract/invoice/order")
    private String formType;

    @Schema(description = "启用状态")
    private Boolean enable;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "组织id")
    private String organizationId;
}
