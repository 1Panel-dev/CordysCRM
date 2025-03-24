package io.cordys.crm.system.dto.request;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


/**
 * @author guoyuqi
 */
@Data
public class ProductEditRequest {

    @Schema(description = "id")
    @Size(max = 32)
    private String id;

    @Schema(description = "产品名称")
    private String name;

    @Schema(description = "价格")
    private Double price;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "自定义字段")
    private List<BaseModuleFieldValue> moduleFields;
}