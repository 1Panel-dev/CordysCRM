package cn.cordys.crm.product.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 产品价格表
 *
 * @author song-cc-rock
 */
@Data
@Table(name = "product_price")
public class ProductPrice extends BaseModel {

  @Schema(description = "价格表名称")
  private String name;

  @Schema(description = "状态")
  private String status;

  @Schema(description = "排序")
  private Long pos;

  @Schema(description = "组织ID")
  private String organizationId;
}
