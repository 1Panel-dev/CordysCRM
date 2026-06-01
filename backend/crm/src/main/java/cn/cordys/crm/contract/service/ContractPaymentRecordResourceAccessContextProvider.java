package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContractPaymentRecordResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<ContractPaymentRecord> contractPaymentRecordMapper;

    @Override
    public String getFormType() {
        return FormKey.CONTRACT_PAYMENT_RECORD.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        ContractPaymentRecord record = contractPaymentRecordMapper.selectByPrimaryKey(resourceId);
        if (record == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(record.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return contractPaymentRecordMapper.selectByIds(resourceIds).stream()
                .filter(record -> record.getOwner() != null)
                .collect(Collectors.toMap(ContractPaymentRecord::getId, ContractPaymentRecord::getOwner, (a, b) -> a));
    }
}
