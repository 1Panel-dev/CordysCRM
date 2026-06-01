package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContractResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Contract> contractMapper;

    @Override
    public String getFormType() {
        return FormKey.CONTRACT.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        Contract contract = contractMapper.selectByPrimaryKey(resourceId);
        if (contract == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(contract.getOwner());
        context.setApprovalStatus(contract.getApprovalStatus());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return contractMapper.selectByIds(resourceIds).stream()
                .filter(contract -> contract.getOwner() != null)
                .collect(Collectors.toMap(Contract::getId, Contract::getOwner, (a, b) -> a));
    }
}
