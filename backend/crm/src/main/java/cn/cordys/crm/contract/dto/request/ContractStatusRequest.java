package cn.cordys.crm.contract.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContractStatusRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;


    @Schema(description = "状态: SIGNED/IN_PROGRESS/COMPLETED_PERFORMANCE/VOID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

}
