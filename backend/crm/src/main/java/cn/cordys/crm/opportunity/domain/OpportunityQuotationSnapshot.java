package cn.cordys.crm.opportunity.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商机报价单快照;
 */
@Data
@Table(name = "opportunity_quotation_snapshot")
public class OpportunityQuotationSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "报价单id")
    private String quotationId;

    @Schema(description = "表单属性快照")
    private String quotationProp;

    @Schema(description = "表单值快照")
    private String quotationValue;
}