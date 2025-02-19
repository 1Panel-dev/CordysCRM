package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "product")
public class Product extends BaseModel {
    @Schema(description = "组织机构id")
    private String organizationId;

    @Schema(description = "产品名称")
    private String name;
}
