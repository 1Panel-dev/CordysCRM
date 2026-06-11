package cn.cordys.crm.form.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.SubListUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleListResponse;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormRoleUserMapper;
import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import cn.cordys.crm.system.dto.response.RoleListResponse;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormRoleService {

    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private BaseMapper<CustomFormAdmin> customFormAdminMapper;
    @Resource
    private ExtDepartmentMapper extDepartmentMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private ExtCustomFormRoleUserMapper extCustomFormRoleUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private LogService logService;

    public List<CustomFormRoleListResponse> listByFormId(String customFormId, String userId) {
        checkFormAdmin(customFormId, userId);

        LambdaQueryWrapper<CustomFormRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRole::getCustomFormId, customFormId);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(wrapper);

        return roles.stream().map(role -> {
            CustomFormRoleListResponse resp = new CustomFormRoleListResponse();
            resp.setId(role.getId());
            resp.setName(role.getName());
            resp.setCustomFormId(role.getCustomFormId());
            resp.setInternalKey(role.getInternalKey());
            return resp;
        }).toList();
    }

    public Pager<List<CustomFormRoleUserListResponse>> listUsersByRole(CustomFormRoleUserPageRequest request,
                                                                       String userId, String orgId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getCustomFormRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomFormRoleUserListResponse> roleUsers = extCustomFormRoleUserMapper.listByRoleId(orgId, request);
        fillRoles(roleUsers, orgId);
        return PageUtils.setPageInfo(page, roleUsers);
    }

    public List<DeptUserTreeNode> getDeptUserTree(String orgId) {
        List<DeptUserTreeNode> treeNodes = extDepartmentMapper.selectDeptUserTreeNode(orgId);
        List<DeptUserTreeNode> userNodes = extUserRoleMapper.selectUserDeptForOrg(orgId);
        userNodes = departmentService.sortByCommander(orgId, userNodes);
        userNodes.addAll(treeNodes);
        return BaseTreeNode.buildTree(userNodes);
    }

    public List<RoleUserTreeNode> getRoleUserTree(String orgId) {
        List<RoleListResponse> roles = roleService.list(orgId);
        List<RoleUserTreeNode> treeNodes = roles.stream().map(role -> {
            RoleUserTreeNode roleNode = new RoleUserTreeNode();
            roleNode.setNodeType("ROLE");
            roleNode.setInternal(BooleanUtils.isTrue(role.getInternal()));
            roleNode.setId(role.getId());
            roleNode.setName(role.getName());
            return roleNode;
        }).collect(Collectors.toList());

        List<RoleUserTreeNode> userNodes = extUserRoleMapper.selectUserRoleForOrg(orgId);
        treeNodes.addAll(userNodes);
        return BaseTreeNode.buildTree(treeNodes);
    }

    private void fillRoles(List<CustomFormRoleUserListResponse> roleUsers, String orgId) {
        if (CollectionUtils.isEmpty(roleUsers)) {
            return;
        }
        List<String> userIds = roleUsers.stream().map(CustomFormRoleUserListResponse::getUserId).toList();
        List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
        userRoles.forEach(role -> role.setName(roleService.translateInternalRole(role.getName())));
        Map<String, List<UserRoleConvert>> userRoleMap = userRoles.stream()
                .collect(Collectors.groupingBy(UserRoleConvert::getUserId));
        roleUsers.forEach(roleUser -> roleUser.setRoles(userRoleMap.getOrDefault(roleUser.getUserId(), List.of())));
    }

    public void addUsers(CustomFormRoleUserBatchRequest request, String userId, String orgId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getCustomFormRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        List<String> resolvedUserIds = resolveUserIds(request);
        if (CollectionUtils.isEmpty(resolvedUserIds)) {
            return;
        }
        List<String> userIds = new ArrayList<>();
        SubListUtils.dealForSubList(resolvedUserIds, SubListUtils.DEFAULT_QUERY_BATCH_SIZE,
                subUserIds -> userIds.addAll(extUserMapper.filterEnabledUserIds(subUserIds, orgId)));
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRoleUser::getRoleId, request.getCustomFormRoleId());
        List<String> currentRoleUserIds = customFormRoleUserMapper.selectListByLambda(wrapper)
                .stream()
                .map(CustomFormRoleUser::getUserId)
                .toList();

        long now = System.currentTimeMillis();
        List<String> insertUserIds = ListUtils.subtract(userIds, currentRoleUserIds);
        List<CustomFormRoleUser> toInsert = insertUserIds
                .stream()
                .map(uid -> {
                    CustomFormRoleUser roleUser = new CustomFormRoleUser();
                    roleUser.setId(IDGenerator.nextStr());
                    roleUser.setRoleId(request.getCustomFormRoleId());
                    roleUser.setUserId(uid);
                    roleUser.setCreateTime(now);
                    roleUser.setUpdateTime(now);
                    roleUser.setCreateUser(userId);
                    roleUser.setUpdateUser(userId);
                    return roleUser;
                }).toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            customFormRoleUserMapper.batchInsert(toInsert);
        }

        CustomForm customForm = customFormMapper.selectByPrimaryKey(role.getCustomFormId());
        List<String> userNames = extUserMapper.selectUserNameByIds(insertUserIds);
        LogDTO logDTO = new LogDTO(orgId, role.getCustomFormId(), userId, LogType.ADD_USER, LogModule.CUSTOM_FORM, customForm.getName());
        String detail = "[" + role.getName() + "]: " + userNames.stream().collect(Collectors.joining(", "));
        logDTO.setDetail(detail);
        logService.add(logDTO);
    }

    public void removeUsers(CustomFormRoleUserBatchRequest request, String userId, String orgId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getCustomFormRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        if (CollectionUtils.isEmpty(request.getUserIds())) {
            return;
        }

        LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRoleUser::getRoleId, request.getCustomFormRoleId()).in(CustomFormRoleUser::getId, request.getUserIds());
        List<String> roleUserIds = customFormRoleUserMapper.selectByIds(request.getUserIds())
                .stream()
                .map(CustomFormRoleUser::getUserId).toList();
        customFormRoleUserMapper.deleteByLambda(wrapper);

        CustomForm customForm = customFormMapper.selectByPrimaryKey(role.getCustomFormId());

        List<String> userNames = extUserMapper.selectUserNameByIds(roleUserIds);
        LogDTO logDTO = new LogDTO(orgId, role.getCustomFormId(), userId, LogType.REMOVE_USER, LogModule.CUSTOM_FORM, customForm.getName());
        String detail = "[" + role.getName() + "]: " + userNames.stream().collect(Collectors.joining(", "));
        logDTO.setDetail(detail);
        logService.add(logDTO);
    }

    private List<String> resolveUserIds(CustomFormRoleUserBatchRequest request) {
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
        return new ArrayList<>(userSet);
    }

    private void checkFormAdmin(String formId, String userId) {
        if (Objects.equals(InternalUser.ADMIN.getValue(), userId)) {
            return;
        }

        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getCustomFormId, formId).eq(CustomFormAdmin::getUserId, userId);
        if (customFormAdminMapper.selectListByLambda(wrapper).isEmpty()) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }
}
