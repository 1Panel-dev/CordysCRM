package cn.cordys.crm.order.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.order.domain.Order;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Order> orderMapper;

    @Override
    public String getFormType() {
        return FormKey.ORDER.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        Order order = orderMapper.selectByPrimaryKey(resourceId);
        if (order == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(order.getOwner());
        context.setApprovalStatus(order.getApprovalStatus());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectByIds(resourceIds).stream()
                .filter(order -> order.getOwner() != null)
                .collect(Collectors.toMap(Order::getId, Order::getOwner, (a, b) -> a));
    }
}
