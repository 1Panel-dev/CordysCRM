package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpportunityUpdateRequest extends OpportunityAddRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;
}
