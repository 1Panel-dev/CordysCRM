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
public class CustomerContactUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Schema(description = "客户id")
    private String customerId;

    @Schema(description = "联系人姓名")
    private String name;





    @Schema(description = "是否停用")
    private Boolean enable;

}