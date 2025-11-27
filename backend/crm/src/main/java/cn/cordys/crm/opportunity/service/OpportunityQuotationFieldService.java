package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationField;
import cn.cordys.crm.opportunity.domain.OpportunityQuotationFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author guoyuqi
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityQuotationFieldService extends BaseResourceFieldService<OpportunityQuotationField, OpportunityQuotationFieldBlob> {

    @Resource
    private BaseMapper<OpportunityQuotationField> opportunityQuotationFieldMapper;
    @Resource
    private BaseMapper<OpportunityQuotationFieldBlob> opportunityQuotationFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.QUOTATION.getKey();
    }

    @Override
    protected BaseMapper<OpportunityQuotationField> getResourceFieldMapper() {
        return opportunityQuotationFieldMapper;
    }

    @Override
    protected BaseMapper<OpportunityQuotationFieldBlob> getResourceFieldBlobMapper() {
        return opportunityQuotationFieldBlobMapper;
    }
}
