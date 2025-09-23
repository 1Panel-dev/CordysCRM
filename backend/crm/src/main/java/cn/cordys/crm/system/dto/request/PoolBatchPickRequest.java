package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoolBatchPickRequest extends PoolBatchRequest {

    @NotBlank
    @Schema(description = "公海ID||线索池ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String poolId;
}
