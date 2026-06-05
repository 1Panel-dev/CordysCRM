package cn.cordys.crm.form.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.mapper.ExtDepartmentMapper;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleListResponse;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormRoleUserMapper;
import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.RoleService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public Pager<List<CustomFormRoleUserListResponse>> listUsersByRole(String roleId, BasePageRequest request,
                                                                       String userId, String orgId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(roleId);
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomFormRoleUserListResponse> roleUsers = extCustomFormRoleUserMapper.listByRoleId(roleId, orgId, request);
        fillRoles(roleUsers, orgId);
        return PageUtils.setPageInfo(page, roleUsers);
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

    public void addUsers(CustomFormRoleUserBatchRequest request, String userId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getCustomFormRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        List<String> userIds = resolveUserIds(request);
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
        List<CustomFormRoleUser> toInsert = ListUtils.subtract(userIds, currentRoleUserIds)
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
    }

    public void removeUsers(CustomFormRoleUserBatchRequest request, String userId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getCustomFormRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        if (CollectionUtils.isEmpty(request.getUserIds())) {
            return;
        }

        for (String uid : request.getUserIds()) {
            LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomFormRoleUser::getRoleId, request.getCustomFormRoleId()).eq(CustomFormRoleUser::getUserId, uid);
            customFormRoleUserMapper.deleteByLambda(wrapper);
        }
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
