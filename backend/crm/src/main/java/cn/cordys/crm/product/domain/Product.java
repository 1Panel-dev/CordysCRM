package cn.cordys.crm.product.domain;

import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.util.BigDecimalNoTrailingZeroSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Data;

@Data
@Table(name = "product")
public class Product extends BaseModel {
  @Schema(description = "组织机构id")
  private String organizationId;

  @Schema(description = "产品名称")
  private String name;

  @Schema(description = "价格")
  @DecimalMin(value = "0.00", inclusive = false, message = "{product.price.min}")
  @DecimalMax(value = "10000000000.00", inclusive = false, message = "{product.price.max}")
  @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class)
  private BigDecimal price;

  @Schema(description = "状态")
  private String status;

  @Schema(description = "排序")
  private Long pos;
}
