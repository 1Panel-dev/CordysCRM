package cn.cordys.crm.approval.dto.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "nodeType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ApprovalNodeApproverResponse.class, name = "APPROVER"),
    @JsonSubTypes.Type(value = ApprovalNodeConditionResponse.class, name = "CONDITION"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "START"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "END"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "DEFAULT"),
})
public class ApprovalNodeResponse {

    @Schema(description = "节点ID")
    private String id;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点类型名称")
    private String nodeTypeName;

    @Schema(description = "排序序号")
    private Integer sort;

    @Schema(description = "子节点列表")
    private List<ApprovalNodeResponse> children;
}