package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpportunityResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Opportunity> opportunityMapper;

    @Override
    public String getFormType() {
        return FormKey.OPPORTUNITY.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(resourceId);
        if (opportunity == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(opportunity.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return opportunityMapper.selectByIds(resourceIds).stream()
                .filter(opportunity -> opportunity.getOwner() != null)
                .collect(Collectors.toMap(Opportunity::getId, Opportunity::getOwner, (a, b) -> a));
    }
}
