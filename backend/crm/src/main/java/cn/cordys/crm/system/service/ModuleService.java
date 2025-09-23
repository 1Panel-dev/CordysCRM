package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.Module;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.ModuleDTO;
import cn.cordys.crm.system.dto.request.ModuleRequest;
import cn.cordys.crm.system.dto.request.ModuleSortRequest;
import cn.cordys.crm.system.dto.response.RoleListResponse;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtModuleMapper;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ModuleService {

    private static final String DEFAULT_ORGANIZATION_ID = "100001";
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

    /**
     * 获取系统模块配置列表
     *
     * @param request 请求参数
     *
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
                    dto.setDisabled(Strings.CS.equals(dto.getModuleKey(), "dashboard") && !isDashboardEnabled(request.getOrganizationId()));
                    return dto;
                })
                .sorted(Comparator.comparing(ModuleDTO::getPos))
                .collect(Collectors.toList());
    }

    /**
     * 获取表单集合
     *
     * @return 表单集合
     */
    public List<OptionDTO> getFormList() {
        return Arrays.stream(FormKey.values()).map(formKey -> new OptionDTO(formKey.getKey(), Translator.get(formKey.getKey()))).toList();
    }

    /**
     * 单个模块开启或关闭
     *
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
     *
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
        originalVal.put("sort", beforeKeys);
        Map<String, List<String>> modifiedVal = new HashMap<>(1);
        modifiedVal.put("sort", afterKeys);
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
     *
     * @param orgId 组织ID
     *
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
     *
     * @param organizationId 组织ID
     *
     * @return 模块列表
     */
    private List<String> getModuleSortKeys(String organizationId) {
        ModuleRequest request = new ModuleRequest();
        request.setOrganizationId(organizationId);
        List<ModuleDTO> moduleList = getModuleList(request);
        return moduleList.stream().map(Module::getModuleKey).toList();
    }

    /**
     * 检查仪表盘功能是否启用
     *
     * @param organizationId 组织ID
     *
     * @return 仪表盘是否启用
     */
    private boolean isDashboardEnabled(String organizationId) {
        // 查询第三方配置
        LambdaQueryWrapper<OrganizationConfig> configQuery = new LambdaQueryWrapper<>();
        configQuery.eq(OrganizationConfig::getOrganizationId, organizationId)
                .eq(OrganizationConfig::getType, OrganizationConfigConstants.ConfigType.THIRD.name());

        // 直接获取第一个配置或返回null
        OrganizationConfig config = organizationConfigMapper.selectListByLambda(configQuery)
                .stream()
                .findFirst()
                .orElse(null);

        if (config == null) {
            return false;
        }

        // 查询仪表盘配置详情
        LambdaQueryWrapper<OrganizationConfigDetail> detailQuery = new LambdaQueryWrapper<>();
        detailQuery.eq(OrganizationConfigDetail::getConfigId, config.getId())
                .like(OrganizationConfigDetail::getType, "DE_BOARD");

        // 查找配置并判断是否启用
        return organizationConfigDetailMapper.selectListByLambda(detailQuery)
                .stream()
                .findFirst()
                .map(OrganizationConfigDetail::getEnable)
                .orElse(false);
    }
}
