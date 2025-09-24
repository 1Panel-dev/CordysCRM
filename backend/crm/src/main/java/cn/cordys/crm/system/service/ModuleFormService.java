package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.resolver.field.TextMultipleResolver;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.TransformSourceApplyDTO;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.*;
import cn.cordys.crm.system.dto.form.FormLinkFill;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.form.base.LinkField;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ModuleFormService {

    public static final Map<String, String> TYPE_SOURCE_MAP;
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final String CONTROL_RULES_KEY = "showControlRules";

    static {
        TYPE_SOURCE_MAP = Map.of(FieldType.MEMBER.name(), "sys_user",
                FieldType.DEPARTMENT.name(), "sys_department", FieldSourceType.CUSTOMER.name(), "customer",
                FieldSourceType.CLUE.name(), "clue", FieldSourceType.CONTACT.name(), "customer_contact",
                FieldSourceType.OPPORTUNITY.name(), "opportunity", FieldSourceType.PRODUCT.name(), "product");
    }

    @Value("classpath:form/form.json")
    private org.springframework.core.io.Resource formResource;
    @Value("classpath:form/field.json")
    private org.springframework.core.io.Resource fieldResource;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<Attachment> attachmentMapper;

    /**
     * 获取模块表单配置
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
     *
     * @return 字段集合
     */
    public ModuleFormConfigDTO getConfig(String formKey, String currentOrgId) {
        ModuleFormConfigDTO formConfig = new ModuleFormConfigDTO();
        // set form
        LambdaQueryWrapper<ModuleForm> formWrapper = new LambdaQueryWrapper<>();
        formWrapper.eq(ModuleForm::getFormKey, formKey).eq(ModuleForm::getOrganizationId, currentOrgId);
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(formWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            throw new GenericException(Translator.get("module.form.not_exist"));
        }
        ModuleForm form = forms.getFirst();
        ModuleFormBlob formBlob = moduleFormBlobMapper.selectByPrimaryKey(form.getId());
        formConfig.setFormProp(JSON.parseObject(formBlob.getProp(), FormProp.class));
        // set fields
        formConfig.setFields(getAllFields(form.getId()));
        return formConfig;
    }

    /**
     * 获取业务表单配置
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     *
     * @return 表单配置
     */
    public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
        ModuleFormConfigDTO config = getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

        // 获取特殊的业务字段
        Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
                collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

        businessModuleFormConfig.setFields(
                config.getFields()
                        .stream()
                        .peek(moduleFieldDTO -> {
                            BusinessModuleField businessModuleFieldEnum = businessModuleFieldMap.get(moduleFieldDTO.getInternalKey());
                            if (businessModuleFieldEnum != null) {
                                // 设置特殊的业务字段 key
                                moduleFieldDTO.setBusinessKey(businessModuleFieldEnum.getBusinessKey());
                                moduleFieldDTO.setDisabledProps(businessModuleFieldEnum.getDisabledProps());
                            }
                        })
                        .toList()
        );
        return businessModuleFormConfig;
    }

    /**
     * 保存表单配置
     *
     * @param saveParam 保存参数
     *
     * @return 表单配置
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, resourceId = "{#saveParam.formKey}")
    public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
        // 处理表单
        LambdaQueryWrapper<ModuleForm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleForm::getFormKey, saveParam.getFormKey()).eq(ModuleForm::getOrganizationId, currentOrgId);
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(queryWrapper);
        if (CollectionUtils.isEmpty(forms)) {
            throw new GenericException(Translator.get("module.form.not_exist"));
        }
        preCheckForFieldSave(saveParam);
        ModuleFormConfigDTO oldConfig = new ModuleFormConfigDTO();
        oldConfig.setFields(getAllFields(saveParam.getFormKey(), currentOrgId));
        ModuleForm form = forms.getFirst();
        // 旧表单配置
        ModuleFormBlob moduleFormBlob = moduleFormBlobMapper.selectByPrimaryKey(form.getId());
        if (moduleFormBlob != null && StringUtils.isNotEmpty(moduleFormBlob.getProp())) {
            oldConfig.setFormProp(JSON.parseObject(moduleFormBlob.getProp(), FormProp.class));
        }

        form.setUpdateUser(currentUserId);
        form.setUpdateTime(System.currentTimeMillis());
        moduleFormMapper.updateById(form);
        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(form.getId());
        formBlob.setProp(JSON.toJSONString(saveParam.getFormProp()));
        moduleFormBlobMapper.updateById(formBlob);

        // 记录日志上下文
        ModuleFormConfigDTO newConfig = new ModuleFormConfigDTO();
        newConfig.setFields(saveParam.getFields());
        newConfig.setFormProp(saveParam.getFormProp());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(Translator.get(saveParam.getFormKey()) + Translator.get("module.form.setting"))
                        .originalValue(oldConfig)
                        .modifiedValue(newConfig)
                        .build()
        );

        // 处理字段
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModuleField::getFormId, form.getId());
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        if (CollectionUtils.isNotEmpty(fields)) {
            List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
            extModuleFieldMapper.deleteByIds(fieldIds);
            extModuleFieldMapper.deletePropByIds(fieldIds);
        }
        if (CollectionUtils.isNotEmpty(saveParam.getFields())) {
            List<ModuleField> addFields = new ArrayList<>();
            List<ModuleFieldBlob> addFieldBlobs = new ArrayList<>();
            AtomicLong pos = new AtomicLong(1);
            saveParam.getFields().forEach(field -> {
                ModuleField moduleField = new ModuleField();
                moduleField.setId(field.getId());
                moduleField.setFormId(form.getId());
                moduleField.setMobile(field.getMobile() != null && field.getMobile());
                moduleField.setInternalKey(field.getInternalKey());
                moduleField.setType(field.getType());
                moduleField.setName(field.getName());
                moduleField.setPos(pos.getAndIncrement());
                moduleField.setCreateTime(System.currentTimeMillis());
                moduleField.setCreateUser(currentUserId);
                moduleField.setUpdateTime(System.currentTimeMillis());
                moduleField.setUpdateUser(currentUserId);
                addFields.add(moduleField);
                ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                fieldBlob.setId(field.getId());
                fieldBlob.setProp(JSON.toJSONString(field));
                addFieldBlobs.add(fieldBlob);
            });
            if (CollectionUtils.isNotEmpty(addFields)) {
                moduleFieldMapper.batchInsert(addFields);
            }
            if (CollectionUtils.isNotEmpty(addFieldBlobs)) {
                moduleFieldBlobMapper.batchInsert(addFieldBlobs);
            }
        }

        // 返回表单整体配置
        return getConfig(form.getFormKey(), currentOrgId);
    }

    public List<BaseField> getAllFields(String formKey, String orgId) {
        ModuleForm example = new ModuleForm();
        example.setFormKey(formKey);
        example.setOrganizationId(orgId);
        ModuleForm moduleForm = moduleFormMapper.selectOne(example);
        return getAllFields(moduleForm.getId());
    }

    /**
     * 获取表单所有字段及其属性集合
     *
     * @param formId 表单ID
     *
     * @return 字段集合
     */
    public List<BaseField> getAllFields(String formId) {
        // set field
        List<BaseField> fieldDTOList = new ArrayList<>();
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModuleField::getFormId, formId);
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
        if (CollectionUtils.isNotEmpty(fields)) {
            fields.sort(Comparator.comparing(ModuleField::getPos));
            List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
            LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
            blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
            List<ModuleFieldBlob> fieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
            Map<String, String> fieldBlobMap = fieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId, ModuleFieldBlob::getProp));
            fields.forEach(field -> {
                BaseField baseField = JSON.parseObject(fieldBlobMap.get(field.getId()), BaseField.class);
                baseField.setType(field.getType());
                baseField.setMobile(field.getMobile());
                if (baseField.needInitialOptions()) {
                    handleInitialOption(baseField);
                }
                fieldDTOList.add(baseField);
            });
        }
        return fieldDTOList;
    }

    /**
     * 获取字段选项集合
     *
     * @param formConfig    表单配置
     * @param allDataFields 所有数据字段
     *
     * @return 字段选项集合
     */
    public Map<String, List<OptionDTO>> getOptionMap(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allDataFields) {
        Map<String, List<OptionDTO>> optionMap = new HashMap<>(4);
        Map<String, String> idTypeMap = new HashMap<>(8);
        formConfig.getFields().forEach(field -> {
            if (Strings.CS.equalsAny(field.getType(), FieldType.RADIO.name()) && field instanceof RadioField radioField) {
                optionMap.put(field.getBusinessKey() != null ? field.getBusinessKey() : field.getId(), optionPropToDto(radioField.getOptions()));
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.CHECKBOX.name()) && field instanceof CheckBoxField checkBoxField) {
                optionMap.put(field.getBusinessKey() != null ? field.getBusinessKey() : field.getId(), optionPropToDto(checkBoxField.getOptions()));
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name())) {
                if (field instanceof SelectField selectField) {
                    optionMap.put(field.getBusinessKey() != null ? field.getBusinessKey() : field.getId(), optionPropToDto(selectField.getOptions()));
                } else if (field instanceof SelectMultipleField selectMultipleField) {
                    optionMap.put(field.getBusinessKey() != null ? field.getBusinessKey() : field.getId(), optionPropToDto(selectMultipleField.getOptions()));
                }
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.DATA_SOURCE.name(), FieldType.DATA_SOURCE_MULTIPLE.name())) {
                if (field instanceof DatasourceField sourceField) {
                    idTypeMap.put(field.getId(), sourceField.getDataSourceType());
                } else if (field instanceof DatasourceMultipleField sourceMultipleField) {
                    idTypeMap.put(field.getId(), sourceMultipleField.getDataSourceType());
                }
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.MEMBER.name(), FieldType.MEMBER_MULTIPLE.name())) {
                idTypeMap.put(field.getId(), FieldType.MEMBER.name());
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.DEPARTMENT.name(), FieldType.DEPARTMENT_MULTIPLE.name())) {
                idTypeMap.put(field.getId(), FieldType.DEPARTMENT.name());
            }
        });

        Map<String, List<String>> typeIdsMap = new HashMap<>(8);

        if (CollectionUtils.isNotEmpty(allDataFields)) {
            allDataFields.stream().filter(field -> idTypeMap.containsKey(field.getFieldId())).forEach(field -> {
                if (!typeIdsMap.containsKey(field.getFieldId())) {
                    typeIdsMap.put(field.getFieldId(), new ArrayList<>());
                }
                Object fieldValue = field.getFieldValue();
                if (fieldValue instanceof List) {
                    typeIdsMap.get(field.getFieldId()).addAll(JSON.parseArray(JSON.toJSONString(fieldValue), String.class));
                } else {
                    typeIdsMap.get(field.getFieldId()).add(fieldValue.toString());
                }
            });
        }

        typeIdsMap.forEach((fieldId, ids) -> {
            List<OptionDTO> options = extModuleFieldMapper.getSourceOptionsByIds(TYPE_SOURCE_MAP.get(idTypeMap.get(fieldId)), ids);
            if (CollectionUtils.isNotEmpty(options)) {
                optionMap.put(fieldId, options);
            }
        });
        return optionMap;
    }

    public Map<String, List<Attachment>> getAttachmentMap(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allDataFields) {
        List<String> attachmentFieldIds = formConfig.getFields().stream().filter(field -> Strings.CS.equalsAny(field.getType(), FieldType.ATTACHMENT.name())).map(BaseField::getId).toList();
        if (CollectionUtils.isEmpty(attachmentFieldIds)) {
            return null;
        }
        Map<String, List<String>> fieldAttachmentIds = new HashMap<>(attachmentFieldIds.size());
        allDataFields.stream().filter(field -> attachmentFieldIds.contains(field.getFieldId()) && field.getFieldValue() != null).forEach(field -> {
            Object fieldValue = field.getFieldValue();
            List<String> attachmentIds = new ArrayList<>();
            if (fieldValue instanceof List) {
                attachmentIds.addAll(JSON.parseArray(JSON.toJSONString(fieldValue), String.class));
            } else {
                attachmentIds.add(fieldValue.toString());
            }
            fieldAttachmentIds.put(field.getFieldId(), attachmentIds);
        });

        List<String> attachmentIds = fieldAttachmentIds.values().stream().flatMap(List::stream).distinct().toList();
        if (CollectionUtils.isEmpty(attachmentIds)) {
            return null;
        }
        List<Attachment> attachments = attachmentMapper.selectByIds(attachmentIds);
        Map<String, Attachment> attachmentMap = attachments.stream().collect(Collectors.toMap(Attachment::getId, Function.identity()));
        Map<String, List<Attachment>> attachmentMapResult = new HashMap<>(fieldAttachmentIds.size());
        for (Map.Entry<String, List<String>> entry : fieldAttachmentIds.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }
            List<Attachment> fieldAttachments = new ArrayList<>();
            entry.getValue().forEach(attachmentId -> {
                if (attachmentMap.containsKey(attachmentId)) {
                    fieldAttachments.add(attachmentMap.get(attachmentId));
                }
            });
            attachmentMapResult.put(entry.getKey(), fieldAttachments);
        }
        return attachmentMapResult;
    }

    public List<String> resolveSourceNames(String type, List<String> nameList) {
        if (CollectionUtils.isEmpty(nameList)) {
            return new ArrayList<>();
        }
        if (!TYPE_SOURCE_MAP.containsKey(type)) {
            LogUtils.error("未知的数据源类型：{}", type);
            return new ArrayList<>();
        }
        return extModuleFieldMapper.resolveIdsByName(TYPE_SOURCE_MAP.get(type), nameList);
    }

    /**
     * OptionProp转OptionDTO
     *
     * @param options 选项集合
     *
     * @return 选项DTO集合
     */
    public List<OptionDTO> optionPropToDto(List<OptionProp> options) {
        if (options == null || options.isEmpty()) {
            return new ArrayList<>();
        }
        return options.stream().map(option -> {
            OptionDTO optionDTO = new OptionDTO();
            optionDTO.setName(option.getLabel());
            optionDTO.setId(option.getValue());
            return optionDTO;
        }).toList();
    }

    /**
     * 表单初始化
     */
    public void initForm() {
        // init form
        Map<String, String> formKeyMap = new HashMap<>(FormKey.values().length);
        List<ModuleForm> forms = new ArrayList<>();
        List<ModuleFormBlob> formBlobs = new ArrayList<>();
        Arrays.stream(FormKey.values()).forEach(formKey -> {
            ModuleForm form = new ModuleForm();
            form.setId(IDGenerator.nextStr());
            form.setFormKey(formKey.getKey());
            form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            form.setCreateUser("admin");
            form.setCreateTime(System.currentTimeMillis());
            form.setUpdateUser("admin");
            form.setUpdateTime(System.currentTimeMillis());
            forms.add(form);
            formKeyMap.put(formKey.getKey(), form.getId());
            ModuleFormBlob formBlob = new ModuleFormBlob();
            formBlob.setId(form.getId());
            try {
                FormProp formProp = JSON.parseObject(formResource.getInputStream(), FormProp.class);
                formBlob.setProp(JSON.toJSONString(formProp));
            } catch (IOException e) {
                throw new GenericException("表单属性初始化失败", e);
            }
            formBlobs.add(formBlob);
        });
        moduleFormMapper.batchInsert(forms);
        moduleFormBlobMapper.batchInsert(formBlobs);
        // init form fields
        initFormFields(formKeyMap);
    }

    /**
     * 字段初始化
     *
     * @param formKeyMap 表单Key映射
     */
    @SuppressWarnings("unchecked")
    public void initFormFields(Map<String, String> formKeyMap) {
        List<ModuleField> fields = new ArrayList<>();
        List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
        try {
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            fieldMap.keySet().forEach(key -> {
                String formId = formKeyMap.get(key);
                List<Map<String, Object>> initFields = fieldMap.get(key);
                AtomicLong pos = new AtomicLong(1L);
                // 显隐规则Key-ID映射
                Map<String, String> controlKeyPreMap = new HashMap<>(2);
                initFields.forEach(initField -> {
                    ModuleField field = new ModuleField();
                    field.setFormId(formId);
                    field.setInternalKey(initField.get("internalKey").toString());
                    field.setId(controlKeyPreMap.containsKey(field.getInternalKey()) ? controlKeyPreMap.get(field.getInternalKey()) : IDGenerator.nextStr());
                    field.setType(initField.get("type").toString());
                    field.setName(initField.get("name").toString());
                    field.setMobile((Boolean) initField.getOrDefault("mobile", false));
                    field.setPos(pos.getAndIncrement());
                    field.setCreateTime(System.currentTimeMillis());
                    field.setCreateUser(InternalUser.ADMIN.getValue());
                    field.setUpdateTime(System.currentTimeMillis());
                    field.setUpdateUser(InternalUser.ADMIN.getValue());
                    initField.put("id", field.getId());
                    fields.add(field);
                    if (initField.containsKey(CONTROL_RULES_KEY)) {
                        List<ControlRuleProp> controlRules = JSON.parseArray(JSON.toJSONString(initField.get(CONTROL_RULES_KEY)), ControlRuleProp.class);
                        controlRules.forEach(controlRule -> {
                            List<String> showFieldIds = new ArrayList<>();
                            controlRule.getFieldIds().forEach(fieldKey -> {
                                if (!controlKeyPreMap.containsKey(fieldKey)) {
                                    controlKeyPreMap.put(fieldKey, IDGenerator.nextStr());
                                }
                                showFieldIds.add(controlKeyPreMap.get(fieldKey));
                            });
                            controlRule.setFieldIds(showFieldIds);
                        });
                        initField.put(CONTROL_RULES_KEY, controlRules);
                    }
                    ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
                    fieldBlob.setId(field.getId());
                    fieldBlob.setProp(JSON.toJSONString(initField));
                    fieldBlobs.add(fieldBlob);
                });
            });
            moduleFieldMapper.batchInsert(fields);
            moduleFieldBlobMapper.batchInsert(fieldBlobs);
        } catch (Exception e) {
            throw new GenericException("表单字段初始化失败", e);
        }
    }

    /**
     * 初始化数据类型-数据源映射
     *
     * @return 集合
     */
    public Map<String, String> initTypeSourceMap() {
        Map<String, String> typeSourceMap = new HashMap<>(8);
        typeSourceMap.put(FieldType.MEMBER.name(), "sys_user");
        typeSourceMap.put(FieldType.DEPARTMENT.name(), "sys_department");
        typeSourceMap.put(FieldSourceType.CUSTOMER.name(), "customer");
        typeSourceMap.put(FieldSourceType.CLUE.name(), "clue");
        typeSourceMap.put(FieldSourceType.CONTACT.name(), "customer_contact");
        typeSourceMap.put(FieldSourceType.OPPORTUNITY.name(), "opportunity");
        typeSourceMap.put(FieldSourceType.PRODUCT.name(), "product");
        return typeSourceMap;
    }

    /**
     * 处理默认值初始化选项
     *
     * @param field 基础字段
     */
    private void handleInitialOption(BaseField field) {
        if (field instanceof MemberField memberField) {
            memberField.setInitialOptions(userExtendService.getUserOptionById(memberField.getDefaultValue()));
        }
        if (field instanceof MemberMultipleField memberMultipleField) {
            memberMultipleField.setInitialOptions(userExtendService.getUserOptionByIds(memberMultipleField.getDefaultValue()));
        }
        if (field instanceof DepartmentField departmentField) {
            departmentField.setInitialOptions(departmentService.getDepartmentOptionsById(departmentField.getDefaultValue()));
        }
        if (field instanceof DepartmentMultipleField departmentMultipleField) {
            departmentMultipleField.setInitialOptions(departmentService.getDepartmentOptionsByIds(departmentMultipleField.getDefaultValue()));
        }
    }

    /**
     * 将业务字段选项放入optionMap
     *
     * @param list              列表数据
     * @param getOptionIdFunc   获取选项ID函数
     * @param getOptionNameFunc 获取选项名称函数
     * @param <T>               实体
     */
    public <T> List<OptionDTO> getBusinessFieldOption(List<T> list,
                                                      Function<T, String> getOptionIdFunc,
                                                      Function<T, String> getOptionNameFunc) {
        return list.stream()
                .map(item -> {
                    OptionDTO optionDTO = new OptionDTO();
                    optionDTO.setId(getOptionIdFunc.apply(item));
                    optionDTO.setName(getOptionNameFunc.apply(item));
                    return optionDTO;
                })
                .distinct()
                .toList();
    }

    public <T> List<OptionDTO> getBusinessFieldOption(T item,
                                                      Function<T, String> getOptionIdFunc,
                                                      Function<T, String> getOptionNameFunc) {
        return getBusinessFieldOption(List.of(item), getOptionIdFunc, getOptionNameFunc);
    }

    public <T> List<BaseModuleFieldValue> getBaseModuleFieldValues(List<T> list, Function<T, List<BaseModuleFieldValue>> getModuleFieldFunc) {
        // 处理自定义字段选项数据
        return list.stream()
                .map(getModuleFieldFunc)
                .filter(org.apache.commons.collections.CollectionUtils::isNotEmpty)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 获取
     *
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     *
     * @return 自定义导入表头集合
     */
    public List<BaseField> getCustomImportHeads(String formKey, String currentOrg) {
        List<BaseField> allFields = getAllFields(formKey, currentOrg);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }
        return allFields.stream().filter(BaseField::canImport).toList();
    }

    /**
     * 字段保存预检查
     *
     * @param saveParam 保存参数
     */
    private void preCheckForFieldSave(ModuleFormSaveRequest saveParam) {
        boolean businessDeleted = BusinessModuleField.isBusinessDeleted(saveParam.getFormKey(), saveParam.getFields());
        if (businessDeleted) {
            throw new GenericException(Translator.get("module.form.business_field.deleted"));
        }
        boolean hasRepeatName = BusinessModuleField.hasRepeatName(saveParam.getFields());
        if (hasRepeatName) {
            throw new GenericException(Translator.get("module.form.fields.repeat"));
        }
        Optional<BaseField> repeatOptional = saveParam.getFields().stream().filter(field -> {
            if (field instanceof HasOption optionField) {
                List<OptionProp> options = optionField.getOptions();
                return CollectionUtils.isNotEmpty(options) && hasRepeatOption(options);
            }
            return false;
        }).findAny();
        if (repeatOptional.isPresent()) {
            BaseField field = repeatOptional.get();
            throw new GenericException(Translator.getWithArgs("module.form.fields.option.repeat", field.getName()));
        }
    }

    /**
     * 包含重复选项
     *
     * @param options 选项
     *
     * @return 是否重复选项
     */
    private boolean hasRepeatOption(List<OptionProp> options) {
        if (CollectionUtils.isEmpty(options)) {
            return false;
        }
        return options.stream()
                .collect(Collectors.groupingBy(OptionProp::getLabel, Collectors.counting()))
                .values().stream()
                .anyMatch(count -> count > 1);
    }

    /**
     * 判断内置字段是否包含唯一性校验
     *
     * @param formKey 表单Key
     * @param orgId   组织ID
     *
     * @return 是否唯一
     */
    public boolean hasFieldUniqueCheck(String formKey, String orgId, String internalKey) {
        List<BaseField> allFields = getAllFields(formKey, orgId);
        if (CollectionUtils.isEmpty(allFields)) {
            return false;
        }
        Optional<BaseField> internalField = allFields.stream().filter(field -> Strings.CS.equals(field.getInternalKey(), internalKey)).findFirst();
        return internalField.isPresent() && internalField.get().needRepeatCheck();
    }

    /**
     * 填充表单联动值
     *
     * @param target           目标对象
     * @param source           源对象
     * @param targetFormConfig 目标表单配置
     * @param orgId            组织ID
     * @param <T>              实体类型
     * @param <S>              数据来源类型
     *
     * @return 填充结果
     */
    public <T, S> FormLinkFill<T> fillFormLinkValue(T target, S source, ModuleFormConfigDTO targetFormConfig, String orgId, String sourceFormKey) throws Exception {
        FormLinkFill.FormLinkFillBuilder<T> fillBuilder = FormLinkFill.builder();
        Map<String, List<LinkField>> linkProp = targetFormConfig.getFormProp().getLinkProp();
        if (linkProp == null || CollectionUtils.isEmpty(linkProp.get(sourceFormKey))) {
            return fillBuilder.entity(target).build();
        }

        ModuleFormConfigDTO sourceFormConfig = getBusinessFormConfig(sourceFormKey, orgId);
        List<BaseField> sourceFields = sourceFormConfig.getFields();
        List<BaseField> targetFields = targetFormConfig.getFields();
        if (CollectionUtils.isEmpty(sourceFields) || CollectionUtils.isEmpty(targetFields)) {
            return fillBuilder.entity(target).build();
        }
        // 目标表单字段
        Map<String, BaseField> targetFieldMap = targetFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity()));
        // 来源表单字段
        Map<String, BaseField> sourceFieldMap = sourceFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity()));
        // 填充数据
        Class<?> targerClass = target.getClass();
        Class<?> sourceClass = source.getClass();
        List<BaseModuleFieldValue> targetFieldVals = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<BaseModuleFieldValue> sourceFieldVals = (List<BaseModuleFieldValue>) sourceClass.getMethod("getModuleFields").invoke(source);
        for (LinkField linkField : linkProp.get(sourceFormKey)) {
            BaseField targetField = targetFieldMap.get(linkField.getCurrent());
            BaseField sourceField = sourceFieldMap.get(linkField.getLink());
            if (targetField == null || sourceField == null) {
                continue;
            }
            // 从源对象字段取值
            TransformSourceApplyDTO sourceValue;
            try {
                sourceValue = applySourceValue(sourceField, sourceClass, source, sourceFieldVals);
            } catch (Exception e) {
                sourceValue = null;
                LogUtils.error("Apply source value error: {}", e.getMessage());
            }

            // 源对象中无值, 跳过取值
            if (sourceValue == null || sourceValue.getActualVal() == null) {
                continue;
            }
            // 放入目标对象字段
            putTargetFieldVal(targetField, sourceValue, targerClass, target, targetFieldVals);
        }
        return fillBuilder.entity(target).fields(targetFieldVals).build();
    }

    /**
     * 属性转方法 (name -> setName)
     *
     * @param param 属性名
     *
     * @return 方法名
     */
    private String capitalizeSetParam(String param) {
        return "set" + param.substring(0, 1).toUpperCase() + param.substring(1);
    }

    /**
     * 属性转方法 (name -> getName)
     *
     * @param param 属性名
     *
     * @return 方法名
     */
    private String capitalizeGetParam(String param) {
        return "get" + param.substring(0, 1).toUpperCase() + param.substring(1);
    }

    /**
     * 选项值转文本
     *
     * @param options 选项集合
     * @param value   值
     *
     * @return 文本
     */
    private Object val2Text(List<OptionProp> options, Object value) {
        if (CollectionUtils.isEmpty(options) || value == null) {
            return null;
        }
        Map<String, String> optionMap = options.stream().collect(Collectors.toMap(OptionProp::getValue, OptionProp::getLabel));
        if (value instanceof List) {
            return ((List<?>) value).stream().map(v -> optionMap.get(v.toString())).toList();
        } else {
            return optionMap.get(value.toString());
        }
    }

    /**
     * 选项文本转值
     *
     * @param options 选项集合
     * @param text    文本
     *
     * @return 值
     */
    private Object text2Val(List<OptionProp> options, Object text) {
        if (CollectionUtils.isEmpty(options) || text == null) {
            return null;
        }
        Map<String, String> optionMap = options.stream().collect(Collectors.toMap(OptionProp::getLabel, OptionProp::getValue));
        if (text instanceof List) {
            return ((List<?>) text).stream().map(v -> optionMap.get(v.toString())).filter(Objects::nonNull).toList();
        } else {
            return optionMap.get(text.toString());
        }
    }

    /**
     * 从源对象取值
     *
     * @param sourceField     来源字段
     * @param sourceClass     类对象
     * @param source          数据来源
     * @param sourceFieldVals 自定义数据来源
     *
     * @return 值
     *
     * @throws Exception 取值异常
     */
    private TransformSourceApplyDTO applySourceValue(BaseField sourceField, Class<?> sourceClass, Object source, List<BaseModuleFieldValue> sourceFieldVals) throws Exception {
        // 来源字段取值
        TransformSourceApplyDTO sourceApply = new TransformSourceApplyDTO();
        Object tmpVal;
        if (StringUtils.isNotEmpty(sourceField.getBusinessKey())) {
            // 业务字段取值
            tmpVal = sourceClass.getMethod(capitalizeGetParam(sourceField.getBusinessKey())).invoke(source);
        } else {
            // 自定义字段取值
            Optional<BaseModuleFieldValue> find = sourceFieldVals.stream().filter(fieldVal -> Strings.CS.equals(sourceField.getId(), fieldVal.getFieldId())).findFirst();
            tmpVal = find.map(BaseModuleFieldValue::getFieldValue).orElse(null);
        }
        sourceApply.setActualVal(tmpVal);
        // 取展示值
        if (tmpVal == null) {
            sourceApply.setDisplayVal(null);
        } else {
            sourceApply.setDisplayVal(displayOfType(sourceField, sourceApply.getActualVal()));
        }
        return sourceApply;
    }

    /**
     * 放入目标对象字段值
     *
     * @param targetField     字段
     * @param putVal          放入值
     * @param targetClass     目标类
     * @param target          目标实例
     * @param targetFieldVals 目标自定义字段值集合
     *
     * @throws Exception 入值异常
     */
    private void putTargetFieldVal(BaseField targetField, TransformSourceApplyDTO putVal, Class<?> targetClass, Object target, List<BaseModuleFieldValue> targetFieldVals) throws Exception {
        Object val = resolveTargetPutVal(targetField, putVal);
        if (val == null) {
            return;
        }
        if (StringUtils.isNotEmpty(targetField.getBusinessKey())) {
            // 目标字段是业务字段
            Method method = targetClass.getMethod(capitalizeSetParam(targetField.getBusinessKey()), targetClass.getDeclaredField(targetField.getBusinessKey()).getType());
            method.invoke(target, val);
        } else {
            // 目标字段是自定义字段
            BaseModuleFieldValue targetFieldVal = new BaseModuleFieldValue();
            targetFieldVal.setFieldId(targetField.getId());
            targetFieldVal.setFieldValue(val);
            targetFieldVals.add(targetFieldVal);
        }
    }

    /**
     * 根据类型获取展示值
     *
     * @param sourceField 来源字段
     * @param actualVal   实际值
     *
     * @return 展示值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object displayOfType(BaseField sourceField, Object actualVal) {
        if (actualVal == null) {
            return null;
        }
        if (sourceField instanceof HasOption fieldWithOption) {
            return val2Text(fieldWithOption.getOptions(), actualVal);
        }
        AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(sourceField.getType());
        // 将数据库中的字符串值,转换为对应的对象值
        return customFieldResolver.trans2Value(sourceField, actualVal instanceof List ? JSON.toJSONString(actualVal) : actualVal.toString());
    }

    /**
     * 解析目标字段值
     *
     * @param targetField 目标字段
     * @param sourceVal   来源值
     *
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public Object resolveTargetPutVal(BaseField targetField, TransformSourceApplyDTO sourceVal) {
        if (targetField.multiple() && sourceVal.getActualVal() instanceof String) {
            // 兼容处理: 单值映射多值的情况
            sourceVal.setActualVal(List.of(sourceVal.getActualVal()));
            sourceVal.setDisplayVal(List.of(sourceVal.getDisplayVal()));
        }
        if (targetField instanceof InputField || targetField instanceof TextAreaField) {
            // 兼容处理: [文本, 多行文本] 按照展示值处理即可.
            Object displayVal = sourceVal.getDisplayVal();
            if (displayVal == null) {
                return null;
            }
            return displayVal instanceof List ? String.join(",", (List<String>) displayVal) : displayVal.toString();
        }
        if (targetField instanceof InputMultipleField) {
            // 兼容处理: 多值输入直接取展示值即可.
            TextMultipleResolver textMultipleResolver = new TextMultipleResolver();
            return new ArrayList<>(textMultipleResolver.getCorrectFormatInput(sourceVal.getDisplayVal() instanceof List ?
                    (List<String>) sourceVal.getDisplayVal() : List.of(sourceVal.getDisplayVal().toString().split(","))));
        }
        if (targetField instanceof HasOption targetFieldWithOption) {
            // 兼容处理: 选项文本映射
            return text2Val(targetFieldWithOption.getOptions(), sourceVal.getDisplayVal());
        }
        return sourceVal.getActualVal();
    }

    /**
     * 表单联动处理(旧数据)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormLinkProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            if (linkProp == null) {
                continue;
            }
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            if (linkPropMap.containsKey("formKey") && linkPropMap.containsKey("linkFields")) {
                Map<String, List<LinkField>> dataMap = new HashMap<>(2);
                String formKey = linkPropMap.get("formKey").toString();
                List<LinkField> linkFields = (List<LinkField>) linkPropMap.get("linkFields");
                dataMap.put(formKey, linkFields);
                propMap.put("linkProp", dataMap);
                formBlob.setProp(JSON.toJSONString(propMap));
                moduleFormBlobMapper.updateById(formBlob);
            }
        }
    }

    /**
     * 表单属性处理(视图)
     */
    @SuppressWarnings("unchecked")
    public void modifyFormProp() {
        List<ModuleForm> moduleForms = moduleFormMapper.selectAll(null);
        List<String> formIds = moduleForms.stream().map(ModuleForm::getId).toList();
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formIds);
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            propMap.put("viewSize", "large");
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    @SuppressWarnings("unchecked")
    public void modifyFieldMobile() {
        List<ModuleField> moduleFields = moduleFieldMapper.selectAll(null);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectByIds(fieldIds);
        for (ModuleFieldBlob fieldBlob : moduleFieldBlobs) {
            Map<String, Object> propMap = JSON.parseMap(fieldBlob.getProp());
            propMap.put("mobile", true);
            fieldBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(fieldBlob);
        }
        extModuleFieldMapper.batchUpdateMobile(fieldIds, true);
    }

    /**
     * 获取MCP表单需要的字段
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
     *
     * @return 字段列表
     */
    public List<SimpleField> getMcpFields(String formKey, String organizationId) {
        ModuleFormConfigDTO businessFormConfig = getBusinessFormConfig(formKey, organizationId);
        return businessFormConfig.getFields().stream().filter(BaseField::canImport).map(field -> {
            SimpleField simpleField = new SimpleField();
            simpleField.setId(field.getId());
            simpleField.setBusinessKey(field.getBusinessKey());
            simpleField.setName(field.getName());
            simpleField.setType(field.getType());
            simpleField.setRequired(field.needRequireCheck());
            if (field instanceof HasOption fieldWithOption) {
                simpleField.setOptions(fieldWithOption.getOptions());
            }
            if (field instanceof DatasourceField datasourceField) {
                simpleField.setDataSourceType(datasourceField.getDataSourceType());
            } else if (field instanceof DatasourceMultipleField datasourceMultipleField) {
                simpleField.setDataSourceType(datasourceMultipleField.getDataSourceType());
            }
            return simpleField;
        }).toList();
    }
}
