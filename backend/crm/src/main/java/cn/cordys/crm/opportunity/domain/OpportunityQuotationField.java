package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 商机报价单自定义属性;
 */
@Data
@Table(name = "opportunity_quotation_field")
public class OpportunityQuotationField extends BaseResourceSubField {

}
