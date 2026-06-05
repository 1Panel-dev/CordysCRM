package cn.cordys.crm.form.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
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
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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

    public List<String> listUsersByRole(String roleId, String userId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(roleId);
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRoleUser::getRoleId, roleId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(wrapper);

        return roleUsers.stream().map(CustomFormRoleUser::getUserId).toList();
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

        List<CustomFormRoleUser> toInsert = ListUtils.subtract(userIds, currentRoleUserIds)
                .stream()
                .map(uid -> {
                    CustomFormRoleUser roleUser = new CustomFormRoleUser();
                    roleUser.setId(IDGenerator.nextStr());
                    roleUser.setRoleId(request.getCustomFormRoleId());
                    roleUser.setUserId(uid);
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
