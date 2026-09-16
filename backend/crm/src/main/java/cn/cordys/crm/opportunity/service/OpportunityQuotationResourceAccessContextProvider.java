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
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OpportunityQuotationResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<OpportunityQuotation> quotationMapper;

    @Override
    public String getFormType() {
        return FormKey.QUOTATION.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        OpportunityQuotation quotation = quotationMapper.selectByPrimaryKey(resourceId);
        if (quotation == null || orgId == null || !orgId.equals(quotation.getOrganizationId())) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        // 与报价列表的 SELF 和部门数据范围一致，按报价创建人授权。
        context.setOwnerId(quotation.getCreateUser());
        context.setApprovalStatus(quotation.getApprovalStatus());
        return context;
    }

    @Override
    public boolean requiresBatchOwner() {
        return false;
    }

    @Override
    public Set<String> batchGetResourceIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty() || orgId == null) {
            return Set.of();
        }
        return quotationMapper.selectByIds(resourceIds).stream()
                .filter(quotation -> orgId.equals(quotation.getOrganizationId()))
                .map(OpportunityQuotation::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        // 不校验数据权限
        return Map.of();
    }
}
