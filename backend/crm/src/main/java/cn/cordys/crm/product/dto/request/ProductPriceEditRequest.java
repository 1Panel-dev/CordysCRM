package cn.cordys.crm.product.dto.request;

import cn.cordys.common.domain.BaseModuleFieldValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ProductPriceEditRequest {

  @NotBlank(message = "{product.price.id.required}")
  @Size(max = 32)
  @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @NotBlank(message = "{product.price.name.required}")
  @Size(max = 255)
  @Schema(description = "价格表名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @NotBlank(message = "{product.price.status.required}")
  @Size(max = 32)
  @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
  private String status;

  @Schema(description = "自定义字段值")
  private List<BaseModuleFieldValue> moduleFields;

  @Schema(description = "子产品信息")
  private List<Map<String, Object>> products;
}
