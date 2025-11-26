package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContractArchivedRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;


    @NotBlank
    @Schema(description = "归档状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String archivedStatus;
}
