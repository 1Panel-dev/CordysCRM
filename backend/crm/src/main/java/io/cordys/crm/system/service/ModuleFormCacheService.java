package io.cordys.crm.system.service;

import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
	@CachePut(value = "formCache", key = "#currentOrgId + ':' + #saveParam.formKey", unless = "#result == null")
	public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
		return moduleFormService.save(saveParam, currentUserId, currentOrgId);
	}

	/**
	 * 获取表单配置(缓存)
	 * @param formKey 表单Key
	 * @param currentOrgId 当前组织ID
	 * @return 表单配置
	 */
	@Cacheable(value = "formCache", key = "#currentOrgId + ':' + #formKey", unless = "#result == null")
	public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
		return moduleFormService.getConfig(formKey, currentOrgId);
	}
}
