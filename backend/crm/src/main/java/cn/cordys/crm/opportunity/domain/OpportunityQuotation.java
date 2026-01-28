package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;

/** 商机报价单; */
@Data
@Table(name = "opportunity_quotation")
public class OpportunityQuotation extends BaseModel {
  @Schema(description = "名称")
  private String name;

  @Schema(description = "商机id")
  private String opportunityId;

  @Schema(description = "有效期至")
  private Long untilTime;

  @Schema(description = "累计金额")
  private BigDecimal amount;

  @Schema(description = "审核状态")
  private String approvalStatus;

  @Schema(description = "组织ID")
  private String organizationId;
}
