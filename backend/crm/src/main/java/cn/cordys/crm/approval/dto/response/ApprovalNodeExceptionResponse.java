package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批异常节点")
public class ApprovalNodeExceptionResponse extends ApprovalNodeResponse {

}