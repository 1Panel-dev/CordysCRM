package io.cordys.crm.system.service;

import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.dto.RoleUserTreeNode;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.SubListUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Department;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.UserExtend;
import io.cordys.crm.system.domain.UserRole;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.RoleUserPageRequest;
import io.cordys.crm.system.dto.request.RoleUserRelateRequest;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.crm.system.dto.response.RoleUserListResponse;
import io.cordys.crm.system.dto.response.RoleUserOptionResponse;
import io.cordys.crm.system.mapper.ExtDepartmentMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserRoleService {
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private BaseMapper<UserExtend> userExtendMapper;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private LogService logService;

    public void delete(String id) {
        userRoleMapper.deleteByPrimaryKey(id);
    }

    public List<RoleUserListResponse> listUserByRoleId(RoleUserPageRequest request, String orgId) {
        List<RoleUserListResponse> users = extUserRoleMapper.list(request, orgId);
        Map<String, List<UserRoleConvert>> userRoleMap = getUserRoleMap(orgId, users);

        Map<String, String> deptNameMap = getDeptNameMap(users);
        Map<String, String> userAvatarMap = getUserAvatarMap(users);

        for (RoleUserListResponse user : users) {
            user.setRoles(userRoleMap.get(user.getUserId()));
            user.setDepartmentName(deptNameMap.get(user.getDepartmentId()));
            user.setAvatar(userAvatarMap.get(user.getUserId()));
        }
        return users;
    }

    private Map<String, String> getUserAvatarMap(List<RoleUserListResponse> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }

        List<String> userIds = users.stream().map(RoleUserListResponse::getUserId).toList();
        return userExtendMapper.selectByIds(userIds.toArray(String[]::new))
                .stream()
                .filter(userExtend -> userExtend != null && StringUtils.isNotBlank(userExtend.getId()))
                .collect(Collectors.toMap(
                        UserExtend::getId,
                        userExtend -> StringUtils.defaultIfBlank(userExtend.getAvatar(), "")
                ));
    }

    private Map<String, String> getDeptNameMap(List<RoleUserListResponse> users) {
        return departmentMapper.selectByIds(users.stream().map(RoleUserListResponse::getDepartmentId).distinct().toArray(String[]::new))
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private Map<String, List<UserRoleConvert>> getUserRoleMap(String orgId, List<RoleUserListResponse> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Map.of();
        }
        List<String> userIds = users.stream()
                .map(RoleUserListResponse::getUserId)
                .collect(Collectors.toList());

        List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
        userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
        return userRoles.stream().collect(Collectors.groupingBy(UserRoleConvert::getUserId));
    }

    /**
     * 获取带用户的信息的部门树
     *
     * @return List<DeptUserTreeNode>
     */
    public List<DeptUserTreeNode> getDeptUserTree(String orgId, String roleId) {
        List<DeptUserTreeNode> treeNodes = extDepartmentMapper.selectDeptUserTreeNode(orgId);
        List<DeptUserTreeNode> userNodes = extUserRoleMapper.selectUserDeptForRelevance(orgId, roleId);
        // 如果已经关联的用户，设置为 disable
        Set<String> currentRoleUserIds = new HashSet<>(extUserRoleMapper.getUserIdsByRoleIds(List.of(roleId)));
        userNodes.forEach(userNode -> {
            if (currentRoleUserIds.contains(userNode.getId())) {
                userNode.setEnabled(false);
            }
        });
        userNodes.addAll(treeNodes);
        return BaseTreeNode.buildTree(userNodes);
    }

    public List<RoleUserTreeNode> getRoleUserTree(String orgId, String roleId) {
        // 查询角色信息
        List<RoleListResponse> list = roleService.list(orgId);
        List<RoleUserTreeNode> treeNodes = list.stream().filter(role -> !StringUtil.equals(roleId, role.getId()))
                .map((role) -> {
                    RoleUserTreeNode roleNode = new RoleUserTreeNode();
                    roleNode.setNodeType("ROLE");
                    roleNode.setInternal(BooleanUtils.isTrue(role.getInternal()));
                    roleNode.setId(role.getId());
                    roleNode.setName(role.getName());
                    return roleNode;
                }).collect(Collectors.toList());
        // 查询用户信息
        List<RoleUserTreeNode> userNodes = extUserRoleMapper.selectUserRoleForRelevance(orgId, roleId);

        // 如果已经关联的用户，设置为 disable
        Set<String> currentRoleUserIds = new HashSet<>(extUserRoleMapper.getUserIdsByRoleIds(List.of(roleId)));

        userNodes.forEach(userNode -> {
            if (currentRoleUserIds.contains(userNode.getId())) {
                userNode.setEnabled(false);
            }
        });
        treeNodes.addAll(userNodes);
        return BaseTreeNode.buildTree(treeNodes);
    }

    public void relateUser(RoleUserRelateRequest request, String operator, String orgId) {
        Set<String> userSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(request.getRoleIds())) {
            userSet.addAll(extUserRoleMapper.getUserIdsByRoleIds(request.getRoleIds()));
        }
        if (CollectionUtils.isNotEmpty(request.getDeptIds())) {
            userSet.addAll(extDepartmentMapper.getUserIdsByDeptIds(request.getDeptIds()));
        }
        if (CollectionUtils.isNotEmpty(request.getUserIds())) {
            userSet.addAll(request.getUserIds());
        }

        List<String> userIds = new ArrayList<>(userSet);
        SubListUtils.dealForSubList(userIds, 50, (subUserIds) -> {
            Map<String, String> userNameMap = baseService.getUserNameMap(subUserIds);
            // 过滤掉已删除的用户
            subUserIds = subUserIds.stream()
                    .filter(userId -> userNameMap.get(userId) != null)
                    .toList();

            List<String> currentRoleUserIds = extUserRoleMapper.getUserIdsByRoleIds(List.of(request.getRoleId()));
            // 去除已关联的用户，添加未关联的用户
            List<UserRole> userRoles = ListUtils.subtract(subUserIds, currentRoleUserIds).stream()
                    .map(userId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(request.getRoleId());
                        userRole.setId(IDGenerator.nextStr());
                        userRole.setCreateUser(operator);
                        userRole.setUpdateUser(operator);
                        userRole.setCreateTime(System.currentTimeMillis());
                        userRole.setUpdateTime(System.currentTimeMillis());
                        return userRole;
                    })
                    .collect(Collectors.toList());
            userRoleMapper.batchInsert(userRoles);

            Role role = roleMapper.selectByPrimaryKey(request.getRoleId());
            role.setName(roleService.translateInternalRole(role.getName()));
            List<LogDTO> logs = new ArrayList<>();
            userRoles.forEach(u -> {
                // 记录日志
                LogDTO logDTO = new LogDTO(orgId, u.getRoleId(), operator, LogType.ADD_USER, LogModule.SYSTEM_ROLE, role.getName());
                String detail = Translator.getWithArgs("role.add_user", role.getName(),
                        userNameMap.get(u.getUserId()));
                logDTO.setDetail(detail);
                logs.add(logDTO);

                // 清除权限缓存
                permissionCache.clearCache(u.getUserId(), orgId);
            });

            logService.batchAdd(logs);
        });
    }

    public void deleteRoleUser(String id, String userId, String orgId) {
        UserRole userRole = userRoleMapper.selectByPrimaryKey(id);
        userRoleMapper.deleteByPrimaryKey(id);
        permissionCache.clearCache(userRole.getUserId(), orgId);

        Role role = roleMapper.selectByPrimaryKey(userRole.getRoleId());
        role.setName(roleService.translateInternalRole(role.getName()));
        String userName = baseService.getUserName(userRole.getUserId());
        // 记录日志
        LogDTO logDTO = new LogDTO(orgId, role.getId(), userId, LogType.REMOVE_USER, LogModule.SYSTEM_ROLE, role.getName());
        String detail = Translator.getWithArgs("role.remove_user", role.getName(),
                userName);
        logDTO.setDetail(detail);
        logService.add(logDTO);
    }

    public void batchDeleteRoleUser(List<String> ids, String userId, String orgId) {
        List<UserRole> userRoles = userRoleMapper.selectByIds(ids);
        extUserRoleMapper.deleteByIds(ids);

        List<String> userIds = userRoles.stream()
                .map(UserRole::getUserId)
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Role role = roleMapper.selectByPrimaryKey(userRoles.getFirst().getRoleId());
        role.setName(roleService.translateInternalRole(role.getName()));
        List<LogDTO> logs = new ArrayList<>();
        userRoles.forEach(userRole -> {
            // 记录日志
            LogDTO logDTO = new LogDTO(orgId, userRole.getRoleId(), userId, LogType.REMOVE_USER, LogModule.SYSTEM_ROLE, role.getName());
            String detail = Translator.getWithArgs("role.remove_user", role.getName(),
                    userNameMap.get(userRole.getUserId()));
            logDTO.setDetail(detail);
            logs.add(logDTO);

            // 清除权限缓存
            permissionCache.clearCache(userRole.getUserId(), orgId);
        });

        logService.batchAdd(logs);
    }

    public List<RoleUserOptionResponse> getUserOptionByRoleId(String organizationId, String roleId) {
        List<RoleUserOptionResponse> roleUserOptions = extUserRoleMapper.selectUserOptionByRoleId(organizationId, roleId);
        Set<String> roleUserIdSet = new HashSet<>(extUserRoleMapper.getUserIdsByRoleIds(List.of(roleId)));

        roleUserOptions.forEach(userOption -> userOption.setEnabled(!roleUserIdSet.contains(userOption.getId())));

        return roleUserOptions;
    }
}