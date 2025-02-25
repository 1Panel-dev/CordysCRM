package io.cordys.crm.opportunity.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_field")
public class OpportunityField {
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "商机id")
    private String opportunityId;

    @Schema(description = "自定义属性id")
    private String fieldId;

    @Schema(description = "自定义属性值")
    private Object fieldValue;
}
