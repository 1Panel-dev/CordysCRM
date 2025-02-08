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
public class CustomerUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Schema(description = "客户名称")
    private String name;





    @Schema(description = "标签")
    private String tags;

    @Schema(description = "是否在公海池")
    private Boolean isInSharedPool;

    @Schema(description = "最终成交状态")
    private String dealStatus;

}