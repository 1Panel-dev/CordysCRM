package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.LinkScenarioKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.resolver.field.TextMultipleResolver;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.FieldSourceServiceProvider;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.SerialNumGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.constants.BusinessTitleConstants;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.*;
import cn.cordys.crm.system.dto.TransformSourceApplyDTO;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.*;
import cn.cordys.crm.system.dto.form.FormLinkFill;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.form.base.LinkField;
import cn.cordys.crm.system.dto.form.base.LinkScenario;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.FormPropLogDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigLogDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class ModuleFormService {

    public static final Map<String, String> TYPE_SOURCE_MAP;
    private static final String DEFAULT_ORGANIZATION_ID = "100001";
    private static final String CONTROL_RULES_KEY = "showControlRules";
    private static final String SHOW_FIELD_KEY = "showFields";
    private static final String SUB_FIELDS = "subFields";
	public static final String SUM_PREFIX = "sum_";
	public static final String EXPORT_SYSTEM_TYPE = "system";
	private static final String PRICE_SUB_ROW_KEY = "price_sub";
	private static final String OPTION_DEFAULT_SOURCE = "custom";
	private static final String UPGRADE_EXT_FIELD = "ext_ver";

    static {
        TYPE_SOURCE_MAP = Map.ofEntries(
				Map.entry(FieldType.MEMBER.name(), "sys_user"),
				Map.entry(FieldType.DEPARTMENT.name(), "sys_department"),
				Map.entry(FieldSourceType.CUSTOMER.name(), "customer"),
				Map.entry(FieldSourceType.CLUE.name(), "clue"),
				Map.entry(FieldSourceType.CONTACT.name(), "customer_contact"),
				Map.entry(FieldSourceType.OPPORTUNITY.name(), "opportunity"),
				Map.entry(FieldSourceType.PRODUCT.name(), "product"),
				Map.entry(FieldSourceType.QUOTATION.name(), "opportunity_quotation"),
				Map.entry(FieldSourceType.PRICE.name(), "product_price"),
				Map.entry(FieldSourceType.CONTRACT.name(), "contract"),
				Map.entry(FieldSourceType.PAYMENT_PLAN.name(), "contract_payment_plan"),
				Map.entry(FieldSourceType.BUSINESS_TITLE.name(), "business_title"),
				Map.entry(FieldSourceType.CONTRACT_PAYMENT_RECORD.name(), "contract_payment_record")
		);
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
	@Resource
	private SerialNumGenerator serialNumGenerator;
	@Lazy
	@Resource
	private FieldSourceServiceProvider fieldSourceServiceProvider;

    /**
     * 获取模块表单配置
     *
     * @param formKey      表单Key
     * @param currentOrgId 当前组织ID
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
     * @return 表单配置
     */
    public ModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
        ModuleFormConfigDTO config = getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());
        // 设置业务字段参数
        List<BaseField> flattenFields = flattenSourceRefFields(config.getFields());
        businessModuleFormConfig.setFields(flattenFields.stream()
				.peek(this::setFieldRefOption)
                .peek(this::setFieldBusinessParam)
                .peek(this::reloadPropOfSubRefFields)
                .collect(Collectors.toList())
        );
        return businessModuleFormConfig;
    }

    public ModuleFormConfigDTO getSourceDisplayFields(String formKey, String organizationId) {
        ModuleFormConfigDTO config = getConfig(formKey, organizationId);
        ModuleFormConfigDTO businessModuleFormConfig = new ModuleFormConfigDTO();
        businessModuleFormConfig.setFormProp(config.getFormProp());

        List<BaseField> flattenFields = flattenFormAllFieldsWithSubId(config);
        // 设置业务字段参数
        businessModuleFormConfig.setFields(flattenFields.stream()
                .filter(BaseField::canDisplay)
                .filter(f -> StringUtils.isEmpty(f.getResourceFieldId()))
                .peek(this::setFieldBusinessParam)
                .collect(Collectors.toList())
        );

        if (Strings.CI.equals(formKey, FormKey.CONTRACT.getKey())) {
            businessModuleFormConfig.getFields().add(getContractAmountField());
        }
        return businessModuleFormConfig;
    }

    /**
     * 保存表单配置
     *
     * @param saveParam 保存参数
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
                        .originalValue(buildModuleFormLogDTO(oldConfig))
                        .modifiedValue(buildModuleFormLogDTO(newConfig))
                        .build()
        );

        // 处理字段 (删除&&新增)
        LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
        fieldWrapper.eq(ModuleField::getFormId, form.getId());
        List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
		// 重置流水号
		resetSerial(fields, saveParam.getFields(), saveParam.getFormKey(), currentOrgId);
        if (CollectionUtils.isNotEmpty(fields)) {
            List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
            extModuleFieldMapper.deleteByIds(fieldIds);
            extModuleFieldMapper.deletePropByIds(fieldIds);
        }
		if (CollectionUtils.isNotEmpty(saveParam.getFields())) {
			saveFields(saveParam.getFields(), form.getId(), currentUserId);
        }

        // 返回表单配置
        return getConfig(form.getFormKey(), currentOrgId);
    }

	/**
	 * 重置流水号
	 * @param oldFields 旧的字段集合
	 * @param saveFields 保存的字段集合
	 */
	private void resetSerial(List<ModuleField> oldFields, List<BaseField> saveFields, String formKey, String orgId) {
		Optional<ModuleField> serialOld = oldFields.stream().filter(f -> Strings.CS.equals(f.getType(), FieldType.SERIAL_NUMBER.name())).findAny();
		Optional<BaseField> serialNew = saveFields.stream().filter(BaseField::isSerialNumber).findAny();
		if (serialOld.isEmpty()) {
			return;
		}
		ModuleFieldBlob oldFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(serialOld.get().getId());
		List<String> oldRules = JSON.parseObject(oldFieldBlob.getProp(), SerialNumberField.class).getSerialNumberRules();
		if (serialNew.isEmpty()) {
			serialNumGenerator.resetKey(oldRules.get(2), formKey, orgId);
			return;
		}
		List<String> newRules = ((SerialNumberField) serialNew.get()).getSerialNumberRules();
		if (serialNumGenerator.sameRule(oldRules, newRules)) {
			return;
		}
		serialNumGenerator.resetKey(oldRules.get(2), formKey, orgId);
	}

	/**
	 * 保存所有字段集合
	 * @param saveFields 字段集合
	 * @param saveFormId 表单ID
	 * @param currentUserId 当前用户ID
	 */
	private void saveFields(List<BaseField> saveFields, String saveFormId, String currentUserId) {
		// 剔除引用字段&&并保留到数据源引用字段
		List<String> showFields = saveFields.stream().filter(f -> f instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields()))
				.flatMap(sf -> ((DatasourceField) sf).getShowFields().stream()).distinct().toList();
		List<BaseField> refFields = saveFields.stream().filter(f -> showFields.contains(f.getId()))
				.collect(Collectors.toMap(BaseField::getId, Function.identity(), (a, b) -> a)).values().stream()
				.toList();
		saveFields.removeAll(refFields);

		List<ModuleField> addFields = new ArrayList<>();
		List<ModuleFieldBlob> addFieldBlobs = new ArrayList<>();
		AtomicLong pos = new AtomicLong(1);
		saveFields.forEach(field -> {
			ModuleField moduleField = new ModuleField();
			moduleField.setId(field.getId());
			moduleField.setFormId(saveFormId);
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
			if (field instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
				List<BaseField> sourceRefFields = refFields.stream().filter(f -> sourceField.getShowFields().contains(f.getId())).toList();
				sourceField.setRefFields(sourceRefFields);
			}
			if (field instanceof HasOption of && Strings.CS.equals(of.getOptionSource(), OPTION_DEFAULT_SOURCE)) {
				of.setOptions(of.getCustomOptions());
			}
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

    public ModuleFormConfigLogDTO buildModuleFormLogDTO(ModuleFormConfigDTO config) {
        ModuleFormConfigLogDTO logDTO = new ModuleFormConfigLogDTO();
        FormPropLogDTO formPropLog = BeanUtils.copyBean(new FormPropLogDTO(), config.getFormProp());
        BeanUtils.copyBean(logDTO, config);
        logDTO.setFormProp(formPropLog);
        // 目前只处理联动字段方便日志详情解析
        Map<String, List<LinkField>> parseLinkFieldMap = new HashMap<>(8);
        Map<String, List<LinkScenario>> linkProp = config.getFormProp().getLinkProp();
        if (linkProp != null && !linkProp.isEmpty()) {
            linkProp.forEach((key, value) -> value.forEach(scenario -> parseLinkFieldMap.put(key + "-" + scenario.getKey(), scenario.getLinkFields())));
        }
        logDTO.getFormProp().setLinkProp(parseLinkFieldMap);
        return logDTO;
    }

    public List<BaseField> getAllFields(String formKey, String orgId) {
        ModuleForm example = new ModuleForm();
        example.setFormKey(formKey);
        example.setOrganizationId(orgId);
        ModuleForm moduleForm = moduleFormMapper.selectOne(example);
        List<BaseField> allFields = getAllFields(moduleForm.getId());
        List<BaseField> flattenFields = flattenSourceRefFields(allFields);
        return flattenFields.stream()
				.peek(this::setFieldRefOption)
                .peek(this::setFieldBusinessParam)
                .peek(this::reloadPropOfSubRefFields)
                .collect(Collectors.toList());
    }

    /**
     * 获取表单所有字段及其属性集合
     *
     * @param formId 表单ID
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
     * @return 字段选项集合
     */
    public Map<String, List<OptionDTO>> getOptionMap(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allDataFields) {
        Map<String, List<OptionDTO>> optionMap = new HashMap<>(4);
        Map<String, String> idTypeMap = new HashMap<>(8);

        // 平铺表单所有字段, 过滤出满足条件的字段获取选项
        List<BaseField> allFields = flattenFormAllFields(formConfig);
        List<String> showFields = allFields.stream().filter(f -> f instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields()))
                .flatMap(f -> ((DatasourceField) f).getShowFields().stream()).distinct().toList();
        allFields.forEach(field -> {
            if (Strings.CS.equalsAny(field.getType(), FieldType.RADIO.name()) && field instanceof RadioField radioField) {
                optionMap.put(getOptionKey(showFields, field), optionPropToDto(radioField.getOptions()));
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.CHECKBOX.name()) && field instanceof CheckBoxField checkBoxField) {
                optionMap.put(getOptionKey(showFields, field), optionPropToDto(checkBoxField.getOptions()));
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name())) {
                if (field instanceof HasOption optionField) {
                    optionMap.put(getOptionKey(showFields, field), optionPropToDto(optionField.getOptions()));
                }
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.DATA_SOURCE.name(), FieldType.DATA_SOURCE_MULTIPLE.name())) {
                if (field instanceof DatasourceField sourceField) {
                    idTypeMap.put(getOptionKey(showFields, field), sourceField.getDataSourceType());
                }
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.MEMBER.name(), FieldType.MEMBER_MULTIPLE.name())) {
                idTypeMap.put(getOptionKey(showFields, field), FieldType.MEMBER.name());
            }
            if (Strings.CS.equalsAny(field.getType(), FieldType.DEPARTMENT.name(), FieldType.DEPARTMENT_MULTIPLE.name())) {
                idTypeMap.put(getOptionKey(showFields, field), FieldType.DEPARTMENT.name());
            }
        });
		if (CollectionUtils.isEmpty(allDataFields)) {
			return optionMap;
		}
        // 平铺子表格字段值
        List<BaseModuleFieldValue> allFieldValues = flattenSubFieldValues(formConfig, allDataFields);

        // 分组获取选项 typeIdsMap: {key: 数据类型, value: id集合}
        Map<String, List<String>> typeIdsMap = new HashMap<>(8);
        if (CollectionUtils.isNotEmpty(allFieldValues)) {
            allFieldValues.stream().filter(fv -> idTypeMap.containsKey(fv.getFieldId())).forEach(fv -> {
                typeIdsMap.putIfAbsent(fv.getFieldId(), new ArrayList<>());
                Object v = fv.getFieldValue();
				if (v == null) {
					return;
				}
                if (v instanceof List) {
                    typeIdsMap.get(fv.getFieldId()).addAll(JSON.parseArray(JSON.toJSONString(v), String.class));
                } else {
                    typeIdsMap.get(fv.getFieldId()).add(v.toString());
                }
            });
        }
        // 批量查询选项映射
        typeIdsMap.forEach((fieldId, ids) -> {
            if (CollectionUtils.isNotEmpty(ids) && TYPE_SOURCE_MAP.containsKey(idTypeMap.get(fieldId))) {
                List<OptionDTO> options = extModuleFieldMapper.getSourceOptionsByIds(TYPE_SOURCE_MAP.get(idTypeMap.get(fieldId)), ids);
                if (CollectionUtils.isNotEmpty(options)) {
                    optionMap.put(fieldId, options);
                }
            }
        });
        return optionMap;
    }

    /**
     * 平铺列表字段(子表格)
     *
     * @param formConfig 表单配置
     * @return 字段集合
     */
    public List<BaseField> flattenFormAllFields(ModuleFormConfigDTO formConfig) {
        List<BaseField> toFlattenFields = new ArrayList<>();
		formConfig.getFields().stream().filter(f -> f instanceof SubField).map(f -> ((SubField) f).getSubFields()).forEach(toFlattenFields::addAll);
        formConfig.getFields().addAll(toFlattenFields);
        return formConfig.getFields();
    }

	public List<BaseField> flattenFormAllFieldsWithSubId(ModuleFormConfigDTO formConfig) {
		List<BaseField> toFlattenFields = new ArrayList<>();
		formConfig.getFields().stream().filter(f -> f instanceof SubField).forEach(sf -> {
			SubField subField = ((SubField) sf);
			if (CollectionUtils.isEmpty(subField.getSubFields())) {
				return;
			}
			subField.getSubFields().forEach(field -> {
				if (StringUtils.isEmpty(field.getSubTableFieldId())) {
					field.setSubTableFieldId(subField.getId());
				}
			});
			toFlattenFields.addAll(subField.getSubFields());
		});
		formConfig.getFields().addAll(toFlattenFields);
		return formConfig.getFields();
	}

    /**
     * 获得所有平铺的字段
     *
     * @param formKey 表单key
     * @param orgId   组织ID
     * @return 平铺的字段集合
     */
    public List<BaseField> getFlattenFormFields(String formKey, String orgId) {
        ModuleFormConfigDTO formConfig = getBusinessFormConfig(formKey, orgId);
        return flattenFormAllFields(formConfig);
    }

    /**
     * 平铺子表字段值
     *
     * @param formConfig     表单配置
     * @param allFieldValues 所有字段值
     * @return 平铺字段值集合
     */
    @SuppressWarnings("unchecked")
    public List<BaseModuleFieldValue> flattenSubFieldValues(ModuleFormConfigDTO formConfig, List<BaseModuleFieldValue> allFieldValues) {
        // 类型为子表的字段
        List<BaseField> subFields = formConfig.getFields().stream().filter(f -> f instanceof SubField).toList();
        Set<String> subIdSet = subFields.stream().map(BaseField::getId).collect(Collectors.toSet());
        // 过滤出子表的字段值进行平铺
        List<BaseModuleFieldValue> allFlattenFieldValues = new ArrayList<>();
        allFieldValues.stream().filter(fv -> subIdSet.contains(fv.getFieldId())).forEach(fv -> {
            List<Map<String, Object>> subRowList = (List<Map<String, Object>>) fv.getFieldValue();
            if (CollectionUtils.isEmpty(subRowList)) {
                return;
            }
            Map<String, List<String>> subFieldIdValueMap = new HashMap<>(subRowList.getFirst().size());
            subRowList.forEach(subRow -> subRow.forEach((k, v) -> {
                if (v == null) {
                    return;
                }
                subFieldIdValueMap.putIfAbsent(k, new ArrayList<>());
                if (v instanceof List) {
                    subFieldIdValueMap.get(k).addAll(JSON.parseArray(JSON.toJSONString(v), String.class));
                } else {
                    subFieldIdValueMap.get(k).add(v.toString());
                }
            }));
            subFieldIdValueMap.forEach((subFieldId, values) -> {
                if (CollectionUtils.isNotEmpty(values)) {
                    BaseModuleFieldValue subFieldValue = new BaseModuleFieldValue();
                    subFieldValue.setFieldId(subFieldId);
                    subFieldValue.setFieldValue(values);
                    allFlattenFieldValues.add(subFieldValue);
                }
            });
        });
        // 插入所有字段值集合中
        allFlattenFieldValues.addAll(allFieldValues);
        return allFlattenFieldValues;
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
        List<String> createUserIds = attachments.stream().map(Attachment::getCreateUser).toList();
        List<String> updateUserIds = attachments.stream().map(Attachment::getUpdateUser).toList();
        List<User> users = userExtendService.getUserOptionByIds(ListUtils.union(createUserIds, updateUserIds));
        Map<String, String> userMap = users.stream().collect(Collectors.toMap(User::getId, User::getName));
        attachments.forEach(attachment -> {
            attachment.setCreateUser(userMap.get(attachment.getCreateUser()));
            attachment.setUpdateUser(userMap.get(attachment.getUpdateUser()));
        });
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

    public List<OptionDTO> getSourceOptionsByKeywords(String type, List<String> nameList) {
        if (CollectionUtils.isEmpty(nameList)) {
            return new ArrayList<>();
        }
        if (!TYPE_SOURCE_MAP.containsKey(type)) {
            log.error("未知的数据源类型：{}", type);
            return new ArrayList<>();
        }
        return extModuleFieldMapper.getSourceOptionsByKeywords(TYPE_SOURCE_MAP.get(type), nameList);
    }

    /**
     * 处理自定义字段中业务子表单值
     *
     * @param resource    资源详情
     * @param fieldValues 自定义字段值
     * @param formConfig  表单配置
     */
    public void processBusinessFieldValues(Object resource, List<BaseModuleFieldValue> fieldValues, ModuleFormConfigDTO formConfig) {
        List<BaseField> subFields = formConfig.getFields().stream().filter(f -> f instanceof SubField && StringUtils.isNotEmpty(f.getBusinessKey())).toList();
        Map<String, String> subFieldBusinessMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, BaseField::getBusinessKey));
        List<BaseModuleFieldValue> businessFieldValues = fieldValues.stream().filter(fv -> subFieldBusinessMap.containsKey(fv.getFieldId())).toList();
        businessFieldValues.forEach(bfv -> {
            String businessKey = subFieldBusinessMap.get(bfv.getFieldId());
            Field field = ReflectionUtils.findField(resource.getClass(), f -> Strings.CS.equals(f.getName(), businessKey));
            if (field == null) {
                log.error("Cannot find field `{}`", businessKey);
                return;
            }
            ReflectionUtils.setField(field, resource, bfv.getFieldValue());
        });
        fieldValues = new ArrayList<>(fieldValues);
        fieldValues.removeIf(fv -> subFieldBusinessMap.containsKey(fv.getFieldId()));
        Field moduleFields = ReflectionUtils.findField(resource.getClass(), f -> Strings.CS.equals(f.getName(), "moduleFields"));
        if (moduleFields == null) {
            log.error("No such field `moduleFields` in resource");
            return;
        }
        ReflectionUtils.setField(moduleFields, resource, fieldValues);
    }

	/**
	 * 替换字段引用选项
	 * @param field 自定义字段
	 */
	public void setFieldRefOption(BaseField field) {
		if (!(field instanceof HasOption of)) {
			return;
		}
		if (StringUtils.isEmpty(of.getOptionSource()) || Strings.CS.equals(of.getOptionSource(), OPTION_DEFAULT_SOURCE)) {
			if (CollectionUtils.isEmpty(of.getCustomOptions())) {
				of.setCustomOptions(of.getOptions());
			}
		} else {
			// 引用字段选项, 清空再替换
			of.setOptions(new ArrayList<>());
			String refId = of.getRefId();
			ModuleFieldBlob fieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(refId);
			if (fieldBlob != null) {
			    BaseField refField = JSON.parseObject(fieldBlob.getProp(), BaseField.class);
			    if (refField instanceof HasOption refOption && CollectionUtils.isNotEmpty(refOption.getCustomOptions())) {
					of.setOptions(refOption.getOptions());
			    }
			}
		}
	}

    /**
     * 设置自定义字段业务参数
     *
     * @param field 自定义字段
     */
    public void setFieldBusinessParam(BaseField field) {
        Set<String> businessTitleKeySet = Arrays.stream(BusinessTitleConstants.values())
                .map(BusinessTitleConstants::getKey)
                .collect(Collectors.toSet());
        if (businessTitleKeySet.contains(field.getId())) {
            return;
        }

        // 获取特殊的业务字段
        Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
                collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));
        if (field instanceof SubField subField) {
            subField.getSubFields().forEach(this::setFieldBusinessParam);
        }
        BusinessModuleField businessEnum = businessModuleFieldMap.get(field.getInternalKey());
        if (businessEnum != null) {
            // 设置特殊的业务字段 key
            field.setBusinessKey(businessEnum.getBusinessKey());
            field.setDisabledProps(businessEnum.getDisabledProps());
        } else {
			field.setBusinessKey(null);
			field.setDisabledProps(null);
		}
    }

    /**
     * 重新加载子表引用字段最新的属性
     *
     * @param field 自定义字段
     */
    public void reloadPropOfSubRefFields(BaseField field) {
        if (field instanceof SubField subField) {
            if (CollectionUtils.isEmpty(subField.getSubFields())) {
                return;
            }
            List<String> subRefIds = subField.getSubFields().stream()
                    .filter(f -> f instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields()))
                    .flatMap(f -> ((DatasourceField) f).getShowFields().stream()).distinct().toList();
            if (CollectionUtils.isEmpty(subRefIds)) {
                return;
            }
			// 子表格引用的字段来源 (表单字段&&价格表子字段)
            List<ModuleFieldBlob> reloadFieldBlobs = moduleFieldBlobMapper.selectByIds(subRefIds);
            Map<String, String> reloadFieldMap = reloadFieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId, ModuleFieldBlob::getProp));
			List<BaseField> subFields = getSubFieldsBySourceType(FieldSourceType.PRICE.name());
			Map<String, BaseField> subFieldMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity(), (p, n) -> p));
			ListIterator<BaseField> it = subField.getSubFields().listIterator();
            while (it.hasNext()) {
                BaseField oldField = it.next();
				if (StringUtils.isEmpty(oldField.getResourceFieldId())) {
					continue;
				}
				if (!reloadFieldMap.containsKey(oldField.getId()) && !subFieldMap.containsKey(oldField.getId())) {
					continue;
				}
				BaseField refField;
				if (reloadFieldMap.containsKey(oldField.getId())) {
					refField = JSON.parseObject(reloadFieldMap.get(oldField.getId()), BaseField.class);
				} else {
					refField = subFieldMap.get(oldField.getId());
				}
				// 属于引用字段 (保留数据源引用ID)
				refField.setResourceFieldId(oldField.getResourceFieldId());
				refField.setFieldWidth(oldField.getFieldWidth());
				refField.setBusinessKey(oldField.getBusinessKey());
				if (refField instanceof DatasourceField refSourceField) {
					// 清空多级引用的属性
					refSourceField.setRefFields(null);
					refSourceField.setShowFields(null);
				}
				refField.setName(oldField.getName());
				refField.setSubTableFieldId(oldField.getSubTableFieldId());
				it.set(refField);
            }
        }
    }

    public List<BaseField> flattenSourceRefFields(List<BaseField> fields) {
        List<BaseField> flatFields = new ArrayList<>();
        fields.forEach(field -> {
            flatFields.add(field);
            if (field instanceof DatasourceField sourceField && CollectionUtils.isNotEmpty(sourceField.getShowFields())) {
                List<String> oldRefIds = sourceField.getShowFields();
                List<String> newRefIds = new ArrayList<>();
                List<ModuleFieldBlob> reloadFieldBlobs = moduleFieldBlobMapper.selectByIds(oldRefIds);
                Map<String, BaseField> reloadFieldMap = reloadFieldBlobs.stream()
                        .collect(Collectors.toMap(ModuleFieldBlob::getId,
								filedBlob -> JSON.parseObject(filedBlob.getProp(), BaseField.class)));

                List<BaseField> subFields = getSubFieldsBySourceType(sourceField.getDataSourceType());
				Map<String, BaseField> subFieldMap = subFields.stream().collect(Collectors.toMap(BaseField::getId, Function.identity(), (p, n) -> p));

                // 补充扩展的系统字段
                getSystemExtendFiles(sourceField.getDataSourceType())
                        .forEach(extField -> reloadFieldMap.put(extField.getId(), extField));

                sourceField.setRefFields(new ArrayList<>());
                // 获取最新引用字段属性
                for (String showFieldKey : sourceField.getShowFields()) {
                    BaseField refField = reloadFieldMap.get(showFieldKey);
                    if (refField != null) {
                        refField.setResourceFieldId(field.getId());
                        sourceField.getRefFields().add(refField);
                    }
                }

                sourceField.getRefFields().forEach(oldRefField -> {
					if (!reloadFieldMap.containsKey(oldRefField.getId()) && !subFieldMap.containsKey(oldRefField.getId())) {
						return;
					}
					BaseField refField;
					if (reloadFieldMap.containsKey(oldRefField.getId())) {
						refField = reloadFieldMap.get(oldRefField.getId());
					} else {
						refField = subFieldMap.get(oldRefField.getId());
					}
					refField.setFieldWidth(oldRefField.getFieldWidth());
					if (refField instanceof DatasourceField refSourceField) {
						// 清空多级引用的属性
						refSourceField.setRefFields(null);
						refSourceField.setShowFields(null);
					}
					refField.setResourceFieldId(oldRefField.getResourceFieldId());
					refField.setName(oldRefField.getName());
					flatFields.add(flatFields.size(), refField);
					newRefIds.add(refField.getId());
                });
				sourceField.setShowFields(newRefIds);
            }
        });
        return flatFields;
    }

    private List<BaseField> getSystemExtendFiles(String dataSourceType) {
        if (Strings.CI.equals(dataSourceType, FieldSourceType.CONTRACT.name())) {
            return List.of(getContractAmountField());
        } else if (Strings.CI.equals(dataSourceType, FieldSourceType.BUSINESS_TITLE.name())) {
            return initBusinessTitleFields();
        }
        return List.of();
    }

    public InputNumberField getContractAmountField() {
        InputNumberField amountField = new InputNumberField();
        amountField.setId(BusinessModuleField.CONTRACT_PRODUCT_SUM_AMOUNT.getKey());
        amountField.setInternalKey(BusinessModuleField.CONTRACT_PRODUCT_SUM_AMOUNT.getKey());
        amountField.setName("合同金额");
        amountField.setBusinessKey(BusinessModuleField.CONTRACT_PRODUCT_SUM_AMOUNT.getBusinessKey());
        amountField.setReadable(false);
        amountField.setEditable(false);
        amountField.setType(FieldType.INPUT_NUMBER.name());
        amountField.setShowLabel(true);
        amountField.setFieldWidth(1F);
        return amountField;
    }

    public List<BaseField> initBusinessTitleFields() {
        List<BaseField> fields = new ArrayList<>();
        Locale locale = LocaleContextHolder.getLocale();
        BusinessTitleConstants[] values = BusinessTitleConstants.values();
        for (BusinessTitleConstants constant : values) {
            InputField field = new InputField();
            field.setId(constant.getKey());
            field.setBusinessKey(constant.getKey());
            if (Locale.US.toString().equalsIgnoreCase(locale.toString())) {
                field.setName(constant.getUs());
            } else {
                field.setName(constant.getCh());
            }
            field.setInternalKey(constant.getKey());
            field.setBusinessKey(constant.getKey());
            field.setType(FieldType.INPUT.name());
            field.setShowLabel(true);
            fields.add(field);
        }
        return fields;
    }

    /**
	 * 获取指定数据源的子表字段集合
	 * @param sourceType 数据源类型
	 * @return 子表字段集合
	 */
	private List<BaseField> getSubFieldsBySourceType(String sourceType) {
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		if (Strings.CS.equals(sourceType, FieldSourceType.QUOTATION.name())) {
			fieldWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.QUOTATION_PRODUCT_TABLE.getKey());
		} else if (Strings.CS.equals(sourceType, FieldSourceType.PRICE.name())) {
			fieldWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.PRICE_PRODUCT_TABLE.getKey());
		} else if (Strings.CS.equals(sourceType, FieldSourceType.CONTRACT.name())) {
			fieldWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.CONTRACT_PRODUCT_TABLE.getKey());
		} else {
			return new ArrayList<>();
		}
		List<ModuleField> subFields = moduleFieldMapper.selectListByLambda(fieldWrapper);
		if (CollectionUtils.isEmpty(subFields)) {
			return new ArrayList<>();
		}
		String subId = subFields.getFirst().getId();
		ModuleFieldBlob fieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(subId);
		SubField subField = JSON.parseObject(fieldBlob.getProp(), SubField.class);
		return subField.getSubFields();
	}

    /**
     * OptionProp转OptionDTO
     *
     * @param options 选项集合
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
        initFormAndFields(FormKey.allKeys());
    }

    /**
     * 初始化升级表单
     */
    public void initUpgradeForm() {
        List<String> allKeys = FormKey.allKeys();
        LambdaQueryWrapper<ModuleForm> moduleFormWrapper = new LambdaQueryWrapper<>();
        moduleFormWrapper.in(ModuleForm::getFormKey, allKeys);
        List<ModuleForm> oldForms = moduleFormMapper.selectListByLambda(moduleFormWrapper);
        allKeys.removeAll(oldForms.stream().map(ModuleForm::getFormKey).toList());
        if (CollectionUtils.isEmpty(allKeys)) {
            // 初始化完成, 无升级表单.
            return;
        }
        initFormAndFields(allKeys);
    }

	@SuppressWarnings("unchecked")
	public void initExtFieldsByVer(String version) {
		try {
			List<ModuleField> fields = new ArrayList<>();
			List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
			Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
			fieldMap.forEach((formKey, formFields) -> {
				boolean existExtVerField = formFields.stream().anyMatch(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString()));
				if (!existExtVerField) {
					return;
				}
				ModuleForm example = new ModuleForm();
				example.setFormKey(formKey);
				ModuleForm form = moduleFormMapper.selectOne(example);
				if (form == null) {
					log.error("未找到表单 {}, 无法初始化扩展字段", formKey);
					return;
				}
				Long maxPos = extModuleFieldMapper.getMaxFieldPosByFormId(form.getId());
				List<Map<String, Object>> extFields = formFields.stream().filter(f -> f.containsKey(UPGRADE_EXT_FIELD) && Strings.CS.equals(version, f.get(UPGRADE_EXT_FIELD).toString())).toList();
				AtomicLong pos = new AtomicLong(maxPos + 1);
				extFields.forEach(initField -> {
					ModuleField field = supplyFieldInfo(initField, form.getId(), pos.getAndIncrement(), new HashMap<>(2));
					initField.put("id", field.getId());
					fields.add(field);
					ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
					fieldBlob.setId(field.getId());
					fieldBlob.setProp(JSON.toJSONString(initField));
					fieldBlobs.add(fieldBlob);
				});
			});
			if (CollectionUtils.isNotEmpty(fields)) {
				moduleFieldMapper.batchInsert(fields);
			}
			if (CollectionUtils.isNotEmpty(fieldBlobs)) {
				moduleFieldBlobMapper.batchInsert(fieldBlobs);
			}
		} catch (Exception e) {
			log.error("表单扩展字段初始化失败", e);
			throw new GenericException("表单扩展字段初始化失败", e);
		}
	}

    /**
     * 表单及字段初始化 (升级)
     *
     * @param initKeys 初始化Key集合
     */
    private void initFormAndFields(List<String> initKeys) {
        Map<String, String> formKeyMap = new HashMap<>(FormKey.values().length);
        List<ModuleForm> forms = new ArrayList<>();
        List<ModuleFormBlob> formBlobs = new ArrayList<>();
        initKeys.forEach(formKey -> {
            ModuleForm form = new ModuleForm();
            form.setId(IDGenerator.nextStr());
            form.setFormKey(formKey);
            form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            form.setCreateUser(InternalUser.ADMIN.getValue());
            form.setCreateTime(System.currentTimeMillis());
            form.setUpdateUser(InternalUser.ADMIN.getValue());
            form.setUpdateTime(System.currentTimeMillis());
            forms.add(form);
            formKeyMap.put(formKey, form.getId());
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
     * 字段初始化 (静态json文件)
     *
     * @param formKeyMap 表单Key映射
     */
    @SuppressWarnings("unchecked")
    public void initFormFields(Map<String, String> formKeyMap) {
        List<ModuleField> fields = new ArrayList<>();
        List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
        try {
            Map<String, List<Map<String, Object>>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
            formKeyMap.keySet().forEach(key -> {
                String formId = formKeyMap.get(key);
                List<Map<String, Object>> initFields = fieldMap.get(key);
                AtomicLong pos = new AtomicLong(1L);
                // 显隐规则Key-ID映射
                Map<String, String> controlKeyPreMap = new HashMap<>(2);
                initFields.forEach(initField -> {
					if (initField.containsKey(UPGRADE_EXT_FIELD)) {
						return;
					}
					ModuleField field = supplyFieldInfo(initField, formId, pos.getAndIncrement(), controlKeyPreMap);
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
                    handleShowFieldsInit(initField, fields);
                    if (initField.containsKey(SUB_FIELDS)) {
                        List<BaseField> subFields = JSON.parseArray(JSON.toJSONString(initField.get(SUB_FIELDS)), BaseField.class);
                        subFields.forEach(subField -> subField.setId(IDGenerator.nextStr()));
                        initField.put(SUB_FIELDS, subFields);
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
            log.error("表单字段初始化失败", e);
            throw new GenericException("表单字段初始化失败", e);
        }
    }

    /**
     * 处理显示字段初始化
     * @param initField 初始化字段
     * @param initFields 如果 initForm 初始化，数据库没有数据，需要从 initFields 中获取
     */
	@SuppressWarnings("unchecked")
    private void handleShowFieldsInit(Map<String, Object> initField, List<ModuleField> initFields) {
        if (initField.containsKey(SHOW_FIELD_KEY)) {
            List<String> showFieldKeys = (List<String>) initField.get(SHOW_FIELD_KEY);
            List<ModuleField> showFields = moduleFieldMapper.selectListByLambda(new LambdaQueryWrapper<ModuleField>()
                    .in(ModuleField::getInternalKey, showFieldKeys));

            if (CollectionUtils.isEmpty(showFields)) {
                // initForm 初始化，数据库没有数据，需要从 initFields 中获取
                showFields = initFields.stream()
                        .filter(f -> showFieldKeys.contains(f.getInternalKey()))
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isNotEmpty(showFieldKeys)) {
                Set<String> internalKeys = showFields.stream()
                        .map(ModuleField::getInternalKey)
                        .collect(Collectors.toSet());

                // 添加表单字段
				List<String> showFieldResult = new ArrayList<>(showFields.stream().map(ModuleField::getId).toList());
                // 添加表单中没有的系统字段
                List<String> systemFieldKeys = showFieldKeys.stream()
                        .filter(fieldKey -> !internalKeys.contains(fieldKey))
                        .toList();
                showFieldResult.addAll(systemFieldKeys);
                initField.put(SHOW_FIELD_KEY, showFieldResult);
            }
        }
    }

    /**
	 * 组装字段基础信息
	 * @param fieldMap 字段集合
	 * @param formId 表单ID
	 * @param pos 字段位置
	 * @param controlKeyPreMap 显隐规则Key-ID映射
	 * @return 字段
	 */
	private ModuleField supplyFieldInfo(Map<String, Object> fieldMap, String formId, Long pos, Map<String, String> controlKeyPreMap) {
		ModuleField field = new ModuleField();
		field.setId(controlKeyPreMap.containsKey(field.getInternalKey()) ? controlKeyPreMap.get(field.getInternalKey()) : IDGenerator.nextStr());
		field.setFormId(formId);
		field.setInternalKey(fieldMap.get("internalKey").toString());
		field.setType(fieldMap.get("type").toString());
		field.setName(fieldMap.get("name").toString());
		field.setMobile((Boolean) fieldMap.getOrDefault("mobile", false));
		field.setPos(pos);
		field.setCreateTime(System.currentTimeMillis());
		field.setCreateUser(InternalUser.ADMIN.getValue());
		field.setUpdateTime(System.currentTimeMillis());
		field.setUpdateUser(InternalUser.ADMIN.getValue());
		return field;
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
     * 获取自定义表头集合 (包括引用显示字段)
     *
	 * @param exportHeads 导出头
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     * @return 自定义导入表头集合
     */
    public List<List<String>> getAllExportHeads(List<ExportHeadDTO> exportHeads, String formKey, String currentOrg) {
		Map<String, BaseField> fieldConfigMap = getFieldConfigMapByName(formKey, currentOrg);
		List<List<String>> heads = new ArrayList<>();
		exportHeads.forEach(exportHead -> {
			if (Strings.CS.equals(exportHead.getColumnType(), EXPORT_SYSTEM_TYPE)) {
				heads.add(new ArrayList<>(Collections.singletonList(exportHead.getTitle())));
			}
			if (!fieldConfigMap.containsKey(exportHead.getTitle())) {
				return;
			}
			BaseField field = fieldConfigMap.get(exportHead.getTitle());
			if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
				List<BaseField> subFields = subField.getSubFields();
				Map<String, String> subFieldMap = subFields.stream().collect(Collectors.toMap(
						f -> StringUtils.isNotBlank(f.getResourceFieldId()) ? f.getId() : f.idOrBusinessKey(), BaseField::getName, (oldValue, newValue) -> oldValue));
				subField.getSubFields().stream()
						.filter(BaseField::canImport)
						.forEach(f -> {
							List<String> head = new ArrayList<>();
							head.add(field.getName());
							head.add(f.getName());
							heads.add(head);
						});
				if (CollectionUtils.isNotEmpty(subField.getSumColumns())) {
					subField.getSumColumns().forEach(sumColumn -> {
						if (!subFieldMap.containsKey(sumColumn)) {
							return;
						}
						heads.add(new ArrayList<>(Collections.singletonList(Translator.get("sum") + "-" + subFieldMap.get(sumColumn))));
					});
				}
			} else {
				heads.add(new ArrayList<>(Collections.singletonList(field.getName())));
			}
		});
        return heads;
    }

	/**
	 * 获取导出的合并头ID集合
	 * @param formKey 表单Key
	 * @param currentOrg 当前组织
	 * @param exportHeads 导出表头集合
	 * @return 导出字段ID集合
	 */
	public List<String> getExportMergeHeads(String formKey, String currentOrg, List<ExportHeadDTO> exportHeads) {
		Map<String, BaseField> fieldConfigMap = getFieldConfigMapByName(formKey, currentOrg);
		List<String> heads = new ArrayList<>();
		exportHeads.forEach(exportHead -> {
			if (Strings.CS.equals(exportHead.getColumnType(), EXPORT_SYSTEM_TYPE)) {
				heads.add(exportHead.getKey());
			}
			if (!fieldConfigMap.containsKey(exportHead.getTitle())) {
				return;
			}
			BaseField field = fieldConfigMap.get(exportHead.getTitle());
			if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
				Map<String, BaseField> subFieldMap = subField.getSubFields().stream().collect(Collectors.toMap(
						f -> StringUtils.isNotBlank(f.getResourceFieldId()) ? f.getId() : f.idOrBusinessKey(), Function.identity(), (oldValue, newValue) -> oldValue));
				subField.getSubFields().stream()
						.filter(BaseField::canImport)
						.map(BaseField::getId)
						.forEach(heads::add);
				if (CollectionUtils.isNotEmpty(subField.getSumColumns())) {
					subField.getSumColumns().forEach(sumColumn -> {
						if (!subFieldMap.containsKey(sumColumn)) {
							return;
						}
						heads.add(SUM_PREFIX + subFieldMap.get(sumColumn).getId());
					});
				}
			} else {
				heads.add(field.getId());
			}
		});
		return heads;
	}

    /**
     * 获取不含引用字段的表头集合
     *
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     * @return 表头集合
     */
    public List<List<String>> getCustomImportHeadsNoRef(String formKey, String currentOrg) {
        List<BaseField> allFields = getAllFields(formKey, currentOrg);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }
        List<BaseField> fields = allFields.stream().filter(f -> StringUtils.isEmpty(f.getResourceFieldId()) && f.canImport()).toList();
        List<List<String>> heads = new ArrayList<>();
        fields.forEach(field -> {
            if (field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields())) {
                subField.getSubFields().forEach(f -> {
                    if (StringUtils.isNotEmpty(f.getResourceFieldId()) || !f.canImport()) {
                        return;
                    }
                    List<String> head = new ArrayList<>();
                    head.add(field.getName());
                    head.add(f.getName());
                    heads.add(head);
                });
            } else {
                heads.add(new ArrayList<>(Collections.singletonList(field.getName())));
            }
        });
        return heads;
    }

    /**
     * 获取自定义导出字段集合
     *
     * @param formKey    表单Key
     * @param currentOrg 当前组织
     * @return 字段集合
     */
    public List<BaseField> getAllCustomImportFields(String formKey, String currentOrg) {
        List<BaseField> allFields = getAllFields(formKey, currentOrg);
        if (CollectionUtils.isEmpty(allFields)) {
            return null;
        }
        return new ArrayList<>(allFields);
    }

    /**
     * 支持子表头
     *
     * @param headFields 表头字段集合
     * @return 是否支持
     */
    public boolean supportSubHead(List<BaseField> headFields) {
        if (CollectionUtils.isEmpty(headFields)) {
            return false;
        }
        return headFields.stream().anyMatch(field -> field instanceof SubField subField && CollectionUtils.isNotEmpty(subField.getSubFields()));
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
     * @param scenarioKey      场景Key
     * @return 填充结果
     */
    public <T, S> FormLinkFill<T> fillFormLinkValue(T target, S source, ModuleFormConfigDTO targetFormConfig,
                                                    String orgId, String sourceFormKey, String scenarioKey) throws Exception {
        FormLinkFill.FormLinkFillBuilder<T> fillBuilder = FormLinkFill.builder();
        Map<String, List<LinkScenario>> linkProp = targetFormConfig.getFormProp().getLinkProp();
        if (linkProp == null || CollectionUtils.isEmpty(linkProp.get(sourceFormKey))) {
            return fillBuilder.entity(target).build();
        }
        // 未找到联动场景，直接返回目标对象
        Optional<LinkScenario> scenarioOptional = linkProp.get(sourceFormKey).stream().filter(scenario -> Strings.CS.equals(scenario.getKey(), scenarioKey)).findFirst();
        if (scenarioOptional.isEmpty()) {
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
        Class<?> targetClass = target.getClass();
        Class<?> sourceClass = source.getClass();
        List<BaseModuleFieldValue> targetFieldVals = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<BaseModuleFieldValue> sourceFieldVals = (List<BaseModuleFieldValue>) sourceClass.getMethod("getModuleFields").invoke(source);
        for (LinkField linkField : scenarioOptional.get().getLinkFields()) {
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
                log.error("Apply source value error", e);
            }

            // 源对象中无值, 跳过取值
            if (sourceValue == null || sourceValue.getActualVal() == null) {
                continue;
            }
            // 放入目标对象字段
            putTargetFieldVal(targetField, sourceValue, targetClass, target, targetFieldVals);
        }
        return fillBuilder.entity(target).fields(targetFieldVals).build();
    }

    /**
     * 属性转方法 (name -> setName)
     *
     * @param param 属性名
     * @return 方法名
     */
    private String capitalizeSetParam(String param) {
        return "set" + param.substring(0, 1).toUpperCase() + param.substring(1);
    }

    /**
     * 属性转方法 (name -> getName)
     *
     * @param param 属性名
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
     * @return 值
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
        return customFieldResolver.transformToValue(sourceField, actualVal instanceof List ? JSON.toJSONString(actualVal) : actualVal.toString());
    }

    /**
     * 解析目标字段值
     *
     * @param targetField 目标字段
     * @param sourceVal   来源值
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
     * 处理表单联动的旧数据&&支持多场景 (客户&商机&记录)
     */
    @SuppressWarnings("unchecked")
    public void processOldLinkData() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ModuleForm::getFormKey, List.of(FormKey.CUSTOMER.getKey(), FormKey.OPPORTUNITY.getKey()));
        List<ModuleForm> forms = moduleFormMapper.selectListByLambda(wrapper);
        Map<String, String> formKeyMap = forms.stream().collect(Collectors.toMap(ModuleForm::getId, ModuleForm::getFormKey));
        List<ModuleFormBlob> moduleFormBlobs = moduleFormBlobMapper.selectByIds(formKeyMap.keySet().stream().toList());
        for (ModuleFormBlob formBlob : moduleFormBlobs) {
            Map<String, Object> propMap = JSON.parseMap(formBlob.getProp());
            Object linkProp = propMap.get("linkProp");
            Map<String, List<LinkScenario>> dataMap = new HashMap<>(2);
            String formKey = formKeyMap.get(formBlob.getId());
            if (linkProp == null) {
                if (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_CUSTOMER.name()).linkFields(new ArrayList<>()).build()));
                } else if (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey())) {
                    dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                    dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()).linkFields(new ArrayList<>()).build()));
                }
            } else {
                Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
                for (Map.Entry<String, Object> entry : linkPropMap.entrySet()) {
                    if (StringUtils.isBlank(entry.getKey()) || entry.getValue() == null || !(entry.getValue() instanceof List)) {
                        continue;
                    }
                    List<Map<String, Object>> fields = (List<Map<String, Object>>) entry.getValue();
                    List<LinkField> fieldList = fields.stream().map(field -> {
                        field.put("enable", true);
						LinkField linkField = new LinkField();
						try {
							org.apache.commons.beanutils.BeanUtils.populate(linkField, field);
						} catch (IllegalAccessException | InvocationTargetException e) {
							log.error("Populate old link field error", e);
						}
						return linkField;
                    }).toList();
                    String scenarioKey = (Strings.CS.equals(formKey, FormKey.CUSTOMER.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                            LinkScenarioKey.CLUE_TO_CUSTOMER.name() :
                            (Strings.CS.equals(formKey, FormKey.OPPORTUNITY.getKey()) && Strings.CS.equals(entry.getKey(), FormKey.CLUE.getKey()) ?
                                    LinkScenarioKey.CLUE_TO_OPPORTUNITY.name() : LinkScenarioKey.CUSTOMER_TO_OPPORTUNITY.name()));
                    LinkScenario linkScenario = LinkScenario.builder().key(scenarioKey).linkFields(fieldList).build();
                    dataMap.put(entry.getKey(), List.of(linkScenario));
                }
            }
            propMap.put("linkProp", dataMap);
            formBlob.setProp(JSON.toJSONString(propMap));
            moduleFormBlobMapper.updateById(formBlob);
        }
    }

    /**
     * 初始化跟进记录表单联动场景
     */
    @SuppressWarnings("unchecked")
    public void initFormScenarioProp() {
        LambdaQueryWrapper<ModuleForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModuleForm::getFormKey, FormKey.FOLLOW_RECORD.getKey());
        ModuleForm recordForm = moduleFormMapper.selectListByLambda(wrapper).getFirst();
        ModuleFormBlob recordFormBlob = moduleFormBlobMapper.selectByPrimaryKey(recordForm.getId());
        Map<String, Object> propMap = JSON.parseMap(recordFormBlob.getProp());
        Object linkProp = propMap.get("linkProp");
        Map<String, List<LinkScenario>> dataMap = new HashMap<>(4);
        dataMap.put(FormKey.CLUE.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CLUE_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.CUSTOMER.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.CUSTOMER_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        dataMap.put(FormKey.OPPORTUNITY.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.OPPORTUNITY_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        if (linkProp != null) {
            Map<String, Object> linkPropMap = (Map<String, Object>) linkProp;
            List<Map<String, Object>> fields = (List<Map<String, Object>>) linkPropMap.get(FormKey.FOLLOW_PLAN.getKey());
            List<LinkField> fieldList = fields.stream().map(field -> {
                LinkField linkField = JSON.parseObject(JSON.toJSONString(field), LinkField.class);
                linkField.setEnable(true);
                return linkField;
            }).toList();
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(fieldList).build()));
        } else {
            dataMap.put(FormKey.FOLLOW_PLAN.getKey(), List.of(LinkScenario.builder().key(LinkScenarioKey.PLAN_TO_RECORD.name()).linkFields(new ArrayList<>()).build()));
        }
        propMap.put("linkProp", dataMap);
        recordFormBlob.setProp(JSON.toJSONString(propMap));
        moduleFormBlobMapper.updateById(recordFormBlob);
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
			if (propMap.containsKey("viewSize")) {
				continue;
			}
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

    @SuppressWarnings("unchecked")
    public void modifyPhoneFieldFormat() {
        LambdaQueryWrapper<ModuleField> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ModuleField::getType, FieldType.PHONE.name());
        List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(lambdaQueryWrapper);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectByIds(fieldIds);
        for (ModuleFieldBlob fieldBlob : moduleFieldBlobs) {
            Map<String, Object> propMap = JSON.parseMap(fieldBlob.getProp());
            propMap.put("format", "255");
            fieldBlob.setProp(JSON.toJSONString(propMap));
            moduleFieldBlobMapper.updateById(fieldBlob);
        }
    }

    /**
     * 获取MCP表单需要的字段
     *
     * @param formKey        表单Key
     * @param organizationId 组织ID
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
            simpleField.setShowControlRules(field.getShowControlRules());
            if (field instanceof HasOption fieldWithOption) {
                simpleField.setOptions(fieldWithOption.getOptions());
            }
            if (field instanceof DatasourceField datasourceField) {
                simpleField.setDataSourceType(datasourceField.getDataSourceType());
            } else if (field instanceof LocationField locationField) {
                simpleField.setLocationType(locationField.getLocationType());
            } else if (field instanceof DateTimeField dateTimeField) {
                simpleField.setDateType(dateTimeField.getDateType());
            }
            return simpleField;
        }).toList();
    }

    /**
     * 获取选项Key
     *
     * @param showFields 显示字段
     * @param baseField 字段配置
     * @return 字段唯一Key
     */
    private String getOptionKey(List<String> showFields, BaseField baseField) {
        if (showFields.contains(baseField.getId())) {
            return baseField.getId();
        }
        return baseField.idOrBusinessKey();
    }

    /**
     * 业务Key => 字段ID (子字段)
     *
     * @param fieldValues 自定义字段值
     * @param formConfig  字段配置
     * @return 处理后的自定义字段值
     */
    public <T extends BaseResourceField, V extends BaseResourceField> List<BaseModuleFieldValue> resolveSnapshotFields(List<BaseModuleFieldValue> fieldValues,
		   ModuleFormConfigDTO formConfig, BaseResourceFieldService<T, V> baseResourceFieldService, String resourceId) {
        // 1. 扁平化所有字段
        final List<BaseField> flattenFields = flattenFormAllFields(formConfig);
		List<BaseModuleFieldValue> resolveFvs = resolveSubKeyToId(fieldValues, flattenFields);

		// 2. 处理数据源引用字段
		List<BaseModuleFieldValue> resolveRefFvs = resolveRefFields(resolveFvs, flattenFields, baseResourceFieldService);

		// 3. 补充流水号字段
		Optional<BaseField> serialField = flattenFields.stream().filter(BaseField::isSerialNumber).findAny();
		if (serialField.isPresent() && StringUtils.isEmpty(serialField.get().getBusinessKey())) {
			Object serialVal = baseResourceFieldService.getResourceFieldValue(resourceId, serialField.get().getId());
			if (serialVal != null) {
				resolveRefFvs.add(new BaseModuleFieldValue(serialField.get().getId(), serialVal));
			}
		}

        return resolveRefFvs;
    }

	/**
	 * 处理数据源引用字段
	 * @param resolveFvs 字段值列表
	 * @param flattenFields 平铺字段配置
	 * @param baseResourceFieldService 字段服务类
	 * @return 处理后的字段值列表
	 * @param <F> 字段类型
	 * @param <B> 大字段类型
	 */
	@SuppressWarnings("unchecked")
	private <F extends BaseResourceField, B extends BaseResourceField> List<BaseModuleFieldValue> resolveRefFields(List<BaseModuleFieldValue> resolveFvs, List<BaseField> flattenFields,
														BaseResourceFieldService<F, B> baseResourceFieldService) {
		// 1. 数据源字段配置映射 && 字段配置映射
		final Map<String, BaseField> sourceConfigMap = flattenFields.stream()
				.filter(f -> f instanceof DatasourceField ds
						&& org.apache.commons.collections.CollectionUtils.isNotEmpty(ds.getShowFields()))
				.collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> f, (f1, f2) -> f1));
		final Map<String, BaseField> fieldMap = flattenFields.stream()
				.collect(Collectors.toMap(BaseField::getId, f -> f, (f1, f2) -> f1));
		// 2. 处理引用字段值
		List<BaseModuleFieldValue> reFvs = new ArrayList<>();
		resolveFvs.forEach(fv -> {
			if (fv.getFieldValue() == null) {
				return;
			}
			final String fieldId = fv.getFieldId();
			// 数据源字段（普通字段）
			if (sourceConfigMap.containsKey(fieldId)) {
				final DatasourceField sourceField = (DatasourceField) sourceConfigMap.get(fieldId);
				final FieldSourceType sourceType = FieldSourceType.valueOf(sourceField.getDataSourceType());
				final Object detail = fieldSourceServiceProvider.safeGetById(sourceType, fv.getFieldValue().toString());
				if (detail == null) {
					return;
				}
				final Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
				sourceField.getShowFields().forEach(refId -> {
					final BaseField showFieldConf = fieldMap.get(refId);
					if (showFieldConf != null) {
						final Object val = baseResourceFieldService
								.getFieldValueOfDetailMap(showFieldConf, detailMap);
						reFvs.add(new BaseModuleFieldValue(refId, val));
					}
				});
				return;
			}
			// 子字段嵌套数据源字段
			if (fieldMap.containsKey(fieldId) && fieldMap.get(fieldId).isSubField()) {
				final List<Map<String, Object>> subValues = (List<Map<String, Object>>) fv.getFieldValue();
				subValues.forEach(sfv -> {
					final Map<String, Object> showFieldMap = new HashMap<>(8);
					sfv.forEach((k, v) -> {
						if (v == null || !sourceConfigMap.containsKey(k)) {
							return;
						}
						final DatasourceField sourceField = (DatasourceField) sourceConfigMap.get(k);
						final FieldSourceType sourceType = FieldSourceType.valueOf(sourceField.getDataSourceType());
						final Object detail = fieldSourceServiceProvider.safeGetById(sourceType, v.toString());
						if (detail == null) {
							return;
						}
						final Map<String, Object> detailMap = JSON.MAPPER.convertValue(detail, Map.class);
						if (MapUtils.isEmpty(detailMap)) {
							return;
						}
						sourceField.getShowFields().forEach(id -> {
							final BaseField showFieldConf = fieldMap.get(id);
							if (showFieldConf == null) {
								return;
							}
							if (StringUtils.isNotEmpty(showFieldConf.getSubTableFieldId()) && sfv.containsKey(PRICE_SUB_ROW_KEY)) {
								Object matchVal = baseResourceFieldService.matchSubFieldValueOfDetailMap(showFieldConf.idOrBusinessKey(), detailMap, BusinessModuleField.PRICE_PRODUCT_TABLE.getBusinessKey(),
										sfv.get(PRICE_SUB_ROW_KEY).toString());
								if (matchVal != null) {
									showFieldMap.put(showFieldConf.getId(), matchVal);
								}
							} else {
								showFieldMap.put(id, baseResourceFieldService.getFieldValueOfDetailMap(showFieldConf, detailMap));
							}
						});
					});
					sfv.putAll(showFieldMap);
				});
			}
		});
		if (!reFvs.isEmpty()) {
			resolveFvs.addAll(reFvs);
		}
		return resolveFvs;
	}

	/**
	 * 解析子表格Key=>ID
	 * @param fvs 所有字段值
	 * @param flattenFields 所有字段配置
	 * @return 处理后的字段值
	 */
	private List<BaseModuleFieldValue> resolveSubKeyToId(List<BaseModuleFieldValue> fvs, List<BaseField> flattenFields) {
		// 获取替换后的子表格值
		final Map<String, BaseField> subFieldConfigMap = flattenFields.stream().filter(f -> f instanceof SubField)
				.collect(Collectors.toMap(BaseField::idOrBusinessKey, f -> f));
		final List<BaseModuleFieldValue> subFieldValues = fvs.stream()
				.filter(fv -> subFieldConfigMap.containsKey(fv.getFieldId())
						&& subFieldConfigMap.get(fv.getFieldId()).isSubField())
				.map(fv -> new BaseModuleFieldValue(
						subFieldConfigMap.get(fv.getFieldId()).getId(),
						fv.getFieldValue()
				)).toList();

		// 删除原 subField 项，并加入替换后的
		fvs.removeIf(fv -> subFieldConfigMap.containsKey(fv.getFieldId())
				&& subFieldConfigMap.get(fv.getFieldId()).isSubField());

		if (!subFieldValues.isEmpty()) {
			fvs.addAll(subFieldValues);
		}
		return fvs;
	}

	/**
	 * 获取字段配置映射 (name => BaseField)
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @return 配置映射
	 */
	private Map<String, BaseField> getFieldConfigMapByName(String formKey, String orgId) {
		List<BaseField> allFields = getAllFields(formKey, orgId);
		if (CollectionUtils.isEmpty(allFields)) {
			return Map.of();
		}
		return allFields.stream().collect(Collectors.toMap(BaseField::getName, Function.identity(), (p, n) -> p));
	}

	/**
	 * 获取表单附件字段ID集合
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @return 字段ID集合
	 */
	public List<String> getFieldIdsOfForm(String formKey, String orgId) {
		List<BaseField> allFields = getAllFields(formKey, orgId);
		if (CollectionUtils.isEmpty(allFields)) {
			return List.of();
		}
		return allFields.stream().filter(BaseField::isAttachment)
				.map(BaseField::getId)
				.toList();
	}

	/**
	 * 获取流水号字段规则
	 * @param formKey 表单Key
	 * @param currentOrg 当前组织
	 * @param internalKey 字段Key
	 * @return 字段规则集合
	 */
	public List<String> getSerialFieldRulesByKey(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return new ArrayList<>();
		}
		SerialNumberField serialNumberField = JSON.parseObject(blob.getProp(), SerialNumberField.class);
		return serialNumberField.getSerialNumberRules();
	}

	/**
	 * 获取字段选项
	 * @param formKey 表单Key
	 * @param currentOrg 组织ID
	 * @param internalKey 字段内置Key
	 * @return 字段选项
	 */
	public List<OptionProp> getFieldOptions(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return new ArrayList<>();
		}
		BaseField field = JSON.parseObject(blob.getProp(), BaseField.class);
		if (field instanceof HasOption of) {
			return of.getOptions();
		}
		return new ArrayList<>();
	}

	/**
	 * 获取日期字段类型
	 * @param formKey 表单Key
	 * @param currentOrg 组织ID
	 * @param internalKey 内部Key
	 * @return 日期字段类型
	 */
	public String getDateFieldType(String formKey, String currentOrg, String internalKey) {
		ModuleFieldBlob blob = getFieldBlobByKey(formKey, currentOrg, internalKey);
		if (blob == null) {
			return "datetime";
		}
		DateTimeField dateField = JSON.parseObject(blob.getProp(), DateTimeField.class);
		return dateField.getDateType();
	}

	/**
	 * 根据字段Key获取额外信息
	 * @param formKey 表单Key
	 * @param orgId 组织ID
	 * @param internalKey 字段内置Key
	 * @return 字段大文本
	 */
	private ModuleFieldBlob getFieldBlobByKey(String formKey, String orgId, String internalKey) {
		ModuleForm example = new ModuleForm();
		example.setFormKey(formKey);
		example.setOrganizationId(orgId);
		ModuleForm moduleForm = moduleFormMapper.selectOne(example);
		ModuleField fieldExample = new ModuleField();
		fieldExample.setFormId(moduleForm.getId());
		fieldExample.setInternalKey(internalKey);
		ModuleField moduleField = moduleFieldMapper.selectOne(fieldExample);
		if (moduleField == null) {
			return null;
		}
		return moduleFieldBlobMapper.selectByPrimaryKey(moduleField.getId());
	}
}
