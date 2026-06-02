package cn.cordys.crm.form.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleListResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormRoleService {

    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private BaseMapper<CustomFormAdmin> customFormAdminMapper;

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
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        for (String uid : request.getUserIds()) {
            LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomFormRoleUser::getRoleId, request.getRoleId()).eq(CustomFormRoleUser::getUserId, uid);
            if (customFormRoleUserMapper.selectListByLambda(wrapper).isEmpty()) {
                CustomFormRoleUser roleUser = new CustomFormRoleUser();
                roleUser.setId(IDGenerator.nextStr());
                roleUser.setRoleId(request.getRoleId());
                roleUser.setUserId(uid);
                customFormRoleUserMapper.insert(roleUser);
            }
        }
    }

    public void removeUsers(CustomFormRoleUserBatchRequest request, String userId) {
        CustomFormRole role = customFormRoleMapper.selectByPrimaryKey(request.getRoleId());
        if (role == null) {
            throw new GenericException(Translator.get("custom.form.role.not.exist"));
        }
        checkFormAdmin(role.getCustomFormId(), userId);

        for (String uid : request.getUserIds()) {
            LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomFormRoleUser::getRoleId, request.getRoleId()).eq(CustomFormRoleUser::getUserId, uid);
            customFormRoleUserMapper.deleteByLambda(wrapper);
        }
    }

    private void checkFormAdmin(String formId, String userId) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getCustomFormId, formId).eq(CustomFormAdmin::getUserId, userId);
        if (customFormAdminMapper.selectListByLambda(wrapper).isEmpty()) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }
}
