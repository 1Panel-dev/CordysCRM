package cn.cordys.crm.product.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductPriceResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<ProductPrice> productPriceMapper;

    @Override
    public String getFormType() {
        return FormKey.PRICE.getKey();
    }

    @Override
    public boolean requiresOwner() {
        return false;
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        if (StringUtils.isBlank(orgId)) {
            return null;
        }
        ProductPrice price = productPriceMapper.selectByPrimaryKey(resourceId);
        return price != null && orgId.equals(price.getOrganizationId()) ? new ResourceAccessContext() : null;
    }

    @Override
    public Set<String> batchGetResourceIds(List<String> resourceIds, String orgId) {
        if (StringUtils.isBlank(orgId) || resourceIds == null || resourceIds.isEmpty()) {
            return Set.of();
        }
        return productPriceMapper.selectByIds(resourceIds).stream()
                .filter(price -> orgId.equals(price.getOrganizationId()))
                .map(ProductPrice::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        // 价格表按组织共享，不使用创建人或其他用户代替负责人。
        return Map.of();
    }
}
