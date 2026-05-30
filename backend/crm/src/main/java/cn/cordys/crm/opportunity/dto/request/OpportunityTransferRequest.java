package cn.cordys.crm.opportunity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OpportunityTransferRequest {

    @NotBlank
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @NotBlank
    @Size(max = 32)
    @Schema(description = "修改负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String owner;
}
