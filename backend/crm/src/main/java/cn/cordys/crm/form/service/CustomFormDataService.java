package cn.cordys.crm.form.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.*;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataBatchUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormDataMapper;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CustomFormDataService {

    @Resource
    private BaseMapper<CustomFormData> customFormDataMapper;
    @Resource
    private ExtCustomFormDataMapper extCustomFormDataMapper;
    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private CustomFormDataFieldService customFormDataFieldService;
    @Resource
    private CustomFormService customFormService;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private BaseMapper<CustomFormDataField> customFormDataFieldMapper;
    @Resource
    private BaseMapper<CustomFormDataFieldBlob> customFormDataFieldBlobMapper;

    public PagerWithOption<List<CustomFormDataListResponse>> page(CustomFormDataPageRequest request, String userId, String orgId, boolean checkDataPermission) {
        String formId = request.getCustomFormId();
        boolean manageOwn = false;
        CustomFormRoleKey dataScope;
        if (checkDataPermission) {
            dataScope = getDataScope(formId, userId);
            manageOwn = dataScope == CustomFormRoleKey.MANAGE_OWN;
        } else {
            dataScope = CustomFormRoleKey.VIEW_ALL;
        }

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomFormDataListResponse> list = extCustomFormDataMapper.list(request, orgId, userId, manageOwn);
        CustomFormDataFieldService.setFormKey(formId);
        try {
            list = buildList(list, formId, orgId);
            Map<String, List<OptionDTO>> optionMap = buildOptionMap(formId, orgId, list);
            list.forEach(item -> item.setIsAdmin(isAdminUser(dataScope, userId, item.getOwner())));
            return PageUtils.setPageInfoWithOption(page, list, optionMap);
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
    }

    private boolean isAdminUser(CustomFormRoleKey dataScope, String userId, String owner) {
        return dataScope == CustomFormRoleKey.MANAGE_ALL ||
                (dataScope == CustomFormRoleKey.MANAGE_OWN && StringUtils.equals(owner, userId));
    }

    public List<CustomFormDataListResponse> buildList(List<CustomFormDataListResponse> list, String formId, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }

        List<String> dataIds = list.stream().map(CustomFormDataListResponse::getId).toList();

        Map<String, List<BaseModuleFieldValue>> fieldMap = customFormDataFieldService.getResourceFieldMap(dataIds, true);
        Map<String, List<BaseModuleFieldValue>> resolvefieldValueMap = customFormDataFieldService.setBusinessRefFieldValue(list, moduleFormService.getFlattenFormFields(formId, orgId), fieldMap);

        list.forEach(resp -> {
            resp.setModuleFields(resolvefieldValueMap.get(resp.getId()));
        });

        return baseService.setCreateUpdateOwnerUserName(list);
    }


    private Map<String, List<OptionDTO>> buildOptionMap(String formId, String orgId, List<CustomFormDataListResponse> list) {
        ModuleForm moduleForm = moduleFormMapper.selectByPrimaryKey(formId);
        if (moduleForm == null || CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }

        ModuleFormConfigDTO formConfig = moduleFormService.getBusinessFormConfig(moduleForm.getFormKey(), orgId);
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, CustomFormDataListResponse::getModuleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFieldValues);

        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(list,
                CustomFormDataListResponse::getOwner, CustomFormDataListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOM_FORM_DATA_OWNER.getBusinessKey(), ownerFieldOption);

        return optionMap;
    }

    public CustomFormDataGetResponse get(String id, String userId, String orgId) {
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(id);
        if (data == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }
        CustomFormRoleKey dataScope = getDataScope(data.getCustomFormId(), userId);
        if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(data.getCreateUser(), userId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        CustomFormDataGetResponse resp = BeanUtils.copyBean(new CustomFormDataGetResponse(), data);
        resp.setIsAdmin(isAdminUser(dataScope, userId, data.getOwner()));

        Map<String, String> userNameMap = baseService.getUserNameMap(
                List.of(data.getOwner(), data.getCreateUser(), data.getUpdateUser())
        );
        resp.setOwnerName(userNameMap.get(data.getOwner()));
        resp.setCreateUserName(userNameMap.get(data.getCreateUser()));
        resp.setUpdateUserName(userNameMap.get(data.getUpdateUser()));

        CustomFormDataFieldService.setFormKey(data.getCustomFormId());
        try {
            ModuleFormConfigDTO formConfig = moduleFormCacheService.getBusinessFormConfig(data.getCustomFormId(), orgId);
            List<BaseModuleFieldValue> moduleFields = customFormDataFieldService.getModuleFieldValuesByResourceId(id);
            Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(formConfig, moduleFields);
            optionMap.put(BusinessModuleField.CUSTOM_FORM_DATA_OWNER.getBusinessKey(),
                    moduleFormService.getBusinessFieldOption(List.of(resp),
                            CustomFormDataGetResponse::getOwner, CustomFormDataGetResponse::getOwnerName));
            moduleFormService.processBusinessFieldValues(resp, moduleFields, formConfig);
            resp.setAttachmentMap(moduleFormService.getAttachmentMap(formConfig, moduleFields));
            resp.setOptionMap(optionMap);
            resp.setModuleFields(moduleFields);
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }

        return resp;
    }

    /**
     * 获取详情（⚠️反射调用; 勿修改入参, 返回, 方法名!）
     *
     * @param id 订单ID
     *
     * @return 详情
     */
    public CustomFormDataGetResponse getSimple(String id) {
        CustomFormData customFormData = customFormDataMapper.selectByPrimaryKey(id);
        if (customFormData == null) {
            return null;
        }
        CustomFormDataGetResponse customFormDataGetResponse = BeanUtils.copyBean(new CustomFormDataGetResponse(), customFormData);
        // 获取模块字段
        List<BaseModuleFieldValue> customFormDataFields = customFormDataFieldService.getModuleFieldValuesByResourceId(id);
        customFormDataGetResponse.setModuleFields(customFormDataFields);
        return customFormDataGetResponse;
    }

    /**
     * 批量获取自定义数据详情 (用于数据源批量查询优化)
     * @param ids ID集合
     * @return 详情列表
     */
    public List<CustomFormDataGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // 批量查询资源基本信息
        List<CustomFormData> customFormData = customFormDataMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(customFormData)) {
            return Collections.emptyList();
        }
        // 批量查询自定义字段值
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = customFormDataFieldService.getResourceFieldMap(ids, true);

        // 组装结果
        return customFormData.stream().map(clue -> {
            CustomFormDataGetResponse response = BeanUtils.copyBean(new CustomFormDataGetResponse(), clue);
            response.setModuleFields(fieldValueMap.get(clue.getId()));
            return response;
        }).toList();
    }

    @OperationLog(module = LogModule.CUSTOM_FORM_DATA, type = LogType.ADD)
    public CustomFormData add(CustomFormDataAddRequest request, String userId, String orgId) {
        CustomFormData data = new CustomFormData();
        data.setId(IDGenerator.nextStr());
        data.setCustomFormId(request.getCustomFormId());
        data.setName(request.getName());
        data.setOwner(StringUtils.isNotBlank(request.getOwner()) ? request.getOwner() : userId);
        data.setOrganizationId(orgId);
        data.setCreateTime(System.currentTimeMillis());
        data.setUpdateTime(System.currentTimeMillis());
        data.setCreateUser(userId);
        data.setUpdateUser(userId);

        CustomFormDataFieldService.setFormKey(request.getCustomFormId());
        try {
            customFormDataFieldService.saveModuleField(data, orgId, userId, request.getModuleFields(), false);
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
        customFormDataMapper.insert(data);

        baseService.handleAddLogWithResourceName(data, request.getModuleFields());

        return data;
    }

    @OperationLog(module = LogModule.CUSTOM_FORM_DATA, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void update(CustomFormDataUpdateRequest request, String userId, String orgId) {
        CustomFormData originData = customFormDataMapper.selectByPrimaryKey(request.getId());
        if (originData == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        CustomFormRoleKey dataScope = getDataScope(originData.getCustomFormId(), userId);
        checkWritePermission(dataScope, originData.getCreateUser(), userId);

        CustomFormData updateData = new CustomFormData();
        updateData.setId(request.getId());
        updateData.setName(request.getName());
        updateData.setOwner(request.getOwner());
        updateData.setUpdateTime(System.currentTimeMillis());
        updateData.setUpdateUser(userId);
        customFormDataMapper.update(updateData);


        CustomFormDataFieldService.setFormKey(originData.getCustomFormId());
        try {
            if (request.getModuleFields() != null) {
                List<BaseModuleFieldValue> originFields = customFormDataFieldService.getModuleFieldValuesByResourceId(request.getId());
                // 过滤掉引用字段（显示字段），这些字段不需要参与日志对比
                List<BaseModuleFieldValue> logOriginFields = filterRefFields(originFields);
                List<BaseModuleFieldValue> logModifiedFields = filterRefFields(request.getModuleFields());
				baseService.handleUpdateLog(originData, updateData, logOriginFields, logModifiedFields, originData.getId(), originData.getName());
                customFormDataFieldService.deleteByResourceId(request.getId());
                customFormDataFieldService.saveModuleField(updateData, orgId, userId, request.getModuleFields(), true);
            } else {
                baseService.handleUpdateLog(originData, updateData, null, null, originData.getId(), originData.getName());
            }
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
    }

    @OperationLog(module = LogModule.CUSTOM_FORM_DATA, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId) {
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(id);
        if (data == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }

        CustomFormRoleKey dataScope = getDataScope(data.getCustomFormId(), userId);
        checkWritePermission(dataScope, data.getCreateUser(), userId);

        customFormDataFieldService.deleteByResourceId(id);
        customFormDataMapper.deleteByPrimaryKey(id);

        // 设置操作对象
        OperationLogContext.setResourceName(data.getName());
    }

    public void batchUpdate(CustomFormDataBatchUpdateRequest request, String userId, String orgId) {
        List<CustomFormData> dataList = customFormDataMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }
        checkBatchPermission(userId, dataList, request.getCustomFormId());
        CustomFormDataFieldService.setFormKey(request.getCustomFormId());
        try {
            BaseField field = customFormDataFieldService.getAndCheckField(request.getFieldId(), orgId);
            customFormDataFieldService.batchUpdate(request, field, dataList, CustomFormData.class, LogModule.CUSTOM_FORM_DATA, extCustomFormDataMapper::batchUpdate, userId, orgId);
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
    }

    private void checkBatchPermission(String userId, List<CustomFormData> dataList, String formId) {
        CustomFormRoleKey dataScope = getDataScope(formId, userId);
        if (dataScope == CustomFormRoleKey.VIEW_ALL) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        for (CustomFormData customFormData : dataList) {
            if (!Strings.CI.equals(customFormData.getCustomFormId(), formId)) {
                // 数据所属表单不一致，禁止批量操作
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(customFormData.getOwner(), userId)) {
                // 仅能操作自己负责的数据，且数据负责人不为当前用户，禁止操作
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
        }
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<CustomFormData> dataList = customFormDataMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        String formId = dataList.getFirst().getCustomFormId();
        checkBatchPermission(userId, dataList, formId);

        List<String> deletableIds = dataList.stream()
                .map(CustomFormData::getId)
                .toList();

        customFormDataFieldService.deleteByResourceIds(deletableIds);
        customFormDataMapper.deleteByIds(deletableIds);

        List<LogDTO> logs = dataList.stream()
                .map(data ->
                        new LogDTO(orgId, data.getId(), userId, LogType.DELETE, LogModule.CUSTOM_FORM_DATA, data.getName())
                )
                .toList();
        logService.batchAdd(logs);
    }

    private void checkWritePermission(CustomFormRoleKey dataScope, String dataCreateUser, String currentUserId) {
        if (dataScope == CustomFormRoleKey.VIEW_ALL) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(dataCreateUser, currentUserId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    CustomFormRoleKey getDataScope(String formId, String userId) {
        return getDataScope(formId, userId, true);
    }

    CustomFormRoleKey getDataScope(String formId, String userId, boolean checkEnable) {
        CustomForm customForm = customFormMapper.selectByPrimaryKey(formId);
        if (customFormService.isFormAdminUser(formId, userId)) {
            // 管理员管理所有数据
            return CustomFormRoleKey.MANAGE_ALL;
        }

        if (checkEnable && BooleanUtils.isFalse(customForm.getEnable())) {
            // 表单未启用，非管理员没有权限查看
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        // check role membership
        LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(CustomFormRole::getCustomFormId, formId);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
        if (CollectionUtils.isEmpty(roles)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        Map<String, CustomFormRoleKey> roleKeyMap = roles.stream()
                .collect(Collectors.toMap(CustomFormRole::getId, r -> {
                    for (CustomFormRoleKey key : CustomFormRoleKey.values()) {
                        if (key.getKey().equals(r.getInternalKey())) {
                            return key;
                        }
                    }
                    return null;
                }));
        List<String> roleIds = roles.stream().map(CustomFormRole::getId).toList();

        LambdaQueryWrapper<CustomFormRoleUser> ruWrapper = new LambdaQueryWrapper<>();
        ruWrapper.in(CustomFormRoleUser::getRoleId, roleIds).eq(CustomFormRoleUser::getUserId, userId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(ruWrapper);

        if (CollectionUtils.isEmpty(roleUsers)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        Set<CustomFormRoleKey> userRoleKeys = roleUsers.stream()
                .map(ru -> roleKeyMap.get(ru.getRoleId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userRoleKeys.contains(CustomFormRoleKey.MANAGE_ALL)) {
            return CustomFormRoleKey.MANAGE_ALL;
        }
        if (userRoleKeys.contains(CustomFormRoleKey.VIEW_ALL)) {
            return CustomFormRoleKey.VIEW_ALL;
        }
        if (userRoleKeys.contains(CustomFormRoleKey.MANAGE_OWN)) {
            return CustomFormRoleKey.MANAGE_OWN;
        }

        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }

    public String getNameStrByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return StringUtils.EMPTY;
        }
        List<CustomFormData> customFormDataList = customFormDataMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(customFormDataList)) {
            List<String> names = customFormDataList.stream().map(CustomFormData::getName).toList();
            return String.join(",", names);
        }
        return StringUtils.EMPTY;
    }

    public List<CustomFormData> selectByNames(List<String> names) {
        LambdaQueryWrapper<CustomFormData> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(CustomFormData::getName, names);
        return customFormDataMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public String getNameById(String id) {
        CustomFormData customFormData = customFormDataMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(customFormData).map(CustomFormData::getName).orElse(null);
    }

    /**
     * 下载导入模板
     *
     * @param response   响应
     * @param customFormId 自定义表单ID
     * @param orgId      组织ID
     */
    public void downloadImportTpl(HttpServletResponse response, String customFormId, String orgId) {
        CustomForm customForm = customFormMapper.selectByPrimaryKey(customFormId);
        String formName = customForm != null ? customForm.getName() : StringUtils.EMPTY;
        new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, moduleFormService.getCustomImportHeadsNoRef(customFormId, orgId),
                        Translator.getWithArgs("custom_form_data.import_tpl.name", formName), Translator.get("sheet.data"), Translator.get("sheet.comment"),
                        new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(customFormId, orgId)),
                        new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入预检查
     *
     * @param file         导入文件
     * @param customFormId 自定义表单ID
     * @param orgId        组织ID
     *
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String customFormId, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, customFormId, orgId);
    }

    /**
     * 自定义表单数据导入
     *
     * @param file         导入文件
     * @param customFormId 自定义表单ID
     * @param orgId        组织ID
     * @param userId       用户ID
     *
     * @return 导入结果
     */
    public ImportResponse realImport(MultipartFile file, String customFormId, String orgId, String userId) {
        try {
            CustomFormDataFieldService.setFormKey(customFormId);
            List<BaseField> fields = moduleFormService.getAllFields(customFormId, orgId);
            CustomImportAfterDoConsumer<CustomFormData, BaseResourceSubField> afterDo = (dataList, fieldList, fieldBlobList) -> {
                var logs = new ArrayList<LogDTO>();
                dataList.forEach(data -> {
                    data.setCustomFormId(customFormId);
                    data.setOrganizationId(orgId);
                    if (StringUtils.isBlank(data.getOwner())) {
                        data.setOwner(userId);
                    }
                    logs.add(new LogDTO(orgId, data.getId(), userId, LogType.ADD, LogModule.CUSTOM_FORM_DATA, data.getName()));
                });
                customFormDataMapper.batchInsert(dataList);
                customFormDataFieldMapper.batchInsert(fieldList.stream()
                        .map(field -> BeanUtils.copyBean(new CustomFormDataField(), field)).toList());
                customFormDataFieldBlobMapper.batchInsert(fieldBlobList.stream()
                        .map(field -> BeanUtils.copyBean(new CustomFormDataFieldBlob(), field)).toList());
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<CustomFormData> eventListener = new CustomFieldImportEventListener<>(
                    fields, CustomFormData.class, orgId, userId, "custom_form_data_field", afterDo, 2000, null, null);
            FastExcelFactory.read(file.getInputStream(), eventListener)
                    .headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("custom form data import error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
    }

    /**
     * 检查导入文件
     *
     * @param file         文件
     * @param customFormId 自定义表单ID
     * @param orgId        组织ID
     *
     * @return 检查结果
     */
    private ImportResponse checkImportExcel(MultipartFile file, String customFormId, String orgId) {
        try {
            CustomFormDataFieldService.setFormKey(customFormId);
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(customFormId, orgId);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "custom_form_data", "custom_form_data_field", orgId);
            FastExcelFactory.read(file.getInputStream(), eventListener)
                    .headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("custom form data import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        } finally {
            CustomFormDataFieldService.clearFormKey();
        }
    }

    /**
     * 过滤掉引用字段（显示字段），这些字段不需要参与日志对比
     */
    private List<BaseModuleFieldValue> filterRefFields(List<BaseModuleFieldValue> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return fields;
        }
        return fields.stream()
                .filter(f -> !f.getFieldId().contains(BaseResourceFieldService.REF_UNDERLINE))
                .toList();
    }
}
