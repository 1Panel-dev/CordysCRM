package io.cordys.crm.customer.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;


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

    @NotBlank
    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @Size(max = 255)
    @NotBlank
    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 30)
    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;
}