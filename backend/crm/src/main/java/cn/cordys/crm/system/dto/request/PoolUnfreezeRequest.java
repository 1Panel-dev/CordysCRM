package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PoolUnfreezeRequest {

    @NotBlank
    @Schema(description = "线索或客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Size(max = 300, message = "{pool.unfreeze.reason.length}")
    @Schema(description = "解冻原因")
    private String reason;
}
