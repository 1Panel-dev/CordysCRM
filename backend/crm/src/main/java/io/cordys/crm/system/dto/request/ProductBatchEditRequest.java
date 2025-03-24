package io.cordys.crm.system.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 * @author guoyuqi
 */
@Data
public class ProductBatchEditRequest {

    @NotEmpty
    @Schema(description = "ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> ids;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}