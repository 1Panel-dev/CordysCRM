package io.cordys.crm.system.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldBlob;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.domain.ModuleFormBlob;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.form.FormProp;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.BusinessModuleFieldDTO;
import io.cordys.crm.system.dto.response.BusinessModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
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

	private static final String DEFAULT_ORGANIZATION_ID = "100001";

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
		formConfig.setFormProp(JSON.parseObject(new String(formBlob.getProp()), FormProp.class));
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
	public BusinessModuleFormConfigDTO getBusinessFormConfig(String formKey, String organizationId) {
		ModuleFormConfigDTO config = getConfig(formKey, organizationId);
		BusinessModuleFormConfigDTO businessModuleFormConfig = new BusinessModuleFormConfigDTO();
		businessModuleFormConfig.setFormProp(config.getFormProp());

		// 获取特殊的业务字段
		Map<String, BusinessModuleField> businessModuleFieldMap = Arrays.stream(BusinessModuleField.values()).
				collect(Collectors.toMap(BusinessModuleField::getKey, Function.identity()));

		businessModuleFormConfig.setFields(
				config.getFields()
						.stream()
						.map(moduleFieldDTO -> {
							BusinessModuleFieldDTO businessModuleField = BeanUtils.copyBean(new BusinessModuleFieldDTO(), moduleFieldDTO);
							BusinessModuleField businessModuleFieldEnum = businessModuleFieldMap.get(businessModuleField.getInternalKey());
							if (businessModuleFieldEnum != null) {
								// 设置特殊的业务字段 key
								businessModuleField.setBusinessKey(businessModuleFieldEnum.getBusinessKey());
							}
							return businessModuleField;
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
	public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
		// 处理表单
		LambdaQueryWrapper<ModuleForm> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ModuleForm::getFormKey, saveParam.getFormKey()).eq(ModuleForm::getOrganizationId, currentOrgId);
		List<ModuleForm> forms = moduleFormMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(forms)) {
			throw new GenericException(Translator.get("module.form.not_exist"));
		}
		ModuleForm form = forms.getFirst();
		form.setUpdateUser(currentUserId);
		form.setUpdateTime(System.currentTimeMillis());
		moduleFormMapper.updateById(form);
		ModuleFormBlob formBlob = new ModuleFormBlob();
		formBlob.setId(form.getId());
		formBlob.setProp(JSON.toJSONBytes(saveParam.getFormProp()));
		moduleFormBlobMapper.updateById(formBlob);

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
				moduleField.setInternalKey(field.getInternalKey());
				moduleField.setType(field.getType());
				moduleField.setPos(pos.getAndIncrement());
				moduleField.setCreateTime(System.currentTimeMillis());
				moduleField.setCreateUser(currentUserId);
				moduleField.setUpdateTime(System.currentTimeMillis());
				moduleField.setUpdateUser(currentUserId);
				addFields.add(moduleField);
				ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
				fieldBlob.setId(field.getId());
				fieldBlob.setProp(JSON.toJSONBytes(field));
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
			List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
			LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
			blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
			List<ModuleFieldBlob> fieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
			Map<String, byte[]> fieldBlobMap = fieldBlobs.stream().collect(Collectors.toMap(ModuleFieldBlob::getId, ModuleFieldBlob::getProp));
			fields.forEach(field -> {
				BaseField baseField = JSON.parseObject(new String(fieldBlobMap.get(field.getId())), BaseField.class);
				fieldDTOList.add(baseField);
			});
		}
		return fieldDTOList;
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
				formBlob.setProp(JSON.toJSONBytes(formProp));
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
	public void initFormFields(Map<String, String> formKeyMap) {
		List<ModuleField> fields = new ArrayList<>();
		List<ModuleFieldBlob> fieldBlobs = new ArrayList<>();
		try {
			Map<String, List<Map>> fieldMap = JSON.parseObject(fieldResource.getInputStream(), Map.class);
			fieldMap.keySet().forEach(key -> {
				String formId = formKeyMap.get(key);
				List<Map> initFields = fieldMap.get(key);
				AtomicLong pos = new AtomicLong(1L);
				initFields.forEach(initField -> {
					ModuleField field = new ModuleField();
					field.setId(IDGenerator.nextStr());
					field.setFormId(formId);
					field.setInternalKey(initField.get("internalKey").toString());
					field.setType(initField.get("type").toString());
					field.setPos(pos.getAndIncrement());
					field.setCreateTime(System.currentTimeMillis());
					field.setCreateUser(InternalUser.ADMIN.getValue());
					field.setUpdateTime(System.currentTimeMillis());
					field.setUpdateUser(InternalUser.ADMIN.getValue());
					initField.put("id", field.getId());
					fields.add(field);
					ModuleFieldBlob fieldBlob = new ModuleFieldBlob();
					fieldBlob.setId(field.getId());
					fieldBlob.setProp(JSON.toJSONBytes(initField));
					fieldBlobs.add(fieldBlob);
				});
			});
			moduleFieldMapper.batchInsert(fields);
			moduleFieldBlobMapper.batchInsert(fieldBlobs);
		} catch (Exception e) {
			throw new GenericException("表单字段初始化失败", e);
		}
	}
}
