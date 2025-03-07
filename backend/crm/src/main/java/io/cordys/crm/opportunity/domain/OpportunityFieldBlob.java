package io.cordys.crm.opportunity.domain;

import io.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_field_blob")
public class OpportunityFieldBlob extends BaseResourceField {
}
