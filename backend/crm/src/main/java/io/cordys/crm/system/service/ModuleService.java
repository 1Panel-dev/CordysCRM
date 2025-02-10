package io.cordys.crm.system.service;

import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.request.ModuleSortRequest;
import io.cordys.crm.system.dto.response.ModuleDTO;
import io.cordys.crm.system.mapper.ExtModuleMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ModuleService {

	@Resource
	private BaseMapper<Module> moduleMapper;
	@Resource
	private ExtModuleMapper extModuleMapper;

	/**
	 * 获取系统模块配置列表
	 * @param request 请求参数
	 * @return 模块配置列表
	 */
	public List<ModuleDTO> getModuleList(ModuleRequest request) {
		LambdaQueryWrapper<Module> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Module::getOrganizationId, request.getOrganizationId());
		List<Module> modules = moduleMapper.selectListByLambda(queryWrapper);
		return modules.stream().map(module -> {
			ModuleDTO moduleDTO = new ModuleDTO();
			BeanUtils.copyBean(moduleDTO, module);
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
		module.setEnable(!module.getEnable());
		moduleMapper.updateById(module);
	}

	/**
	 * 模块排序
	 * @param request 请求参数
	 */
	public void sort(ModuleSortRequest request) {
		if (request.getStart() < request.getEnd()) {
			// start < end, 区间模块上移, pos - 1
			extModuleMapper.moveUpModule(request.getStart(), request.getEnd());
		} else {
			// start > end, 区间模块下移, pos + 1
			extModuleMapper.moveDownModule(request.getEnd(), request.getStart());
		}
		Module dragModule = new Module();
		dragModule.setId(request.getDragModuleId());
		dragModule.setPos(request.getEnd());
		moduleMapper.updateById(dragModule);
	}

	/**
	 * 初始化系统(组织或公司)模块数据
	 */
	@Transactional(rollbackFor = Exception.class)
	public void initModule(String organizationId) {
		// init module data
		List<Module> modules = new ArrayList<>();
		AtomicLong pos = new AtomicLong(1L);
		Arrays.stream(ModuleKey.values()).forEach(moduleConstant -> {
			Module module = new Module();
			module.setId(IDGenerator.nextStr());
			module.setKey(moduleConstant.getKey());
			module.setOrganizationId(organizationId);
			module.setPos(pos.getAndIncrement());
			module.setCreateUser(InternalUser.ADMIN.getValue());
			module.setCreateTime(System.currentTimeMillis());
			module.setUpdateUser(InternalUser.ADMIN.getValue());
			module.setUpdateTime(System.currentTimeMillis());
			modules.add(module);
		});
		moduleMapper.batchInsert(modules);
	}

	public Module getModuleByKey(ModuleKey moduleConstants, String organizationId) {
		Module module = new Module();
		module.setKey(moduleConstants.getKey());
		module.setOrganizationId(organizationId);
		return moduleMapper.select(module).getFirst();
	}
}
