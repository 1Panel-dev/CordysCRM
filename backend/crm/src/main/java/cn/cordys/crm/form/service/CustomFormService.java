package cn.cordys.crm.form.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormSaveRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormService {

    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private BaseMapper<CustomFormAdmin> customFormAdminMapper;
    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private BaseService baseService;

    public List<CustomFormListResponse> list(String userId) {
        // single query: union of admin + role-member form ids
        Set<String> accessibleFormIds = getAccessibleFormIds(userId);
        if (accessibleFormIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> adminFormIds = getAdminFormIds(userId);

        return accessibleFormIds.stream().map(formId -> {
            CustomForm form = customFormMapper.selectByPrimaryKey(formId);
            if (form == null) {
                return null;
            }
            CustomFormListResponse resp = new CustomFormListResponse();
            resp.setId(form.getId());
            resp.setName(form.getName());
            resp.setEnable(form.getEnable());
            resp.setCreateTime(form.getCreateTime());
            resp.setUpdateTime(form.getUpdateTime());
            resp.setIsAdmin(adminFormIds.contains(formId));
            return resp;
        }).filter(Objects::nonNull).toList();
    }

    public CustomFormGetResponse get(String id, String userId) {
        checkFormAccess(id, userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        CustomFormGetResponse resp = BeanUtils.copyBean(new CustomFormGetResponse(), form);
        baseService.setCreateUpdateOwnerUserName(resp);

        Set<String> adminFormIds = getAdminFormIds(userId);
        resp.setIsAdmin(adminFormIds.contains(id));

        return resp;
    }

    public CustomForm create(CustomFormSaveRequest request, String userId, String orgId) {
        String formId = IDGenerator.nextStr();

        // 1. save custom_form
        CustomForm form = new CustomForm();
        form.setId(formId);
        form.setName(request.getName());
        form.setEnable(request.getEnable() != null ? request.getEnable() : false);
        form.setCreateTime(System.currentTimeMillis());
        form.setUpdateTime(System.currentTimeMillis());
        form.setCreateUser(userId);
        form.setUpdateUser(userId);
        customFormMapper.insert(form);

        // 2. save sys_module_form with same ID
        ModuleForm moduleForm = new ModuleForm();
        moduleForm.setId(formId);
        moduleForm.setFormKey(UUID.randomUUID().toString().substring(20));
        moduleForm.setOrganizationId(orgId);
        moduleForm.setCreateTime(System.currentTimeMillis());
        moduleForm.setUpdateTime(System.currentTimeMillis());
        moduleForm.setCreateUser(userId);
        moduleForm.setUpdateUser(userId);
        moduleFormMapper.insert(moduleForm);

        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(formId);
        formBlob.setProp(request.getFormProp() != null ? JSON.toJSONString(request.getFormProp()) : "{}");
        moduleFormBlobMapper.insert(formBlob);

        // 3. save form fields via ModuleFormSaveRequest
        if (CollectionUtils.isNotEmpty(request.getFields())) {
            ModuleFormSaveRequest formSaveRequest = new ModuleFormSaveRequest();
            formSaveRequest.setFormKey(moduleForm.getFormKey());
            formSaveRequest.setFields(request.getFields());
            formSaveRequest.setFormProp(request.getFormProp());
            moduleFormCacheService.save(formSaveRequest, userId, orgId);
        }

        // 4. create built-in roles (batch)
        createBuiltinRoles(formId, userId);

        // 5. add creator as admin
        CustomFormAdmin admin = new CustomFormAdmin();
        admin.setId(IDGenerator.nextStr());
        admin.setCustomFormId(formId);
        admin.setUserId(userId);
        customFormAdminMapper.insert(admin);

        return form;
    }

    public void update(CustomFormUpdateRequest request, String userId) {
        checkFormAdmin(request.getId(), userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(request.getId());
        if (form == null) {
            throw new GenericException(Translator.get("custom.form.not.exist"));
        }

        CustomForm updateForm = new CustomForm();
        updateForm.setId(request.getId());
        updateForm.setName(request.getName());
        updateForm.setEnable(request.getEnable());
        updateForm.setUpdateTime(System.currentTimeMillis());
        updateForm.setUpdateUser(userId);
        customFormMapper.update(updateForm);
    }

    public void delete(String id, String userId) {
        checkFormAdmin(id, userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("custom.form.not.exist"));
        }

        // delete custom_form_admin
        LambdaQueryWrapper<CustomFormAdmin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(CustomFormAdmin::getCustomFormId, id);
        customFormAdminMapper.deleteByLambda(adminWrapper);

        // delete custom_form_role_user and custom_form_role
        LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(CustomFormRole::getCustomFormId, id);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
        if (CollectionUtils.isNotEmpty(roles)) {
            List<String> roleIds = roles.stream().map(CustomFormRole::getId).toList();
            LambdaQueryWrapper<CustomFormRoleUser> roleUserWrapper = new LambdaQueryWrapper<>();
            roleUserWrapper.in(CustomFormRoleUser::getRoleId, roleIds);
            customFormRoleUserMapper.deleteByLambda(roleUserWrapper);
            customFormRoleMapper.deleteByLambda(roleWrapper);
        }

        // delete custom_form
        customFormMapper.deleteByIds(List.of(id));
    }

    public void addAdmins(CustomFormAdminBatchRequest request, String userId) {
        checkFormAdmin(request.getCustomFormId(), userId);

        String formId = request.getCustomFormId();

        // query existing admins for dedup
        LambdaQueryWrapper<CustomFormAdmin> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(CustomFormAdmin::getCustomFormId, formId)
                .in(CustomFormAdmin::getUserId, request.getUserIds());
        Set<String> existingUserIds = customFormAdminMapper.selectListByLambda(existWrapper)
                .stream().map(CustomFormAdmin::getUserId).collect(Collectors.toSet());

        List<CustomFormAdmin> toInsert = request.getUserIds().stream()
                .filter(uid -> !existingUserIds.contains(uid))
                .map(uid -> {
                    CustomFormAdmin admin = new CustomFormAdmin();
                    admin.setId(IDGenerator.nextStr());
                    admin.setCustomFormId(formId);
                    admin.setUserId(uid);
                    return admin;
                }).toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            customFormAdminMapper.batchInsert(toInsert);
        }
    }

    public void removeAdmins(CustomFormAdminBatchRequest request, String userId) {
        checkFormAdmin(request.getCustomFormId(), userId);

        String formId = request.getCustomFormId();
        for (String uid : request.getUserIds()) {
            LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomFormAdmin::getCustomFormId, formId).eq(CustomFormAdmin::getUserId, uid);
            customFormAdminMapper.deleteByLambda(wrapper);
        }
    }

    // --- Permission helpers ---

    private void checkFormAccess(String formId, String userId) {
        Set<String> accessibleFormIds = getAccessibleFormIds(userId);
        if (!accessibleFormIds.contains(formId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    private void checkFormAdmin(String formId, String userId) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getCustomFormId, formId).eq(CustomFormAdmin::getUserId, userId);
        if (customFormAdminMapper.selectListByLambda(wrapper).isEmpty()) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    Set<String> getAdminFormIds(String userId) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getUserId, userId);
        List<CustomFormAdmin> admins = customFormAdminMapper.selectListByLambda(wrapper);
        return admins.stream().map(CustomFormAdmin::getCustomFormId).collect(Collectors.toSet());
    }

    /**
     * Combined query: returns form IDs where user is either admin or role member.
     * Replaces the separate getAdminFormIds + getMemberFormIds for access checks.
     */
    private Set<String> getAccessibleFormIds(String userId) {
        // admin form ids
        LambdaQueryWrapper<CustomFormAdmin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(CustomFormAdmin::getUserId, userId);
        List<CustomFormAdmin> admins = customFormAdminMapper.selectListByLambda(adminWrapper);
        Set<String> formIds = admins.stream().map(CustomFormAdmin::getCustomFormId).collect(Collectors.toSet());

        // role-member form ids: role_user -> role -> formId
        LambdaQueryWrapper<CustomFormRoleUser> ruWrapper = new LambdaQueryWrapper<>();
        ruWrapper.eq(CustomFormRoleUser::getUserId, userId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(ruWrapper);
        if (CollectionUtils.isNotEmpty(roleUsers)) {
            List<String> roleIds = roleUsers.stream().map(CustomFormRoleUser::getRoleId).distinct().toList();
            LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.in(CustomFormRole::getId, roleIds);
            List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
            roles.forEach(r -> formIds.add(r.getCustomFormId()));
        }

        return formIds;
    }

    private void createBuiltinRoles(String formId, String userId) {
        long now = System.currentTimeMillis();
        List<CustomFormRole> roles = Arrays.stream(CustomFormRoleKey.values())
                .map(key -> {
                    CustomFormRole role = new CustomFormRole();
                    role.setId(IDGenerator.nextStr());
                    role.setName(key.getName());
                    role.setCustomFormId(formId);
                    role.setInternalKey(key.getKey());
                    role.setCreateTime(now);
                    role.setUpdateTime(now);
                    role.setCreateUser(userId);
                    role.setUpdateUser(userId);
                    return role;
                }).toList();
        customFormRoleMapper.batchInsert(roles);
    }
}
