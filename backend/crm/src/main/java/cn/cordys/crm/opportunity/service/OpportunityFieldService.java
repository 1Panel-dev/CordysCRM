package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.opportunity.domain.OpportunityField;
import cn.cordys.crm.opportunity.domain.OpportunityFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityFieldService
    extends BaseResourceFieldService<OpportunityField, OpportunityFieldBlob> {

  @Resource private BaseMapper<OpportunityField> opportunityFieldMapper;
  @Resource private BaseMapper<OpportunityFieldBlob> opportunityFieldBlobMapper;

  @Override
  protected String getFormKey() {
    return FormKey.OPPORTUNITY.getKey();
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
