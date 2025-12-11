package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpportunityQuotationEditRequest extends OpportunityQuotationAddRequest {

    @NotBlank(message = "{opportunity.quotation.id.required}")
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String approvalStatus;

}
