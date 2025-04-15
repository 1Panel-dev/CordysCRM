package io.cordys.crm.system.service;

import io.cordys.common.constants.ModuleKey;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Module;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class SendModuleService {

    @Resource
    private BaseMapper<Module> moduleMapper;

    /**
     * 获取已开启的模块
     *
     * @return 已开启的模块列表
     */
    public List<String> getNoticeModules() {
        List<String> enabledModules = moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, OrganizationContext.getOrganizationId())
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey).distinct()
                .toList();

        List<String> modules = new ArrayList<>();
        for (String enabledModule : enabledModules) {
            if (StringUtils.equalsIgnoreCase(enabledModule, ModuleKey.BUSINESS.getKey())) {
                modules.add(NotificationConstants.Module.OPPORTUNITY);
            }
            if (StringUtils.equalsIgnoreCase(enabledModule, ModuleKey.CUSTOMER.getKey())) {
                modules.add(NotificationConstants.Module.CUSTOMER);
            }
            if (StringUtils.equalsIgnoreCase(enabledModule, ModuleKey.CLUE.getKey())) {
                modules.add(NotificationConstants.Module.CLUE);
            }

        }
        return modules;
    }
}
