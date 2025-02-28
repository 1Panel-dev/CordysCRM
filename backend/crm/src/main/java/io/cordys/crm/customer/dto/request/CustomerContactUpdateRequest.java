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
public class CustomerContactUpdateRequest {

    @NotBlank
    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 32)
    private String id;

    @Size(max = 32)
    @Schema(description = "客户id")
    private String customerId;

    @Size(max = 255)
    @Schema(description = "联系人姓名")
    private String name;

    @Size(max = 32)
    @Schema(description = "负责人")
    private String owner;

    @Schema(description = "是否停用")
    private Boolean enable;

    @Schema(description = "模块字段值")
    private List<BaseModuleFieldValue> moduleFields;
}