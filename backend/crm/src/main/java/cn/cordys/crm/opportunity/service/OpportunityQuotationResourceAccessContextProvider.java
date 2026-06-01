package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.opportunity.domain.OpportunityQuotation;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OpportunityQuotationResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<OpportunityQuotation> quotationMapper;

    @Override
    public String getFormType() {
        return FormKey.OPPORTUNITY.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        OpportunityQuotation quotation = quotationMapper.selectByPrimaryKey(resourceId);
        if (quotation == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setApprovalStatus(quotation.getApprovalStatus());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        // 不校验数据权限
        return Map.of();
    }
}
