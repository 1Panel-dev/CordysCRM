package io.cordys.crm.search.service;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.domain.Module;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;

import java.util.List;

public abstract class BaseSearchService<T extends BasePageRequest, R> {

    @Resource
    private BaseMapper<Module> moduleMapper;

    abstract public PagerWithOption<List<R>> startSearch(T request, String orgId, String userId);


    /**
     * 获取当前组织下所有已开启的模块key
     *
     * @return List<String>EnabledModuleKeys
     */
    public List<String> getEnabledModules() {
        return moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, OrganizationContext.getOrganizationId())
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey)
                .toList();
    }
}
