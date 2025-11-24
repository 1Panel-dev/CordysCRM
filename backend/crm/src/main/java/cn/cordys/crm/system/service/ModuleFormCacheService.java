package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
@Service
public class ModuleFormCacheService {

    @Resource
    private ModuleFormService moduleFormService;

    /**
     * 保存表单配置并更新缓存
     *
     * @param saveParam     保存参数
     * @param currentUserId 当前用户ID
     *
     * @return 表单配置
     */
    @CachePut(value = "form_cache", key = "#currentOrgId + ':' + #saveParam.formKey", unless = "#result == null")
    public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
        return moduleFormService.save(saveParam, currentUserId, currentOrgId);
    }

    /**
     * 获取表单配置(缓存)
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
     *
     * @return 表单配置
     */
    @Cacheable(value = "form_cache", key = "#currentOrgId + ':' + #formKey", unless = "#result == null")
    public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
        return moduleFormService.getConfig(formKey, currentOrgId);
    }

    /**
     * 获取业务表单配置
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     *
     * @return 表单配置
     */
    public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
        ModuleFormConfigDTO config = Objects.requireNonNull(CommonBeanFactory.getBean(this.getClass())).getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

		// 设置业务字段参数
		businessModuleFormConfig.setFields(config.getFields().stream()
				.peek(moduleFormService::setFieldBusinessParam)
				.collect(Collectors.toList())
		);
        return businessModuleFormConfig;
    }
}
