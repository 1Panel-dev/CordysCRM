package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商机报价单审批;
 */
@Data
@Table(name = "opportunity_quotation_approval")
public class OpportunityQuotationApproval implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "商机报价单id")
    private String quotationId;

    @Schema(description = "审核状态")
    private String approvalStatus;
}
