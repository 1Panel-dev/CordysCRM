package cn.cordys.crm.approval.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.approval.constants.ApprovalNodeTypeEnum;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "nodeType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ApprovalNodeApproverRequest.class, name = "APPROVER"),
    @JsonSubTypes.Type(value = ApprovalNodeConditionRequest.class, name = "CONDITION"),
    @JsonSubTypes.Type(value = ApprovalNodeRequest.class, name = "START"),
    @JsonSubTypes.Type(value = ApprovalNodeRequest.class, name = "END"),
    @JsonSubTypes.Type(value = ApprovalNodeRequest.class, name = "DEFAULT"),
})
public class ApprovalNodeRequest {

    @Schema(description = "节点ID")
    @NotBlank
    private String id;

    @Schema(description = "节点编码")
    private String number;

    @Schema(description = "节点名称")
    private String name;

    @EnumValue(enumClass = ApprovalNodeTypeEnum.class)
    @NotBlank(message = "节点类型不能为空")
    @Schema(description = "节点类型：START/APPROVER/CONDITION/DEFAULT/END")
    private String nodeType;
}