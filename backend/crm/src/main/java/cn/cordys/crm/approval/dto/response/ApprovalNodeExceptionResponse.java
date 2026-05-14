package cn.cordys.crm.approval.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.approval.dto.ApprovalPostConfigDTO;
import cn.cordys.crm.approval.dto.FieldPermissionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批异常节点")
public class ApprovalNodeExceptionResponse extends ApprovalNodeResponse {

}