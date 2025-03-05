package io.cordys.crm.system.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.BusinessModuleFieldDTO;
import io.cordys.crm.system.dto.response.BusinessModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ModuleFormCacheService {

	@Resource
	private ModuleFormService moduleFormService;

	/**
	 * 保存表单配置并更新缓存
	 * @param saveParam 保存参数
	 * @param currentUserId 当前用户ID
	 * @return 表单配置
	 */
	@CachePut(value = "form_cache", key = "#currentOrgId + ':' + #saveParam.formKey", unless = "#result == null")
	public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
		return moduleFormService.save(saveParam, currentUserId, currentOrgId);
	}

	/**
	 * 获取表单配置(缓存)
	 * @param formKey 表单Key
	 * @param currentOrgId 当前组织ID
	 * @return 表单配置
	 */
	@Cacheable(value = "form_cache", key = "#currentOrgId + ':' + #formKey", unless = "#result == null")
	public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
		return moduleFormService.getConfig(formKey, currentOrgId);
	}

	/**
	 * 获取业务表单配置
	 * @param formKey 表单Key
	 * @param organizationId 组织ID
	 * @return 表单配置
	 */
	public BusinessModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
		ModuleFormConfigDTO config = CommonBeanFactory.getBean(this.getClass()).getConfig(formKey, organizationId);
		BusinessModuleFormConfigDTO businessModuleFormConfig = new BusinessModuleFormConfigDTO();
		businessModuleFormConfig.setFormProp(config.getFormProp());

		// 获取特殊的业务字段
		Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

		businessModuleFormConfig.setFields(
				config.getFields()
						.stream()
						.map(moduleFieldDTO -> {
							BusinessModuleFieldDTO businessModuleField = BeanUtils.copyBean(new BusinessModuleFieldDTO(), moduleFieldDTO);
							BusinessModuleField businessModuleFieldEnum = businessModuleFieldMap.get(businessModuleField.getInternalKey());
							if (businessModuleFieldEnum != null) {
								// 设置特殊的业务字段 key
								businessModuleField.setBusinessKey(businessModuleFieldEnum.getBusinessKey());
								businessModuleField.setDisabledProps(businessModuleFieldEnum.getDisabledProps());
							}
							return businessModuleField;
						})
						.toList()
		);
		return businessModuleFormConfig;
	}
}
