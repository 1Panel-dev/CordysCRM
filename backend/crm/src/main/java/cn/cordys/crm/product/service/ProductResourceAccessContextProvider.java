package cn.cordys.crm.product.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Product> productMapper;

    @Override
    public String getFormType() {
        return FormKey.PRODUCT.getKey();
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
        Product product = productMapper.selectByPrimaryKey(resourceId);
        return product != null && orgId.equals(product.getOrganizationId()) ? new ResourceAccessContext() : null;
    }

    @Override
    public Set<String> batchGetResourceIds(List<String> resourceIds, String orgId) {
        if (StringUtils.isBlank(orgId) || resourceIds == null || resourceIds.isEmpty()) {
            return Set.of();
        }
        return productMapper.selectByIds(resourceIds).stream()
                .filter(product -> orgId.equals(product.getOrganizationId()))
                .map(Product::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        // 产品按组织共享，不使用创建人或其他用户代替负责人。
        return Map.of();
    }
}
