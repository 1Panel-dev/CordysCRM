package io.cordys.crm.system.service;

import io.cordys.common.constants.ModuleConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.response.ModuleDTO;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ModuleService {

	@Resource
	private BaseMapper<Module> moduleMapper;

	/**
	 * 获取系统模块配置列表
	 * @param request 请求参数
	 * @return 模块配置列表
	 */
	public List<ModuleDTO> getModuleList(ModuleRequest request) {
		LambdaQueryWrapper<Module> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Module::getOrganizationId, request.getOrganizationId());
		List<Module> modules = moduleMapper.selectListByLambda(queryWrapper);
		// translate i18n key
		return modules.stream().map(module -> {
			ModuleDTO moduleDTO = new ModuleDTO();
			BeanUtils.copyBean(moduleDTO, module);
			moduleDTO.setTranslateName(Translator.get(module.getName()));
			return moduleDTO;
		}).sorted(Comparator.comparing(ModuleDTO::getPos)).toList();
	}

	/**
	 * 单个模块开启或关闭
	 * @param id 模块ID
	 */
	public void switchModule(String id) {
		Module module = moduleMapper.selectByPrimaryKey(id);
		if (module == null) {
			throw new GenericException(Translator.get("module.not_exist"));
		}
		module.setEnable(module.getEnable() ^ 1);
		moduleMapper.updateById(module);
	}

	/**
	 * 初始化系统(组织或公司)模块数据
	 */
	public void initModule(String organizationId) {
		// init module data
		List<Module> modules = new ArrayList<>();
		AtomicLong pos = new AtomicLong(1L);
		Arrays.stream(ModuleConstants.values()).forEach(moduleConstant -> {
			Module module = new Module();
			module.setId(IDGenerator.nextStr());
			module.setName(moduleConstant.getKey());
			module.setOrganizationId(organizationId);
			module.setPos(pos.getAndIncrement());
			module.setCreateUser("admin");
			module.setCreateTime(System.currentTimeMillis());
			module.setUpdateUser("admin");
			module.setUpdateTime(System.currentTimeMillis());
			modules.add(module);
		});
		moduleMapper.batchInsert(modules);
	}
}
