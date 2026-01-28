package cn.cordys.crm.opportunity.domain;

import cn.cordys.common.domain.BaseResourceSubField;
import jakarta.persistence.Table;
import lombok.Data;

/** 商机报价单自定义属性大文本; */
@Data
@Table(name = "opportunity_quotation_field_blob")
public class OpportunityQuotationFieldBlob extends BaseResourceSubField {}
