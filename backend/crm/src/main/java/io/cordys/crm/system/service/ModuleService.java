package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.dto.RoleUserTreeNode;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.domain.OrganizationConfig;
import io.cordys.crm.system.domain.OrganizationConfigDetail;
import io.cordys.crm.system.dto.ModuleDTO;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.request.ModuleSortRequest;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.crm.system.mapper.ExtModuleMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ModuleService {

	@Resource
	private BaseMapper<Module> moduleMapper;
	@Resource
	private ExtModuleMapper extModuleMapper;
	@Resource
	private ExtDepartmentMapper extDepartmentMapper;
	@Resource
	private ExtUserRoleMapper extUserRoleMapper;
	@Resource
	private RoleService roleService;
	@Resource
	private DepartmentService departmentService;
	@Resource
	private BaseMapper<OrganizationConfig> organizationConfigMapper;
	@Resource
	private BaseMapper<OrganizationConfigDetail> organizationConfigDetailMapper;

	private static final String DEFAULT_ORGANIZATION_ID = "100001";

	/**
	 * 获取系统模块配置列表
	 * @param request 请求参数
	 * @return 模块配置列表
	 */
	public List<ModuleDTO> getModuleList(ModuleRequest request) {
		LambdaQueryWrapper<Module> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(Module::getOrganizationId, request.getOrganizationId());
		List<Module> modules = moduleMapper.selectListByLambda(queryWrapper);
		return modules.stream()
				.map(module -> {
					ModuleDTO dto = new ModuleDTO();
					BeanUtils.copyProperties(module, dto);
					dto.setDisabled(StringUtils.equals(dto.getModuleKey(), "dashboard") && !isDashboardEnable(request.getOrganizationId()));
					return dto;
				})
				.sorted(Comparator.comparing(ModuleDTO::getPos))
				.collect(Collectors.toList());
	}

	/**
	 * 单个模块开启或关闭
	 * @param id 模块ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, operator = "{#currentUser}")
	public void switchModule(String id, String currentUser) {
		Module module = moduleMapper.selectByPrimaryKey(id);
		if (module == null) {
			throw new GenericException(Translator.get("module.not_exist"));
		}
		module.setEnable(!module.getEnable());
		module.setUpdateUser(currentUser);
		module.setUpdateTime(System.currentTimeMillis());
		moduleMapper.updateById(module);

		//添加日志上下文
		Map<String, String> originalVal = new HashMap<>(1);
		originalVal.put("module.switch", !module.getEnable() ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
		Map<String, String> modifiedVal = new HashMap<>(1);
		modifiedVal.put("module.switch", module.getEnable() ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
		OperationLogContext.setContext(LogContextInfo.builder()
				.originalValue(originalVal)
				.resourceName(Translator.get(module.getModuleKey()) + "模块开关")
				.modifiedValue(modifiedVal)
				.resourceId(module.getId())
				.build());
	}

	/**
	 * 模块排序
	 * @param request 请求参数
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, operator = "{#currentUser}")
	public void sort(ModuleSortRequest request, String currentUser) {
		Module module = moduleMapper.selectByPrimaryKey(request.getDragModuleId());
		if (module == null) {
			throw new GenericException(Translator.get("module.not_exist"));
		}
		List<String> beforeKeys = getModuleSortKeys(module.getOrganizationId());
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
		dragModule.setUpdateUser(currentUser);
		dragModule.setUpdateTime(System.currentTimeMillis());
		moduleMapper.updateById(dragModule);
		List<String> afterKeys = getModuleSortKeys(module.getOrganizationId());

		//添加日志上下文
		Map<String, List<String>> originalVal = new HashMap<>(1);
		originalVal.put(Translator.get("module.main.nav"), beforeKeys);
		Map<String, Boolean> modifiedVal = new HashMap<>(1);
		originalVal.put(Translator.get("module.main.nav"), afterKeys);
		OperationLogContext.setContext(LogContextInfo.builder()
				.originalValue(originalVal)
				.resourceName(Translator.get("module.main.nav"))
				.modifiedValue(modifiedVal)
				.resourceId(module.getId())
				.build());
	}

	/**
	 * 获取带用户的信息的部门树
	 *
	 * @return List<DeptUserTreeNode>
	 */
	public List<DeptUserTreeNode> getDeptUserTree(String orgId) {
		List<DeptUserTreeNode> treeNodes = extDepartmentMapper.selectDeptUserTreeNode(orgId);
		List<DeptUserTreeNode> userNodes = extUserRoleMapper.selectUserDeptForOrg(orgId);
		userNodes = departmentService.sortByCommander(orgId, userNodes);
		userNodes.addAll(treeNodes);
		return BaseTreeNode.buildTree(userNodes);
	}

	/**
	 * 获取角色树
	 * @param orgId 组织ID
	 * @return 角色树
	 */
	public List<RoleUserTreeNode> getRoleTree(String orgId) {
		// 查询角色信息
		List<RoleListResponse> list = roleService.list(orgId);
		List<RoleUserTreeNode> treeNodes = list.stream().map((role) -> {
			RoleUserTreeNode roleNode = new RoleUserTreeNode();
			roleNode.setNodeType("ROLE");
			roleNode.setInternal(BooleanUtils.isTrue(role.getInternal()));
			roleNode.setId(role.getId());
			roleNode.setName(role.getName());
			return roleNode;
		}).collect(Collectors.toList());
		return BaseTreeNode.buildTree(treeNodes);
	}

	/**
	 * 初始化系统(组织或公司)模块数据
	 */
	public void initModule(String organizationId) {
		// init module data
		List<Module> modules = new ArrayList<>();
		AtomicLong pos = new AtomicLong(1L);
		Arrays.stream(ModuleKey.values()).forEach(moduleConstant -> {
			Module module = new Module();
			module.setId(IDGenerator.nextStr());
			module.setModuleKey(moduleConstant.getKey());
			module.setOrganizationId(organizationId);
			module.setEnable(true);
			module.setPos(pos.getAndIncrement());
			module.setCreateUser(InternalUser.ADMIN.getValue());
			module.setCreateTime(System.currentTimeMillis());
			module.setUpdateUser(InternalUser.ADMIN.getValue());
			module.setUpdateTime(System.currentTimeMillis());
			modules.add(module);
		});
		moduleMapper.batchInsert(modules);
	}

	public void initDefaultOrgModule() {
		initModule(DEFAULT_ORGANIZATION_ID);
	}

	/**
	 * 获取排序之后的模块菜单
	 * @param organizationId 组织ID
	 * @return 模块列表
	 */
	private List<String> getModuleSortKeys(String organizationId) {
		ModuleRequest request = new ModuleRequest();
		request.setOrganizationId(organizationId);
		List<ModuleDTO> moduleList = getModuleList(request);
		return moduleList.stream().map(Module::getModuleKey).toList();
	}

	/**
	 * 检查仪表盘开关
	 * @param organizationId 组织ID
	 * @return bool
	 */
	private boolean isDashboardEnable(String organizationId) {
		LambdaQueryWrapper<OrganizationConfig> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(OrganizationConfig::getOrganizationId, organizationId).eq(OrganizationConfig::getType, OrganizationConfigConstants.ConfigType.THIRD.name());
		OrganizationConfig config = organizationConfigMapper.selectListByLambda(queryWrapper).getFirst();
		if (config == null) {
			return false;
		}
		LambdaQueryWrapper<OrganizationConfigDetail> detailQueryWrapper = new LambdaQueryWrapper<>();
		detailQueryWrapper.eq(OrganizationConfigDetail::getConfigId, config.getId()).like(OrganizationConfigDetail::getType, DepartmentConstants.DE.name());
		OrganizationConfigDetail configDetail = organizationConfigDetailMapper.selectListByLambda(detailQueryWrapper).getFirst();
		return configDetail != null && configDetail.getEnable();
	}
}
