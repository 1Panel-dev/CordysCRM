package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "opportunity_field")
public class OpportunityField extends BaseResourceField {
}
