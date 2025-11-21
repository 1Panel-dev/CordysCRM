package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpportunityQuotationEditRequest extends OpportunityQuotationAddRequest {

    @NotBlank
    @Schema(description = "ID")
    private String id;

    @NotBlank
    @Schema(description = "审批状态")
    private String approvalStatus;

}
