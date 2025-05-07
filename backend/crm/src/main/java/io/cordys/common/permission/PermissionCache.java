package io.cordys.common.permission;

import io.cordys.common.dto.RoleDataScopeDTO;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.service.RoleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限缓存
 * @author jianxing
 */
@Component
public class PermissionCache {

    @Cacheable(value = "permission_cache", key = "#userId + ':' +  #orgId")
    public Set<String> getPermissionIds(String userId, String orgId) {
        RoleService roleService = CommonBeanFactory.getBean(RoleService.class);
        List<RoleDataScopeDTO> roleOptions = roleService.getRoleOptions(userId, orgId);
        List<String> roleIds = roleOptions.stream()
                .map(RoleDataScopeDTO::getId).collect(Collectors.toList());
        return roleService.getPermissionIds(roleIds);
    }

    @CacheEvict(value = "permission_cache", key = "#userId + ':' +  #orgId", beforeInvocation = true)
    public void clearCache(String userId, String orgId) {
        // do nothing
    }
}
