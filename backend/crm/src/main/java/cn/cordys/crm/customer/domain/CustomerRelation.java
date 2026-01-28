package cn.cordys.crm.customer.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 客户关系
 *
 * @author jianxing
 * @date 2025-03-12 17:19:16
 */
@Data
@Table(name = "customer_relation")
public class CustomerRelation {

  @Schema(description = "id")
  private String id;

  @Schema(description = "客户ID(集团)")
  private String sourceCustomerId;

  @Schema(description = "客户ID(子公司)")
  private String targetCustomerId;

  @Schema(description = "创建时间")
  private Long createTime;
}
