package cn.cordys.crm.approval.dto.response;

import cn.cordys.crm.approval.domain.ApprovalFlow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalFlowListResponse extends ApprovalFlow {

    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}