package cn.cordys.crm.system.service;

import cn.cordys.common.constants.ModuleKey;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Module;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
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
            if (Strings.CI.equals(enabledModule, ModuleKey.BUSINESS.getKey())) {
                modules.add(NotificationConstants.Module.OPPORTUNITY);
            }
            if (Strings.CI.equals(enabledModule, ModuleKey.CUSTOMER.getKey())) {
                modules.add(NotificationConstants.Module.CUSTOMER);
            }
            if (Strings.CI.equals(enabledModule, ModuleKey.CLUE.getKey())) {
                modules.add(NotificationConstants.Module.CLUE);
            }
            if (Strings.CI.equals(enabledModule, NotificationConstants.Module.CONTRACT)) {
                modules.add(NotificationConstants.Module.CONTRACT);
            }
        }
        //这里是系统模块的通知，默认开启的
        modules.add(NotificationConstants.Module.SYSTEM);
        return modules;
    }
}
