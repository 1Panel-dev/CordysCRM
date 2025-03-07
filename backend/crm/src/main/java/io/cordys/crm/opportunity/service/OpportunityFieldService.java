package io.cordys.crm.opportunity.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.opportunity.domain.OpportunityField;
import io.cordys.crm.opportunity.domain.OpportunityFieldBlob;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityFieldService extends BaseResourceFieldService<OpportunityField, OpportunityFieldBlob> {

    @Resource
    private BaseMapper<OpportunityField> opportunityFieldMapper;
    @Resource
    private BaseMapper<OpportunityFieldBlob> opportunityFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.BUSINESS.getKey();
    }

    @Override
    protected BaseMapper<OpportunityField> getResourceFieldMapper() {
        return opportunityFieldMapper;
    }

    @Override
    protected BaseMapper<OpportunityFieldBlob> getResourceFieldBlobMapper() {
        return opportunityFieldBlobMapper;
    }
}
