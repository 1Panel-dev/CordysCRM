package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityQuotationBatchVoidedRequest {

    @NotEmpty
    @Schema(description = "ID集合")
    private List<String> ids;

}
