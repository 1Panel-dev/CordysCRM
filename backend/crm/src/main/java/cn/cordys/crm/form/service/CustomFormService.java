package cn.cordys.crm.form.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.*;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.form.mapper.ExtCustomFormMapper;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigLogDTO;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomFormService {

    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private ExtCustomFormMapper extCustomFormMapper;
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
	private ModuleFormService moduleFormService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ExtCustomFormDataMapper extCustomFormDataMapper;
    @Resource
    private ExtUserMapper extUserMapper;

    @Value("classpath:form/form.json")
    private org.springframework.core.io.Resource formResource;

    public List<OptionDTO> getOptions() {
        LambdaQueryWrapper<CustomForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomForm::getEnable, true);
        return customFormMapper.selectListByLambda(wrapper).stream()
                .map(form -> new OptionDTO(form.getId(), form.getName()))
                .toList();
    }

    public List<CustomFormListResponse> list(String userId) {
        List<CustomForm> customForms;
        Set<String> adminFormIds;
        Set<String> hasCreatePermissionFormIds;
        if (Strings.CS.equals(InternalUser.ADMIN.getValue(), userId)) {
            customForms = customFormMapper.selectAll(null);
            adminFormIds = customForms.stream().map(CustomForm::getId).collect(Collectors.toSet());
            hasCreatePermissionFormIds = adminFormIds;
        } else {
            adminFormIds = getAdminFormIds(userId);
            List<CustomFormRole> formRoles = getFormRoles(userId);
            // formRoles 按 customFormId 分组，后续可以展示角色信息
            Map<String, List<CustomFormRole>> roleMap = formRoles.stream()
                    .collect(Collectors.groupingBy(CustomFormRole::getCustomFormId));
            Set<String> memberFormIds = roleMap.keySet();

            if (adminFormIds.isEmpty() && memberFormIds.isEmpty()) {
                return Collections.emptyList();
            }

            Set<String> accessibleFormIds = new HashSet<>();
            accessibleFormIds.addAll(adminFormIds);
            accessibleFormIds.addAll(memberFormIds);

            customForms = customFormMapper.selectByIds(accessibleFormIds.stream().toList());

            hasCreatePermissionFormIds = getCreatePermissionFormIds(customForms, roleMap);

            // 非管理员只能看到启用的表单
            customForms = customForms.stream()
                    .filter(form -> adminFormIds.contains(form.getId()) || BooleanUtils.isTrue(form.getEnable()))
                    .toList();
        }
        Set<String> finalHasCreatePermissionFormIds = hasCreatePermissionFormIds;
        return customForms.stream()
                .map(form -> {
                    CustomFormListResponse resp = new CustomFormListResponse();
                    resp.setId(form.getId());
                    resp.setName(form.getName());
                    resp.setEnable(form.getEnable());
                    resp.setOrganizationId(form.getOrganizationId());
                    // 标记是否是管理员
                    resp.setIsAdmin(adminFormIds.contains(form.getId()));
                    resp.setHasCreateDataPermission(finalHasCreatePermissionFormIds.contains(form.getId()));
                    return resp;
                }).toList();
    }

    private Set<String> getCreatePermissionFormIds(List<CustomForm> customForms, Map<String, List<CustomFormRole>> roleMap) {
        Set<String> hasCreatePermissionFormIds = new HashSet<>();
        for (CustomForm form : customForms) {
            // 如果角色中有 CustomFormRoleKey 中的 MANAGE_ALL 或者 MANAGE_OWN，则有创建权限
            List<CustomFormRole> roles = roleMap.get(form.getId());
            boolean hasCreatePermission = roles != null && roles.stream()
                    .anyMatch(role -> CustomFormRoleKey.MANAGE_ALL.getKey().equals(role.getInternalKey())
                            || CustomFormRoleKey.MANAGE_OWN.getKey().equals(role.getInternalKey()));
            if (hasCreatePermission) {
                hasCreatePermissionFormIds.add(form.getId());
            }
        }
        return hasCreatePermissionFormIds;
    }

    public CustomFormGetResponse get(String id, String userId, String orgId) {
        checkFormAccess(id, userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(id);
        ModuleForm moduleForm = moduleFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        CustomFormGetResponse resp = BeanUtils.copyBean(new CustomFormGetResponse(), form);
        resp.setCreator(getUserOption(form.getCreateUser()));

        if (moduleForm != null) {
            ModuleFormConfigDTO businessFormConfig = moduleFormService.getBusinessFormConfig(moduleForm.getFormKey(), orgId);
            resp.setFields(businessFormConfig.getFields());
            resp.setFormProp(businessFormConfig.getFormProp());
        }

        resp.setIsAdmin(isFormAdminUser(form.getId(), userId));
        return resp;
    }

    private OptionDTO getUserOption(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return extUserMapper.selectUserOptionByIds(List.of(userId)).stream()
                .findFirst()
                .orElseGet(() -> new OptionDTO(userId, null));
    }

    public List<OptionDTO> getAdmins(String formId, String userId) {
        checkFormAccess(formId, userId);
        return getAdminOptions(formId);
    }

    private List<OptionDTO> getAdminOptions(String formId) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getCustomFormId, formId);
        List<CustomFormAdmin> admins = customFormAdminMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(admins)) {
            return Collections.emptyList();
        }

        List<String> userIds = admins.stream()
                .map(CustomFormAdmin::getUserId)
                .distinct()
                .toList();
        Map<String, String> userNameMap = new HashMap<>(userIds.size());
        extUserMapper.selectUserOptionByIds(userIds)
                .forEach(option -> userNameMap.putIfAbsent(option.getIdAsString(), option.getName()));
        return userIds.stream()
                .map(uid -> new OptionDTO(uid, userNameMap.get(uid)))
                .toList();
    }

    @OperationLog(module = LogModule.CUSTOM_FORM, type = LogType.ADD, resourceName = "{#request.name}")
    public CustomForm create(CustomFormAddRequest request, String userId, String orgId) {
        checkNameUnique(request.getName(), null);
        String formId = IDGenerator.nextStr();

        // 保存 custom_form
        CustomForm form = new CustomForm();
        form.setId(formId);
        form.setName(request.getName());
        form.setEnable(!BooleanUtils.isFalse(request.getEnable()));
        form.setOrganizationId(orgId);
        form.setCreateTime(System.currentTimeMillis());
        form.setUpdateTime(System.currentTimeMillis());
        form.setCreateUser(userId);
        form.setUpdateUser(userId);

        checkAddExist(form);
        customFormMapper.insert(form);

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(formId)
                        .modifiedValue(form)
                        .build()
        );

        // 保存 sys_module_form 使用相同的 ID, formKey 也使用 formId
        ModuleForm moduleForm = new ModuleForm();
        moduleForm.setId(formId);
        moduleForm.setFormKey(formId);
        moduleForm.setOrganizationId(orgId);
        moduleForm.setCreateTime(System.currentTimeMillis());
        moduleForm.setUpdateTime(System.currentTimeMillis());
        moduleForm.setCreateUser(userId);
        moduleForm.setUpdateUser(userId);
        moduleFormMapper.insert(moduleForm);

        // 保存表单配置
        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(formId);
        FormProp formProp = getFormPropForCreate(request.getFormProp());
        formBlob.setProp(JSON.toJSONString(formProp));
        moduleFormBlobMapper.insert(formBlob);

        // 校验字段
        moduleFormService.preCheckForFieldSave(formId, request.getFields());

        // 保存字段
        moduleFormService.saveFields(request.getFields(), form.getId(), userId);

        // 创建内置角色
        createBuiltinRoles(formId, userId);

        // 将当前用户设置为管理员
        createFormAdmin(userId, formId);

        return form;
    }

    private void createFormAdmin(String userId, String formId) {
        CustomFormAdmin admin = new CustomFormAdmin();
        admin.setId(IDGenerator.nextStr());
        admin.setCustomFormId(formId);
        admin.setUserId(userId);
        customFormAdminMapper.insert(admin);
    }

    private FormProp getFormPropForCreate(FormProp createFormProp) {
        if (createFormProp == null) {
            try {
                return JSON.parseObject(formResource.getInputStream(), FormProp.class);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return new FormProp();
            }
        }
        return createFormProp;
    }

    @OperationLog(module = LogModule.CUSTOM_FORM, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void update(CustomFormUpdateRequest request, String userId, String orgId) {
        checkFormAdmin(request.getId(), userId);
        CustomForm originForm = customFormMapper.selectByPrimaryKey(request.getId());
        ModuleForm originModuleForm = moduleFormMapper.selectByPrimaryKey(request.getId());
        if (originForm == null || originModuleForm == null) {
            throw new GenericException(Translator.get("custom.form.not.exist"));
        }

        CustomForm updateForm = new CustomForm();
        updateForm.setId(request.getId());
        updateForm.setName(request.getName());
        updateForm.setEnable(request.getEnable());

        checkUpdateExist(updateForm);
        customFormMapper.update(updateForm);

        ModuleFormSaveRequest moduleFormRequest = new ModuleFormSaveRequest();
        moduleFormRequest.setFormKey(originModuleForm.getFormKey());
        moduleFormRequest.setFormProp(request.getFormProp());
        moduleFormRequest.setFields(request.getFields());

        // 获取表单日志信息
        LogContextInfo formChangeLogContext = moduleFormService.getModuleFormChangeLogContext(originModuleForm.getFormKey(), orgId, moduleFormRequest);
		formChangeLogContext.setResourceName(request.getName());
        // 追加自定义表单名称
        if (formChangeLogContext.getOriginalValue() instanceof ModuleFormConfigLogDTO originalLog) {
            originalLog.setName(originForm.getName());
        }
        if (formChangeLogContext.getModifiedValue() instanceof ModuleFormConfigLogDTO modifiedLog) {
            modifiedLog.setName(request.getName());
        }

        // 保存表单配置
        moduleFormCacheService.saveWithoutLog(moduleFormRequest, userId, orgId);

		// 设置日志上下文
        OperationLogContext.setContext(formChangeLogContext);
    }

    @OperationLog(module = LogModule.CUSTOM_FORM, type = LogType.UPDATE, resourceId = "{#id}")
    public void updateEnable(String id, String userId, boolean enable) {
        checkFormAdmin(id, userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("custom.form.not.exist"));
        }

        CustomForm updateForm = new CustomForm();
        updateForm.setId(id);
        updateForm.setEnable(enable);
        customFormMapper.update(updateForm);
		updateForm.setName(form.getName());

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(form.getName())
                        .originalValue(form)
                        .modifiedValue(updateForm)
                        .build()
        );
    }

    @OperationLog(module = LogModule.CUSTOM_FORM, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId) {
        checkFormAdmin(id, userId);

        CustomForm form = customFormMapper.selectByPrimaryKey(id);
        if (form == null) {
            throw new GenericException(Translator.get("custom.form.not.exist"));
        }

        // 设置操作对象
        OperationLogContext.setResourceName(form.getName());

        // 删除表单管理员
        deleteCustomFormAdminByFormId(id);

        // 删除表单角色和成员
        deleteCustomFormRoleAndUser(id);

        // 删除表单数据（多表联删，一次扫描）
        extCustomFormDataMapper.deleteFormDataByCustomFormId(id);

        // 删除表单
        customFormMapper.deleteByIds(List.of(id));
    }

    private void deleteCustomFormRoleAndUser(String formId) {
        LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(CustomFormRole::getCustomFormId, formId);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
        if (CollectionUtils.isNotEmpty(roles)) {
            List<String> roleIds = roles.stream().map(CustomFormRole::getId).toList();
            LambdaQueryWrapper<CustomFormRoleUser> roleUserWrapper = new LambdaQueryWrapper<>();
            roleUserWrapper.in(CustomFormRoleUser::getRoleId, roleIds);
            customFormRoleUserMapper.deleteByLambda(roleUserWrapper);
            customFormRoleMapper.deleteByLambda(roleWrapper);
        }
    }

    private void deleteCustomFormAdminByFormId(String formId) {
        LambdaQueryWrapper<CustomFormAdmin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(CustomFormAdmin::getCustomFormId, formId);
        customFormAdminMapper.deleteByLambda(adminWrapper);
    }

    @OperationLog(module = LogModule.CUSTOM_FORM, type = LogType.UPDATE, resourceId = "{#request.customFormId}")
    public void setAdmins(CustomFormAdminBatchRequest request, String userId) {
        checkFormAdmin(request.getCustomFormId(), userId);

        List<String> originUserIds = getAdminOptions(request.getCustomFormId()).stream().map(OptionDTO::getIdAsString).toList();

        String formId = request.getCustomFormId();
        deleteCustomFormAdminByFormId(formId);
        insertAdmins(formId, request.getUserIds());

        CustomForm customForm = customFormMapper.selectByPrimaryKey(formId);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(customForm.getName())
                        .originalValue(Map.of("adminUserIds", originUserIds))
                        .modifiedValue(Map.of("adminUserIds", request.getUserIds()))
                        .build()
        );
    }

    private void insertAdmins(String formId, List<String> userIds) {
        List<CustomFormAdmin> toInsert = userIds.stream()
                .distinct()
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

    private void checkFormAccess(String formId, String userId) {
        if (InternalUser.ADMIN.getValue().equals(userId)) {
            return;
        }

        CustomFormAdmin example = new CustomFormAdmin();
        example.setCustomFormId(formId);
        example.setUserId(userId);
        Long adminCount = customFormAdminMapper.countByExample(example);
        if (adminCount > 0) {
            return;
        }

        LambdaQueryWrapper<CustomFormRoleUser> ruWrapper = new LambdaQueryWrapper<>();
        ruWrapper.eq(CustomFormRoleUser::getUserId, userId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(ruWrapper);
        if (CollectionUtils.isNotEmpty(roleUsers)) {
            List<String> roleIds = roleUsers.stream().map(CustomFormRoleUser::getRoleId).distinct().toList();
            LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(CustomFormRole::getCustomFormId, formId)
                    .in(CustomFormRole::getId, roleIds);
            List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
            if (CollectionUtils.isNotEmpty(roles)) {
                return;
            }
        }

        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }

    private void checkFormAdmin(String formId, String userId) {
        if (!isFormAdminUser(formId, userId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    public Boolean isFormAdminUser(String formId, String userId) {
        if (InternalUser.ADMIN.getValue().equals(userId)) {
            return true;
        }
        CustomFormAdmin example = new CustomFormAdmin();
        example.setCustomFormId(formId);
        example.setUserId(userId);
        return customFormAdminMapper.countByExample(example) > 0;
    }

    Set<String> getAdminFormIds(String userId) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getUserId, userId);
        List<CustomFormAdmin> admins = customFormAdminMapper.selectListByLambda(wrapper);
        return admins.stream().map(CustomFormAdmin::getCustomFormId).collect(Collectors.toSet());
    }

    private List<CustomFormRole> getFormRoles(String userId) {
        LambdaQueryWrapper<CustomFormRoleUser> ruWrapper = new LambdaQueryWrapper<>();
        ruWrapper.eq(CustomFormRoleUser::getUserId, userId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(ruWrapper);
        if (CollectionUtils.isNotEmpty(roleUsers)) {
            List<String> roleIds = roleUsers.stream().map(CustomFormRoleUser::getRoleId).distinct().toList();
            LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.in(CustomFormRole::getId, roleIds);
            List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
            return roles;
        }
        return List.of();
    }

    private void checkNameUnique(String name, String excludeFormId) {
        LambdaQueryWrapper<CustomForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomForm::getName, name);
        List<CustomForm> existForms = customFormMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isNotEmpty(existForms)) {
            boolean conflict = existForms.stream()
                    .anyMatch(f -> excludeFormId == null || !excludeFormId.equals(f.getId()));
            if (conflict) {
                throw new GenericException(Translator.get("custom.form.name.duplicate"));
            }
        }
    }

    private void checkAddExist(CustomForm customForm) {
        if (extCustomFormMapper.checkAddExist(customForm)) {
            throw new GenericException(Translator.get("custom.form.name.duplicate"));
        }
    }

    private void checkUpdateExist(CustomForm customForm) {
        if (StringUtils.isNotBlank(customForm.getName()) && extCustomFormMapper.checkUpdateExist(customForm)) {
            throw new GenericException(Translator.get("custom.form.name.duplicate"));
        }
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
