package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerContactResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<CustomerContact> contactMapper;

    @Override
    public String getFormType() {
        return FormKey.CUSTOMER.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        var customerContact = contactMapper.selectByPrimaryKey(resourceId);
        if (customerContact == null) {
            return null;
        }
        var context = new ResourceAccessContext();
        context.setOwnerId(customerContact.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return contactMapper.selectByIds(resourceIds).stream()
                .filter(customer -> customer.getOwner() != null)
                .collect(Collectors.toMap(CustomerContact::getId, CustomerContact::getOwner, (a, b) -> a));
    }
}
