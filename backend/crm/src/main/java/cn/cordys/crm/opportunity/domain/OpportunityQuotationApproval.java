package cn.cordys.crm.opportunity.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/** 商机报价单审批; */
@Data
@Table(name = "opportunity_quotation_approval")
public class OpportunityQuotationApproval implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(description = "商机报价单id")
  private String quotationId;

  @Schema(description = "审核状态")
  private String approvalStatus;

  @Schema(description = "创建人")
  private String createUser;

  @Schema(description = "修改人")
  private String updateUser;

  @Schema(description = "创建时间")
  private Long createTime;

  @Schema(description = "更新时间")
  private Long updateTime;
}
