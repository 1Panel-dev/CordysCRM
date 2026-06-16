package cn.cordys.crm.approval.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "nodeType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ApprovalNodeApproverResponse.class, name = "APPROVER"),
    @JsonSubTypes.Type(value = ApprovalNodeConditionResponse.class, name = "CONDITION"),
    @JsonSubTypes.Type(value = ApprovalNodeExceptionResponse.class, name = "EXCEPTION"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "START"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "END"),
    @JsonSubTypes.Type(value = ApprovalNodeResponse.class, name = "DEFAULT"),
})
public class ApprovalNodeResponse {

    @Schema(description = "节点ID")
    private String id;

    @Schema(description = "节点编码")
    private String number;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "排序序号")
    private Integer sort;

    @Schema(description = "执行时机：CREATE/UPDATE/DELETE")
    private String executeTime;
}