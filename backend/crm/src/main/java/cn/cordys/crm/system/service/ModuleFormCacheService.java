package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.field.base.SimpleField;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
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
	public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
		ModuleFormConfigDTO config = CommonBeanFactory.getBean(this.getClass()).getConfig(formKey, organizationId);
		ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
		businessModuleFormConfig.setFormProp(config.getFormProp());

		// 获取特殊的业务字段
		Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

		businessModuleFormConfig.setFields(
				config.getFields()
						.stream()
						.peek(moduleFieldDTO -> {
							BusinessModuleField businessModuleFieldEnum = businessModuleFieldMap.get(moduleFieldDTO.getInternalKey());
							if (businessModuleFieldEnum != null) {
								// 设置特殊的业务字段 key
								moduleFieldDTO.setBusinessKey(businessModuleFieldEnum.getBusinessKey());
								moduleFieldDTO.setDisabledProps(businessModuleFieldEnum.getDisabledProps());
							}
						})
						.toList()
		);
		return businessModuleFormConfig;
	}

	/**
	 * 获取MCP表单需要的字段
	 * @param formKey 表单Key
	 * @param organizationId 组织ID
	 * @return 字段列表
	 */
	public List<SimpleField> getMcpFields(String formKey, String organizationId) {
		ModuleFormConfigDTO businessFormConfig = getBusinessFormConfig(formKey, organizationId);
		return businessFormConfig.getFields().stream().filter(BaseField::canImport).map(field -> {
			SimpleField simpleField = new SimpleField();
			simpleField.setId(field.getId());
			simpleField.setBusinessKey(field.getBusinessKey());
			simpleField.setName(field.getName());
			simpleField.setType(field.getType());
			simpleField.setRequired(field.needRequireCheck());
			simpleField.setUnique(field.needRepeatCheck());
			if (field instanceof HasOption fieldWithOption) {
				simpleField.setOptions(fieldWithOption.getOptions().stream().map(OptionProp::getLabel).toList());
			}
			return simpleField;
		}).toList();
	}
}
