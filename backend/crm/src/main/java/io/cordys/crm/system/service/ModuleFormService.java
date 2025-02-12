package io.cordys.crm.system.service;

import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldOption;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.dto.response.ModuleFieldOptionDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;
import io.cordys.crm.system.mapper.ExtModuleFieldOptionMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ModuleFormService {

	@Resource
	private BaseMapper<ModuleForm> moduleFormMapper;
	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;
	@Resource
	private BaseMapper<ModuleFieldOption> moduleFieldOptionMapper;
	@Resource
	private ExtModuleFieldMapper extModuleFieldMapper;
	@Resource
	private ExtModuleFieldOptionMapper extModuleFieldOptionMapper;

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
		ModuleFormDTO formDTO = new ModuleFormDTO();
		BeanUtils.copyBean(formDTO, form);
		formConfig.setForm(formDTO);
		// set fields
		formConfig.setFields(getAllFields(form.getId()));
		return formConfig;
	}

	/**
	 * 保存表单配置
	 * @param saveParam 保存参数
	 * @return 表单配置
	 */
	public ModuleFormConfigDTO save(ModuleFormSaveRequest saveParam, String currentUserId, String currentOrgId) {
		// handle form
		LambdaQueryWrapper<ModuleForm> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ModuleForm::getFormKey, saveParam.getFormKey()).eq(ModuleForm::getOrganizationId, currentOrgId);
		List<ModuleForm> forms = moduleFormMapper.selectListByLambda(queryWrapper);
		ModuleForm saveForm = new ModuleForm();
		BeanUtils.copyBean(saveForm, saveParam.getForm());
		saveForm.setFormKey(saveParam.getFormKey());
		saveForm.setOrganizationId(currentOrgId);
		saveForm.setUpdateTime(System.currentTimeMillis());
		saveForm.setUpdateUser(currentUserId);
		if (CollectionUtils.isEmpty(forms)) {
			// new form
			saveForm.setId(IDGenerator.nextStr());
			saveForm.setCreateTime(System.currentTimeMillis());
			saveForm.setCreateUser(currentUserId);
			moduleFormMapper.insert(saveForm);
		} else {
			// edited form
			ModuleForm originalForm = forms.getFirst();
			saveForm.setId(originalForm.getId());
			saveForm.setUpdateTime(System.currentTimeMillis());
			moduleFormMapper.updateById(saveForm);
		}

		if (CollectionUtils.isNotEmpty(saveParam.getDeleteFieldIds())) {
			// remove deleted fields
			extModuleFieldMapper.deleteByIds(saveParam.getDeleteFieldIds());
			extModuleFieldOptionMapper.deleteByFieldIds(saveParam.getDeleteFieldIds());
		}
		if (CollectionUtils.isNotEmpty(saveParam.getFields())) {
			// handle edit fields and options
			List<ModuleField> addFields = new ArrayList<>();
			List<ModuleField> updateFields = new ArrayList<>();
			List<ModuleFieldOption> addOptions = new ArrayList<>();
			List<ModuleFieldOption> updateOptions = new ArrayList<>();
			List<String> deleteOptionIds = new ArrayList<>();
			saveParam.getFields().forEach(field -> {
				if (field.getId() == null) {
					field.setId(IDGenerator.nextStr());
					addFields.add(buildField(field, currentUserId, saveForm.getId(), false));
				} else {
					updateFields.add(buildField(field, currentUserId, saveForm.getId(), true));
				}
				// handle field option
				if (FieldType.hasOption(field.getType())) {
					handleFieldOptions(addOptions, updateOptions, deleteOptionIds, field.getId(), CollectionUtils.isEmpty(field.getOptions()) ? List.of() : field.getOptions());
				}
			});
			if (CollectionUtils.isNotEmpty(addFields)) {
				// new fields
				moduleFieldMapper.batchInsert(addFields);
			}
			if (CollectionUtils.isNotEmpty(updateFields)) {
				// edited fields
				updateFields.forEach(field -> moduleFieldMapper.update(field));
			}
			if (CollectionUtils.isNotEmpty(addOptions)) {
				// new options
				moduleFieldOptionMapper.batchInsert(addOptions);
			}
			if (CollectionUtils.isNotEmpty(updateOptions)) {
				// edited options
				updateOptions.forEach(option -> moduleFieldOptionMapper.update(option));
			}
			if (CollectionUtils.isNotEmpty(deleteOptionIds)) {
				// deleted options
				extModuleFieldOptionMapper.deleteByFieldIds(deleteOptionIds);
			}
		}

		// get form config
		return getConfig(saveForm.getFormKey(), currentOrgId);
	}

	/**
	 * 处理字段选项
	 * @param addOptions	新增选项集合
	 * @param updateOptions	修改选项集合
	 * @param deleteOptionIds	删除的选项ID集合
	 * @param fieldId	字段ID
	 * @param saveOptions 保存的选项集合
	 */
	public void handleFieldOptions(List<ModuleFieldOption> addOptions, List<ModuleFieldOption> updateOptions, List<String> deleteOptionIds,
								   String fieldId, List<ModuleFieldOptionDTO> saveOptions) {
		LambdaQueryWrapper<ModuleFieldOption> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ModuleFieldOption::getFieldId, fieldId);
		List<ModuleFieldOption> originalOptions = moduleFieldOptionMapper.selectListByLambda(queryWrapper);
		List<ModuleFieldOption> modifiedOptions = saveOptions.stream().map(option -> {
			ModuleFieldOption fieldOption = new ModuleFieldOption();
			fieldOption.setId(option.getId());
			fieldOption.setLabel(option.getLabel());
			fieldOption.setFieldId(fieldId);
			return fieldOption;
		}).toList();
		if (CollectionUtils.isNotEmpty(originalOptions) || CollectionUtils.isNotEmpty(modifiedOptions)) {
			if (CollectionUtils.isEmpty(originalOptions)) {
				addOptions.addAll(modifiedOptions);
			}
			if (CollectionUtils.isEmpty(modifiedOptions)) {
				deleteOptionIds.addAll(originalOptions.stream().map(ModuleFieldOption::getId).toList());
			}
			modifiedOptions.forEach(modifiedOption -> {
				boolean noMatch = originalOptions.stream().noneMatch(option -> StringUtils.equals(option.getId(), modifiedOption.getId()));
				if (noMatch) {
					addOptions.add(modifiedOption);
				} else {
					updateOptions.add(modifiedOption);
				}
			});
			originalOptions.forEach(originalOption -> {
				boolean noMatch = modifiedOptions.stream().noneMatch(option -> StringUtils.equals(option.getId(), originalOption.getId()));
				if (noMatch) {
					deleteOptionIds.add(originalOption.getId());
				}
			});
		}
	}

	/**
	 * 生成字段
	 * @param field 字段DTO
	 * @param currentUserId 当前用户
	 * @param edited 是否修改
	 * @return 模块字段
	 */
	public ModuleField buildField(ModuleFieldDTO field, String currentUserId, String formId, boolean edited) {
		ModuleField moduleField = new ModuleField();
		BeanUtils.copyBean(moduleField, field);
		moduleField.setFormId(formId);
		if (!edited) {
			moduleField.setCreateTime(System.currentTimeMillis());
			moduleField.setCreateUser(currentUserId);
		}
		moduleField.setUpdateTime(System.currentTimeMillis());
		moduleField.setUpdateUser(currentUserId);
		return moduleField;
	}

	/**
	 * 获取表单所有字段集合
	 * @param formId 表单ID
	 * @return 字段集合
	 */
	public List<ModuleFieldDTO> getAllFields(String formId) {
		// set field
		List<ModuleFieldDTO> fieldDTOList = new ArrayList<>();
		LambdaQueryWrapper<ModuleField> fieldWrapper = new LambdaQueryWrapper<>();
		fieldWrapper.eq(ModuleField::getFormId, formId);
		List<ModuleField> fields = moduleFieldMapper.selectListByLambda(fieldWrapper);
		if (CollectionUtils.isNotEmpty(fields)) {
			List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
			LambdaQueryWrapper<ModuleFieldOption> optionWrapper = new LambdaQueryWrapper<>();
			optionWrapper.in(ModuleFieldOption::getFieldId, fieldIds);
			List<ModuleFieldOption> fieldOptions = moduleFieldOptionMapper.selectListByLambda(optionWrapper);
			Map<String, List<ModuleFieldOption>> fieldOptionMap = fieldOptions.stream().collect(Collectors.groupingBy(ModuleFieldOption::getFieldId));
			fields.forEach(field -> {
				ModuleFieldDTO fieldDTO = new ModuleFieldDTO();
				BeanUtils.copyBean(fieldDTO, field);
				fieldDTO.setOptions(fieldOptionMap.containsKey(field.getId()) ? toOptionDTO(fieldOptionMap.get(field.getId())) : List.of());
				fieldDTOList.add(fieldDTO);
			});
		}
		return fieldDTOList;
	}

	/**
	 * 解析成选项DTO
	 * @param options 选项集合
	 * @return 选项DTO集合
	 */
	private List<ModuleFieldOptionDTO> toOptionDTO(List<ModuleFieldOption> options) {
		return options.stream().map(option -> {
			ModuleFieldOptionDTO optionDTO = new ModuleFieldOptionDTO();
			optionDTO.setId(option.getId());
			optionDTO.setLabel(option.getLabel());
			return optionDTO;
		}).toList();
	}
}
