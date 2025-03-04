package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactDisableRequest {
    @Size(max = 255)
    @Schema(description = "停用原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}