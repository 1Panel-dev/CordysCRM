package cn.cordys.crm.integration.dataease.service;

import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.constants.ThirdConstants;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.dataease.DataEaseClient;
import cn.cordys.crm.integration.dataease.constanst.DataScopeDeptVariable;
import cn.cordys.crm.integration.dataease.constanst.DataScopeVariable;
import cn.cordys.crm.integration.dataease.dto.*;
import cn.cordys.crm.integration.dataease.dto.request.*;
import cn.cordys.crm.system.domain.RolePermission;
import cn.cordys.crm.system.domain.RoleScopeDept;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.crm.system.dto.response.RoleListResponse;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.security.UserDTO;
import com.fit2cloud.quartz.anno.QuartzScheduled;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Service
public class DataEaseSyncService {
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtOrganizationMapper extOrganizationMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private IntegrationConfigService integrationConfigService;

    public static final String NONE_DATA_SCOPE = "NONE";


    @QuartzScheduled(cron = "0 0 0 * * ?")
    public void syncDataEase() {
        Set<String> orgIds = extOrganizationMapper.selectAllOrganizationIds();
        // 同步角色
        for (String orgId : orgIds) {
            LogUtils.info("定时同步DataEase数据，组织ID: " + orgId);
            syncDataEase(orgId);
        }
    }

    public void syncDataEase(String orgId) {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        ThirdConfigurationDTO thirdConfig;
        try {
            thirdConfig = integrationConfigService.getThirdConfigByType(ThirdConstants.ThirdDetailType.DE_BOARD.name(), orgId);
        } catch (Exception e) {
            LogUtils.error("获取DataEase配置失败，组织ID: " + orgId, e);
            return;
        }
        if (thirdConfig == null || !BooleanUtils.isTrue(thirdConfig.getDeModuleEmbedding())
                || StringUtils.isAnyBlank(thirdConfig.getDeAccessKey(), thirdConfig.getDeSecretKey(), thirdConfig.getDeOrgID(), thirdConfig.getRedirectUrl())) {
            return;
        }
        try {
            syncDataEase(orgId, thirdConfig);
        } catch (Exception e) {
            LogUtils.error(e);
            throw e;
        }
    }

    public void syncDataEase(String orgId, ThirdConfigurationDTO thirdConfig) {
        DataEaseClient dataEaseClient = new DataEaseClient(thirdConfig);

        DeTempResourceDTO deTempResourceDTO = new DeTempResourceDTO();
        deTempResourceDTO.setDataEaseClient(dataEaseClient);
        deTempResourceDTO.setCrmOrgId(orgId);
        deTempResourceDTO.setDeOrgId(thirdConfig.getDeOrgID());

        // 手动切到 DE 组织，DE 接口设计不是很好，接口调用会受到页面切组织的影响，后天手动切组织，降低影响
        dataEaseClient.switchOrg(deTempResourceDTO.getDeOrgId());
        syncSysVariable(deTempResourceDTO);

        // 获取当前组织下的所有角色
        List<RoleListResponse> crmRoles = roleService.getRoleListResponses(orgId);
        deTempResourceDTO.setCrmRoles(crmRoles);

        dataEaseClient.switchOrg(deTempResourceDTO.getDeOrgId());
        syncRole(deTempResourceDTO);

        syncUser(deTempResourceDTO);
    }

    private void syncUser(DeTempResourceDTO deTempResourceDTO) {
        List<UserDTO> crmUsers = extUserMapper.selectNameAndEmail(deTempResourceDTO.getCrmOrgId());
        deTempResourceDTO.setCrmUsers(crmUsers);

        // 记录CRM用户ID和用户的映射关系
        Map<String, UserDTO> crmUserMap = crmUsers.stream()
                .collect(Collectors.toMap(UserDTO::getId, Function.identity()));

        List<RoleListResponse> crmRoles = deTempResourceDTO.getCrmRoles();
        DataEaseClient dataEaseClient = deTempResourceDTO.getDataEaseClient();

        // 获取所有的角色
        List<String> roleIds = crmRoles.stream()
                .map(RoleListResponse::getId)
                .collect(Collectors.toList());

        // 角色名称集合
        Set<String> roleNameSet = crmRoles.stream()
                .map(RoleListResponse::getName)
                .collect(Collectors.toSet());

        setSyncUserTempParam(deTempResourceDTO, roleIds);

        int pageNum = 1;
        int pageSize = 20;
        // 记录DE的用户ID
        List<String> deUserIds = new ArrayList<>();
        deTempResourceDTO.setDeUserIds(deUserIds);
        do {
            dataEaseClient.switchOrg(deTempResourceDTO.getDeOrgId());
            PageDTO<UserPageDTO> userPage = dataEaseClient.listUserPage(pageNum, pageSize);
            List<UserPageDTO> users = userPage.getRecords();
            for (UserPageDTO user : users) {
                deUserIds.add(user.getAccount());
                updateUser(deTempResourceDTO, crmUserMap, roleNameSet, user);
            }
            if (userPage.getRecords().size() < pageSize) {
                break;
            }
        } while (true);

        addUsers(deTempResourceDTO);
    }

    private void setSyncUserTempParam(DeTempResourceDTO deTempResourceDTO, List<String> roleIds) {
        List<RoleListResponse> crmRoles = deTempResourceDTO.getCrmRoles();

        // 获取用户下的角色信息
        Map<String, List<UserRole>> userRoleMap = roleService.getUserRoleByRoleIds(roleIds)
                .stream()
                .collect(Collectors.groupingBy(UserRole::getUserId));
        deTempResourceDTO.setCrmUserRoleMap(userRoleMap);

        // 获取自定义部门角色对应的部门ID
        Map<String, List<String>> customDeptRoleDeptMap = getCustomDeptRoleDeptMap(crmRoles);
        deTempResourceDTO.setCustomDeptRoleDeptMap(customDeptRoleDeptMap);

        // 获取部门树
        List<BaseTreeNode> tree = departmentService.getTree(deTempResourceDTO.getCrmOrgId());
        deTempResourceDTO.setDeptTree(tree);

        Map<String, RoleListResponse> crmRoleMap = crmRoles.stream()
                .collect(Collectors.toMap(RoleListResponse::getId, Function.identity()));
        deTempResourceDTO.setCrmRoleMap(crmRoleMap);

        // 记录角色和权限的映射关系
        Map<String, Set<String>> rolePermissionMap = getRolePermissionMap(roleIds);
        deTempResourceDTO.setRolePermissionMap(rolePermissionMap);
    }

    private void updateUser(DeTempResourceDTO deTempResourceDTO, Map<String, UserDTO> crmUserMap, Set<String> roleNameSet,
                            UserPageDTO deUser) {
        UserDTO crmUser = crmUserMap.get(deUser.getAccount());
        DataEaseClient dataEaseClient = deTempResourceDTO.getDataEaseClient();
        Map<String, RoleDTO> roleMap = deTempResourceDTO.getDeRoleMap();
        Map<String, SysVariableDTO> sysVariableMap = deTempResourceDTO.getSysVariableMap();
        Map<String, Map<String, String>> variableValueMap = deTempResourceDTO.getVariableValueMap();
        Map<String, RoleListResponse> crmRoleMap = deTempResourceDTO.getCrmRoleMap();
        List<BaseTreeNode> tree = deTempResourceDTO.getDeptTree();
        Map<String, Set<String>> rolePermissionMap = deTempResourceDTO.getRolePermissionMap();
        Map<String, List<String>> customDeptRoleDeptMap = deTempResourceDTO.getCustomDeptRoleDeptMap();
        Map<String, List<UserRole>> userRoleMap = deTempResourceDTO.getCrmUserRoleMap();

        if (crmUser == null) {
            // 不在crm的用户，不处理
            return;
        }

        String userId = crmUser.getId();
        List<OptionDTO> roleItems = deUser.getRoleItems();

        List<RoleListResponse> userCrmRoles = getUserCrmRoles(userRoleMap, crmRoleMap, userId);

        UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
        userUpdateRequest.setId(deUser.getId());
        boolean needUpdate = false;

        if (!Strings.CS.equals(deUser.getName(), crmUser.getName()) || !Strings.CS.equals(deUser.getEmail(), crmUser.getEmail())) {
            needUpdate = true;
            userUpdateRequest.setName(crmUser.getName());
            userUpdateRequest.setEmail(crmUser.getEmail());
        } else {
            // 设置为DE原值
            userUpdateRequest.setName(deUser.getName());
            userUpdateRequest.setEmail(deUser.getEmail());
        }

        // 如果该用户有crm中的角色不存在于DE，则需要更新
        Set<String> crmRoleNames = userCrmRoles.stream()
                .map(RoleListResponse::getName)
                .collect(Collectors.toSet());
        Set<String> deRoleNames = roleItems.stream()
                .map(OptionDTO::getName)
                .collect(Collectors.toSet());

        // 获取DE中存在的CRM角色
        Set<String> linkDeRoleNames = deRoleNames.stream()
                .filter(deRoleName -> roleNameSet.contains(deRoleName))
                .collect(Collectors.toSet());
        if (!linkDeRoleNames.equals(crmRoleNames)) {
            // 如果DE中的角色不包含CRM中的角色，则需要更新
            needUpdate = true;
            List<String> updateRoleIds = crmRoleNames.stream()
                    .map(crmRoleName -> roleMap.get(crmRoleName).getId())
                    .collect(Collectors.toList());
            // DE中非CRM的角色保留
            List<String> deRoleIds = deRoleNames.stream()
                    .filter(deRoleName -> !roleNameSet.contains(deRoleName))
                    .map(crmRoleName -> roleMap.get(crmRoleName).getId())
                    .collect(Collectors.toList());
            updateRoleIds.addAll(deRoleIds);
            userUpdateRequest.setRoleIds(updateRoleIds);
        } else {
            // 设置为DE原值
            List<String> roleIds = roleItems.stream().map(OptionDTO::getId).toList();
            userUpdateRequest.setRoleIds(roleIds);
        }

        List<UserCreateRequest.Variable> variables = new ArrayList<>();
        Map<String, String> userVariablePairs = extractUserVariablePairs(deUser.getSysVariable());
        UserVariableTempDTO userVariableTempDTO = getUserVariableTempDTO(userCrmRoles, customDeptRoleDeptMap,
                tree, rolePermissionMap, crmUser);

        for (DataScopeVariable value : DataScopeVariable.values()) {
            String dataScopeValue = userVariablePairs.get(value.name());
            String crmDataScope = userVariableTempDTO.getScopeValueMap().get(value.name());
            if (!Strings.CS.equals(dataScopeValue, crmDataScope)) {
                // 如果DE和CRM的数据权限不一致，则更新
                needUpdate = true;
            }
            // 记录待更新的用户变量
            UserCreateRequest.Variable variable = getCreateVariable(deTempResourceDTO.getSysVariableMap(), value.name(), deTempResourceDTO.getVariableValueMap(), crmDataScope);
            variables.add(variable);
        }

        for (DataScopeDeptVariable value : DataScopeDeptVariable.values()) {
            String dataScopeDeptValue = userVariablePairs.get(value.name());
            List<String> deDeptIds = List.of();
            if (StringUtils.isNotBlank(dataScopeDeptValue)) {
                deDeptIds = stream(dataScopeDeptValue.trim().split(",")).toList();
            }
            List<String> crmDeptIds = Optional.ofNullable(userVariableTempDTO.getUserDeptIdMap().get(value.name()))
                    .orElse(List.of());
            if (!crmDeptIds.equals(deDeptIds)) {
                needUpdate = true;
            }
            if (CollectionUtils.isNotEmpty(crmDeptIds)) {
                // 记录待更新的用户变量
                UserCreateRequest.Variable variable = getCreateVariable(sysVariableMap, value.name(), variableValueMap, crmDeptIds);
                variables.add(variable);
            }
        }

        if (!deUser.getEnable().equals(crmUser.getEnable())) {
            needUpdate = true;
            userUpdateRequest.setEnable(crmUser.getEnable());
        } else {
            // 设置为DE原值
            userUpdateRequest.setEnable(deUser.getEnable());
        }

        if (needUpdate) {
            userUpdateRequest.setVariables(variables);
            try {
                if (CollectionUtils.isEmpty(userUpdateRequest.getRoleIds())) {
                    // 没有角色则删除
                    dataEaseClient.deleteUser(userUpdateRequest.getId());
                } else {
                    dataEaseClient.editUser(userUpdateRequest);
                }
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
    }

    private void addUsers(DeTempResourceDTO deTempResourceDTO) {
        List<UserDTO> enableUsers = deTempResourceDTO.getCrmUsers()
                .stream().filter(UserDTO::getEnable)
                .collect(Collectors.toList());
        Map<String, List<String>> customDeptRoleDeptMap = deTempResourceDTO.getCustomDeptRoleDeptMap();
        Map<String, List<UserRole>> userRoleMap = deTempResourceDTO.getCrmUserRoleMap();
        List<BaseTreeNode> tree = deTempResourceDTO.getDeptTree();
        Map<String, RoleListResponse> crmRoleMap = deTempResourceDTO.getCrmRoleMap();
        Map<String, Set<String>> rolePermissionMap = deTempResourceDTO.getRolePermissionMap();
        DataEaseClient dataEaseClient = deTempResourceDTO.getDataEaseClient();
        Map<String, RoleDTO> deRoleMap = deTempResourceDTO.getDeRoleMap();

        enableUsers.stream()
                .filter(crmUser -> crmUser.getEnable() && !deTempResourceDTO.getDeUserIds().contains(crmUser.getId()))
                .forEach(crmUser -> {
                    List<RoleListResponse> userCrmRoles = getUserCrmRoles(userRoleMap, crmRoleMap, crmUser.getId());
                    UserVariableTempDTO userVariableTempDTO = getUserVariableTempDTO(userCrmRoles, customDeptRoleDeptMap,
                            tree, rolePermissionMap, crmUser);

                    UserCreateRequest userCreateRequest = new UserCreateRequest();
                    userCreateRequest.setName(crmUser.getName());
                    userCreateRequest.setEmail(crmUser.getEmail());
                    userCreateRequest.setAccount(crmUser.getId());
                    List<String> createRoleIds = userCrmRoles.stream()
                            .filter(role -> deRoleMap.containsKey(role.getName()))
                            .map(role -> deRoleMap.get(role.getName()).getId())
                            .collect(Collectors.toList());
                    userCreateRequest.setRoleIds(createRoleIds);
                    userCreateRequest.setVariables(getCreateVariables(deTempResourceDTO, userVariableTempDTO));
                    try {
                        if (CollectionUtils.isNotEmpty(createRoleIds)) {
                            dataEaseClient.createUser(userCreateRequest);
                        }
                    } catch (Exception e) {
                        LogUtils.error(e);
                    }
                });
    }


    private List<String> getRoleIds(List<RoleListResponse> userCrmRoles) {
        if (CollectionUtils.isNotEmpty(userCrmRoles)) {
            return userCrmRoles.stream()
                    .map(RoleListResponse::getId)
                    .toList();
        }
        return List.of();
    }

    private List<UserCreateRequest.Variable> getCreateVariables(DeTempResourceDTO deTempResourceDTO, UserVariableTempDTO userVariableTempDTO) {
        List<UserCreateRequest.Variable> variables = new ArrayList<>();
        Map<String, SysVariableDTO> sysVariableMap = deTempResourceDTO.getSysVariableMap();
        Map<String, Map<String, String>> variableValueMap = deTempResourceDTO.getVariableValueMap();
        for (int i = 0; i < DataScopeVariable.values().length; i++) {
            DataScopeVariable value = DataScopeVariable.values()[i];
            String crmDataScope = userVariableTempDTO.getScopeValueMap().get(value.name());
            UserCreateRequest.Variable variable = getCreateVariable(sysVariableMap, value.name(), variableValueMap, crmDataScope);
            variables.add(variable);
        }

        for (DataScopeDeptVariable value : DataScopeDeptVariable.values()) {
            List<String> crmDeptIds = Optional.ofNullable(userVariableTempDTO.getUserDeptIdMap().get(value.name()))
                    .orElse(List.of());

            if (CollectionUtils.isNotEmpty(crmDeptIds)) {
                UserCreateRequest.Variable variable = getCreateVariable(sysVariableMap, value.name(), variableValueMap, crmDeptIds);
                variables.add(variable);
            }
        }
        return variables;
    }

    private Map<String, Set<String>> getRolePermissionMap(List<String> roleIds) {
        Map<String, Set<String>> rolePermissionMap = new HashMap<>();
        List<RolePermission> rolePermissions = roleService.getRolePermissionByRoleIds(roleIds);
        for (RolePermission rolePermission : rolePermissions) {
            rolePermissionMap.putIfAbsent(rolePermission.getRoleId(), new HashSet<>());
            rolePermissionMap.get(rolePermission.getRoleId()).add(rolePermission.getPermissionId());
        }
        return rolePermissionMap;
    }

    private Map<String, List<String>> getCustomDeptRoleDeptMap(List<RoleListResponse> crmRoles) {
        // 获取自定义部门角色ID
        List<String> customDeptRolesIds = crmRoles.stream()
                .filter(role -> Strings.CS.equalsAny(role.getDataScope(), RoleDataScope.DEPT_CUSTOM.name()))
                .map(RoleListResponse::getId)
                .collect(Collectors.toList());

        List<RoleScopeDept> roleScopeDeptList = roleService.getRoleScopeDeptByRoleIds(customDeptRolesIds);
        Map<String, List<String>> customDeptRoleDeptMap = new HashMap<>();
        roleScopeDeptList.forEach(roleScopeDept -> {
            customDeptRoleDeptMap.putIfAbsent(roleScopeDept.getRoleId(), new ArrayList<>());
            customDeptRoleDeptMap.get(roleScopeDept.getRoleId()).add(roleScopeDept.getDepartmentId());
        });
        return customDeptRoleDeptMap;
    }

    private UserCreateRequest.Variable getCreateVariable(Map<String, SysVariableDTO> sysVariableMap, String variableName,
                                                         Map<String, Map<String, String>> variableValueMap, String variableValueName) {
        SysVariableDTO sysVariable = sysVariableMap.get(variableName);
        String variableId = sysVariable.getId();
        UserCreateRequest.Variable variable = new UserCreateRequest.Variable();
        variable.setVariableId(variableId);
        Map<String, String> nameValueMap = variableValueMap.get(variableId);
        String variableValueId = nameValueMap.get(variableValueName);
        variable.setVariableValueId(variableValueId);
        variable.setVariableValueIds(List.of(variableValueId));
        variable.setSysVariableDto(sysVariable);
        return variable;
    }

    private UserCreateRequest.Variable getCreateVariable(Map<String, SysVariableDTO> sysVariableMap, String variableName,
                                                         Map<String, Map<String, String>> variableValueMap, List<String> variableValueNames) {
        SysVariableDTO sysVariable = sysVariableMap.get(variableName);
        String variableId = sysVariable.getId();
        UserCreateRequest.Variable variable = new UserCreateRequest.Variable();
        variable.setVariableId(variableId);
        Map<String, String> nameValueMap = variableValueMap.get(variableId);
        List<String> variableValueIds = variableValueNames.stream().map(nameValueMap::get).toList();
        variable.setVariableValueIds(variableValueIds);
        variable.setSysVariableDto(sysVariable);
        return variable;
    }

    private UserVariableTempDTO getUserVariableTempDTO(List<RoleListResponse> userCrmRoles,
                                                       Map<String, List<String>> customDeptRoleDeptMap,
                                                       List<BaseTreeNode> tree,
                                                       Map<String, Set<String>> rolePermissionMap,
                                                       UserDTO orgUser) {
        UserVariableTempDTO userVariableTempDTO = new UserVariableTempDTO();

        for (DataScopeVariable value : DataScopeVariable.values()) {
            // 获取有对应的权限的角色
            List<RoleListResponse> permissionRoles = userCrmRoles.stream()
                    .filter(role -> rolePermissionMap.get(role.getId()) != null
                            && rolePermissionMap.get(role.getId()).contains(value.getPermission()))
                    .toList();

            // 获取用户的DataScope
            String crmDataScope = getCrmDataScope(permissionRoles);
            // 记录用户的数据权限
            userVariableTempDTO.getScopeValueMap().put(value.name(), crmDataScope);

            if (Strings.CS.equals(crmDataScope, RoleDataScope.DEPT_CUSTOM.name())) {
                List<String> crmDataScopeDeptIds = getCrmDataScopeDeptIds(tree, permissionRoles,
                        customDeptRoleDeptMap, orgUser);
                // 记录用户有权限的部门ID
                userVariableTempDTO.getUserDeptIdMap().put(value.getDataScopeDeptVariable().name(), crmDataScopeDeptIds);
            }
        }
        return userVariableTempDTO;
    }

    /**
     * 获取当前用户的角色
     *
     * @param userRoleMap
     * @param crmRoleMap
     * @param userId
     * @return
     */
    private List<RoleListResponse> getUserCrmRoles(Map<String, List<UserRole>> userRoleMap, Map<String, RoleListResponse> crmRoleMap, String userId) {
        if (userRoleMap.get(userId) == null) {
            return List.of();
        }
        return userRoleMap.get(userId)
                .stream()
                .filter(userRole -> crmRoleMap.containsKey(userRole.getRoleId()))
                .map(userRole -> crmRoleMap.get(userRole.getRoleId()))
                .collect(Collectors.toList());
    }

    private String getCrmDataScope(List<RoleListResponse> userCrmRoles) {
        String dataScope = NONE_DATA_SCOPE;
        for (RoleListResponse userCrmRole : userCrmRoles) {
            if (Strings.CS.equals(userCrmRole.getDataScope(), RoleDataScope.ALL.name())) {
                return RoleDataScope.ALL.name();
            } else if (Strings.CS.equalsAny(userCrmRole.getDataScope(), RoleDataScope.DEPT_CUSTOM.name(), RoleDataScope.DEPT_AND_CHILD.name())) {
                dataScope = RoleDataScope.DEPT_CUSTOM.name();
            } else {
                dataScope = RoleDataScope.SELF.name();
            }
        }
        return dataScope;
    }

    private List<String> getCrmDataScopeDeptIds(List<BaseTreeNode> tree, List<RoleListResponse> userCrmRoles,
                                                Map<String, List<String>> customDeptRoleDeptMap, UserDTO orgUser) {
        List<String> parentDeptIds = new ArrayList<>();
        for (RoleListResponse userCrmRole : userCrmRoles) {
            if (Strings.CS.equals(userCrmRole.getDataScope(), RoleDataScope.DEPT_CUSTOM.name())) {
                if (CollectionUtils.isNotEmpty(customDeptRoleDeptMap.get(userCrmRole.getId()))) {
                    // 获取指定部门角色中的部门Id
                    List<String> customDeptIds = customDeptRoleDeptMap.get(userCrmRole.getId());
                    parentDeptIds.addAll(customDeptIds);
                }
            } else if (Strings.CS.equals(userCrmRole.getDataScope(), RoleDataScope.DEPT_AND_CHILD.name())) {
                String departmentId = orgUser.getDepartmentId();
                if (StringUtils.isNotBlank(departmentId)) {
                    // 获取当前部门ID
                    parentDeptIds.add(departmentId);
                }
            }
        }
        // 获取部门及子部门ID
        return dataScopeService.getDeptIdsWithChild(tree, new HashSet<>(parentDeptIds));
    }

    public Map<String, String> extractUserVariablePairs(String input) {
        if (StringUtils.isBlank(input)) {
            return Map.of();
        }
        Map<String, String> keyValuePairs = new HashMap<>();

        // 正则表达式匹配 {key: value} 格式
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String pair = matcher.group(1);
            // 分割键值对
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                // 处理值中的可能的分隔符
                if (value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1);
                }

                keyValuePairs.put(key, value);
            }
        }

        return keyValuePairs;
    }

    private void syncRole(DeTempResourceDTO deTempResourceDTO) {
        DataEaseClient dataEaseClient = deTempResourceDTO.getDataEaseClient();
        List<RoleListResponse> crmRoles = deTempResourceDTO.getCrmRoles();
        List<RoleDTO> roles = dataEaseClient.listRole();
        Map<String, RoleDTO> roleMap = roles.stream()
                .collect(Collectors.toMap(RoleDTO::getName, Function.identity()));
        // 记录角色名和角色的映射
        deTempResourceDTO.setDeRoleMap(roleMap);

        RoleCreateRequest roleCreateRequest = new RoleCreateRequest();
        for (RoleListResponse crmRole : crmRoles) {
            if (!roleMap.containsKey(crmRole.getName())) {
                roleCreateRequest.setName(crmRole.getName());
                // 创建角色
                try {
                    Long roleId = dataEaseClient.createRole(roleCreateRequest);
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setId(roleId.toString());
                    roleDTO.setName(crmRole.getName());
                    roleMap.put(roleDTO.getName(), roleDTO);
                } catch (Exception e) {
                    LogUtils.error(e);
                }
            }
        }
    }

    private void syncSysVariable(DeTempResourceDTO deTempResourceDTO) {
        // 记录变量名和变量的映射
        deTempResourceDTO.setVariableValueMap(new HashMap<>());
        Map<String, Map<String, String>> variableValueMap = deTempResourceDTO.getVariableValueMap();

        DataEaseClient dataEaseClient = deTempResourceDTO.getDataEaseClient();
        List<SysVariableDTO> sysVariables = dataEaseClient.listSysVariable();

        Map<String, SysVariableDTO> sysVariableMap = sysVariables.stream()
                .collect(Collectors.toMap(SysVariableDTO::getName, Function.identity()));
        // 记录变量名和变量的映射
        deTempResourceDTO.setSysVariableMap(sysVariableMap);

        for (DataScopeVariable value : DataScopeVariable.values()) {
            if (!sysVariableMap.keySet().contains(value.name())) {
                // 创建数据权限系统变量
                SysVariableDTO dataScopeVariable = createDataScopeVariable(dataEaseClient, value.name(), variableValueMap);
                sysVariableMap.put(value.name(), dataScopeVariable);
            } else {
                // 同步数据权限系统变量值
                SysVariableDTO sysVariable = sysVariableMap.get(value.name());
                Map<String, SysVariableValueDTO> valueMap = dataEaseClient.listSysVariableValue(sysVariable.getId())
                        .stream().collect(Collectors.toMap(SysVariableValueDTO::getValue, Function.identity()));

                variableValueMap.putIfAbsent(sysVariable.getId(), new HashMap<>());
                Map<String, String> variableValueIdNameMap = variableValueMap.get(sysVariable.getId());

                valueMap.forEach((variableValueId, sysVariableValue) ->
                        variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId()));

                List<String> dataScopeValues = List.of(RoleDataScope.ALL.name(),
                        RoleDataScope.SELF.name(),
                        RoleDataScope.DEPT_CUSTOM.name(), NONE_DATA_SCOPE);

                for (String dataScopeValue : dataScopeValues) {
                    if (!valueMap.containsKey(dataScopeValue)) {
                        // 创建缺失的变量
                        SysVariableValueCreateRequest variableValueCreateRequest = new SysVariableValueCreateRequest();
                        variableValueCreateRequest.setSysVariableId(sysVariable.getId());
                        variableValueCreateRequest.setValue(dataScopeValue);
                        try {
                            SysVariableValueDTO sysVariableValue = dataEaseClient.createSysVariableValue(variableValueCreateRequest);
                            variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId());
                        } catch (Exception e) {
                            LogUtils.error(e);
                        }
                    }
                }
            }
        }

        List<String> deptIds = extDepartmentMapper.selectAllDepartmentIds(deTempResourceDTO.getCrmOrgId());
        Set<String> deptIdSet = deptIds.stream().collect(Collectors.toSet());
        for (DataScopeDeptVariable value : DataScopeDeptVariable.values()) {
            if (!sysVariableMap.keySet().contains(value.name())) {
                // 创建数据权限部门变量
                try {
                    SysVariableDTO dataScopeDeptVariable = createDataScopeDeptVariable(dataEaseClient, value.name(), deptIds, variableValueMap);
                    sysVariableMap.put(value.name(), dataScopeDeptVariable);
                } catch (Exception e) {
                    LogUtils.error(e);
                }
            } else {
                // 同步部门
                SysVariableDTO sysVariable = sysVariableMap.get(value.name());
                Map<String, SysVariableValueDTO> valueMap = dataEaseClient.listSysVariableValue(sysVariable.getId())
                        .stream().collect(Collectors.toMap(SysVariableValueDTO::getValue, Function.identity()));

                variableValueMap.putIfAbsent(sysVariable.getId(), new HashMap<>());
                Map<String, String> variableValueIdNameMap = variableValueMap.get(sysVariable.getId());
                valueMap.forEach((variableValueId, sysVariableValue) ->
                        variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId()));

                // 取 deptIds 和 valueMap.key() 的差集
                List<String> addValues = deptIds.stream()
                        .filter(deptId -> !valueMap.containsKey(deptId))
                        .collect(Collectors.toList());

                SysVariableValueCreateRequest variableValueCreateRequest = new SysVariableValueCreateRequest();
                variableValueCreateRequest.setSysVariableId(sysVariable.getId());
                for (String addValue : addValues) {
                    variableValueCreateRequest.setValue(addValue);
                    try {
                        SysVariableValueDTO sysVariableValue = dataEaseClient.createSysVariableValue(variableValueCreateRequest);
                        variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId());
                    } catch (Exception e) {
                        LogUtils.error(e);
                    }
                }

                // 取 valueMap.key() 和 deptIds 的差集
                List<String> deleteValueIds = valueMap.keySet().stream()
                        .filter(deDeptId -> !deptIdSet.contains(deDeptId))
                        .map(key -> valueMap.get(key).getId())
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(deleteValueIds)) {
                    dataEaseClient.batchDelSysVariableValue(deleteValueIds);
                }
            }
        }
    }

    private SysVariableDTO createDataScopeVariable(DataEaseClient dataEaseClient, String sysVariableName,
                                                   Map<String, Map<String, String>> variableValueMap) {
        SysVariableCreateRequest sysVariableCreateRequest = new SysVariableCreateRequest();
        sysVariableCreateRequest.setName(sysVariableName);
        SysVariableDTO sysVariable = dataEaseClient.createSysVariable(sysVariableCreateRequest);

        variableValueMap.putIfAbsent(sysVariable.getId(), new HashMap<>());
        Map<String, String> variableValueIdNameMap = variableValueMap.get(sysVariable.getId());

        List<String> dataScopeValues = List.of(RoleDataScope.ALL.name(),
                RoleDataScope.SELF.name(),
                RoleDataScope.DEPT_CUSTOM.name(), NONE_DATA_SCOPE);


        SysVariableValueCreateRequest sysVariableValueCreateRequest = new SysVariableValueCreateRequest();
        sysVariableValueCreateRequest.setSysVariableId(sysVariable.getId());
        for (String dataScopeValue : dataScopeValues) {
            sysVariableValueCreateRequest.setValue(dataScopeValue);
            try {
                SysVariableValueDTO sysVariableValue = dataEaseClient.createSysVariableValue(sysVariableValueCreateRequest);
                variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId());
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
        return sysVariable;
    }

    private SysVariableDTO createDataScopeDeptVariable(DataEaseClient dataEaseClient, String sysVariableName,
                                                       List<String> deptIds, Map<String, Map<String, String>> variableValueMap) {
        SysVariableCreateRequest sysVariableCreateRequest = new SysVariableCreateRequest();
        sysVariableCreateRequest.setName(sysVariableName);
        sysVariableCreateRequest.setDesc("CRM生成，请勿修改！");
        SysVariableDTO sysVariable = dataEaseClient.createSysVariable(sysVariableCreateRequest);

        variableValueMap.putIfAbsent(sysVariable.getId(), new HashMap<>());
        Map<String, String> variableValueIdNameMap = variableValueMap.get(sysVariable.getId());

        SysVariableValueCreateRequest sysVariableValueCreateRequest = new SysVariableValueCreateRequest();
        for (String deptId : deptIds) {
            sysVariableValueCreateRequest.setSysVariableId(sysVariable.getId());
            sysVariableValueCreateRequest.setValue(deptId);
            try {
                SysVariableValueDTO sysVariableValue = dataEaseClient.createSysVariableValue(sysVariableValueCreateRequest);
                variableValueIdNameMap.put(sysVariableValue.getValue(), sysVariableValue.getId());
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
        return sysVariable;
    }

    public List<OptionDTO> getDeOrgList(ThirdConfigurationDTO thirdConfigurationDTO) {
        try {
            DataEaseClient dataEaseClient = new DataEaseClient(thirdConfigurationDTO);
            return dataEaseClient.listOrg();
        } catch (Exception e) {
            LogUtils.error(e);
            return List.of();
        }
    }
}
