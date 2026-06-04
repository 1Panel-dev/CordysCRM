package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomerResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    public String getFormType() {
        return FormKey.CUSTOMER.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        var customer = customerMapper.selectByPrimaryKey(resourceId);
        if (customer == null) {
            return null;
        }
        var context = new ResourceAccessContext();
        context.setOwnerId(customer.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectByIds(resourceIds).stream()
                .filter(customer -> customer.getOwner() != null)
                .collect(Collectors.toMap(Customer::getId, Customer::getOwner, (a, b) -> a));
    }
}
