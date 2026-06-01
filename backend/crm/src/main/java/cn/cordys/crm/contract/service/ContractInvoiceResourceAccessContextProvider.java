package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.contract.domain.ContractInvoice;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContractInvoiceResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<ContractInvoice> contractInvoiceMapper;

    @Override
    public String getFormType() {
        return FormKey.INVOICE.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        ContractInvoice invoice = contractInvoiceMapper.selectByPrimaryKey(resourceId);
        if (invoice == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(invoice.getOwner());
        context.setApprovalStatus(invoice.getApprovalStatus());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return contractInvoiceMapper.selectByIds(resourceIds).stream()
                .filter(invoice -> invoice.getOwner() != null)
                .collect(Collectors.toMap(ContractInvoice::getId, ContractInvoice::getOwner, (a, b) -> a));
    }
}
