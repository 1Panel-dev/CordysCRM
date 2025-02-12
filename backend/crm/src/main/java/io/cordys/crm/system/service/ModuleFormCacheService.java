package io.cordys.crm.system.service;

import io.cordys.crm.system.dto.request.ModuleFormRequest;
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
	 * 保存模块字段集合并更新缓存
	 * @param saveParam 保存参数
	 * @param currentUserId 当前用户ID
	 * @return
	 */
	@CachePut(value = "formCache", key = "#saveParam.formKey", unless = "#result == null")
	public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId) {
		return moduleFormService.save(saveParam, currentUserId);
	}

	/**
	 * 获取模块字段集合(缓存)
	 * @param request 请求参数
	 * @return 字段集合
	 */
	@Cacheable(value = "formCache", key = "#request.formKey", unless = "#result == null")
	public ModuleFormConfigDTO getConfig(ModuleFormRequest request) {
		return moduleFormService.getConfig(request);
	}
}
