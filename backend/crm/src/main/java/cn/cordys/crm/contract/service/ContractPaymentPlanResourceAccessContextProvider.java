package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContractPaymentPlanResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;

    @Override
    public String getFormType() {
        return FormKey.CONTRACT_PAYMENT_PLAN.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        ContractPaymentPlan plan = contractPaymentPlanMapper.selectByPrimaryKey(resourceId);
        if (plan == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(plan.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return contractPaymentPlanMapper.selectByIds(resourceIds).stream()
                .filter(plan -> plan.getOwner() != null)
                .collect(Collectors.toMap(ContractPaymentPlan::getId, ContractPaymentPlan::getOwner, (a, b) -> a));
    }
}
