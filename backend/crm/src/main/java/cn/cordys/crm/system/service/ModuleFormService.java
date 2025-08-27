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
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.ControlRuleProp;
import cn.cordys.crm.system.dto.field.base.HasOption;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.crm.system.dto.form.FormProp;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ModuleFormService {

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

	private static final String DEFAULT_ORGANIZATION_ID = "100001";

	private static final String CONTROL_RULES_KEY = "showControlRules";

	/**
	 * 获取模块表单配置
	 * @param formKey 表单Key
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
	 * @param formKey 表单Key
	 * @param organizationId 组织ID
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
	 * @param formConfig 表单配置
	 * @param allDataFields 所有数据字段
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
			if (Strings.CS.equalsAny(field.getType(), FieldType.SELECT.name(), FieldType.SELECT_MULTIPLE.name()) ) {
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

		Map<String, String> sourceMap = initTypeSourceMap();
		typeIdsMap.forEach((fieldId, ids) -> {
			List<OptionDTO> options = extModuleFieldMapper.getSourceOptionsByIds(sourceMap.get(idTypeMap.get(fieldId)), ids);
			if (CollectionUtils.isNotEmpty(options)) {
				optionMap.put(fieldId, options);
			}
		});
		return optionMap;
	}

	/**
	 * OptionProp转OptionDTO
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
	 * @param list 列表数据
	 * @param getOptionIdFunc 获取选项ID函数
	 * @param getOptionNameFunc 获取选项名称函数
	 * @param <T>
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
				.map(getModuleFieldFunc::apply)
				.filter(org.apache.commons.collections.CollectionUtils::isNotEmpty)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	/**
	 * 获取
	 * @param formKey 表单Key
	 * @param currentOrg 当前组织
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
	 * @param formKey 表单Key
	 * @param orgId 组织ID
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
}
