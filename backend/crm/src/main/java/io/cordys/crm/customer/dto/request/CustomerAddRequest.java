package io.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;


/**
 *
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
public class CustomerAddRequest {

    @Size(max = 255)
    @NotBlank
    @Schema(description = "客户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 1000)
    private String tags;

    @NotNull
    @Schema(description = "是否在公海池")
    private Boolean isInSharedPool;

    @Size(max = 255)
    @NotBlank
    @Schema(description = "最终成交状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dealStatus;

}