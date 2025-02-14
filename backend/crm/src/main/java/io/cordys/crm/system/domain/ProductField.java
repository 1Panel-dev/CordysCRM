package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product_field")
public class ProductField {
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "产品id")
    private String productId;

    @Schema(description = "自定义属性id")
    private String fieldId;

    @Schema(description = "自定义属性值")
    private String fieldValue;
}
