package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractInvoiceApprovalRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;


    @NotBlank
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String approvalStatus;
}
