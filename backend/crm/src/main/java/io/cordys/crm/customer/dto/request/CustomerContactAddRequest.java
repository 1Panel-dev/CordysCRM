package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
@Data
public class CustomerContactAddRequest {

    @Size(max = 32)
    @NotBlank
    @Schema(description = "客户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Size(max = 255)
    @NotBlank
    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}