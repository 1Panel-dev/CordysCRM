package io.cordys.crm.system.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldOption;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleFieldSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;
import io.cordys.crm.system.mapper.ExtModuleFieldOptionMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModuleFieldService {

	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;
	@Resource
	private BaseMapper<ModuleFieldOption> moduleFieldOptionMapper;
	@Resource
	private BaseMapper<ModuleForm> moduleFormMapper;
	@Resource
	private ExtModuleFieldMapper extModuleFieldMapper;
	@Resource
	private ExtModuleFieldOptionMapper extModuleFieldOptionMapper;

	public List<ModuleFieldDTO> getFieldList(ModuleFieldRequest request) {
		return getFieldList(request.getModuleId());
	}

	/**
	 * 获取模块字段集合
	 * @param moduleId 请求参数
	 * @return 字段集合
	 */
	@Cacheable(value = "fields", key = "#moduleId", unless = "#result == null")
	public List<ModuleFieldDTO> getFieldList(String moduleId) {
		List<ModuleFieldDTO> fieldDTOList = new ArrayList<>();
		LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ModuleField::getModuleId, moduleId);
		List<ModuleField> fields = moduleFieldMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(fields)) {
			return fieldDTOList;
		}
		List<String> fieldIds = fields.stream().map(ModuleField::getId).toList();
		LambdaQueryWrapper<ModuleFieldOption> optionQueryWrapper = new LambdaQueryWrapper<>();
		optionQueryWrapper.in(ModuleFieldOption::getFieldId, fieldIds);
		List<ModuleFieldOption> fieldOptions = moduleFieldOptionMapper.selectListByLambda(optionQueryWrapper);
		Map<String, List<ModuleFieldOption>> fieldOptionMap = fieldOptions.stream().collect(Collectors.groupingBy(ModuleFieldOption::getFieldId));
		fields.forEach(field -> {
			ModuleFieldDTO fieldDTO = new ModuleFieldDTO();
			BeanUtils.copyBean(fieldDTO, field);
			fieldDTO.setOptions(fieldOptionMap.containsKey(field.getId()) ? fieldOptionMap.get(field.getId()) : List.of());
			fieldDTOList.add(fieldDTO);
		});
		return fieldDTOList;
	}

	/**
	 * 保存模块字段
	 * @param saveParam 保存参数
	 * @return 保存后的字段集合
	 */
	@Transactional(rollbackFor = Exception.class)
	@CachePut(value = "fields", key = "#saveParam.moduleId", unless = "#result == null")
	public List<ModuleFieldDTO> save(ModuleFieldSaveRequest saveParam, String currentUserId) {
		// remove deleted fields
		if (CollectionUtils.isNotEmpty(saveParam.getDeleteFieldIds())) {
			extModuleFieldMapper.deleteByIds(saveParam.getDeleteFieldIds());
			extModuleFieldOptionMapper.deleteByFieldIds(saveParam.getDeleteFieldIds());
		}
		if (CollectionUtils.isEmpty(saveParam.getFields())) {
			return List.of();
		}
		List<ModuleField> addFields = new ArrayList<>();
		List<ModuleField> updateFields = new ArrayList<>();
		List<ModuleFieldOption> addFieldOptions = new ArrayList<>();
		saveParam.getFields().forEach(field -> {
			if (field.getId() == null) {
				// add new field
				field.setId(IDGenerator.nextStr());
				addFields.add(buildField(field, currentUserId, true));
			} else {
				// update field
				updateFields.add(buildField(field, currentUserId, false));
			}
			// handle field option
			if (CollectionUtils.isNotEmpty(field.getOptions())) {
				List<ModuleFieldOption> fieldOptions = field.getOptions().stream().map(option -> {
					option.setId(IDGenerator.nextStr());
					option.setFieldId(field.getId());
					return buildFieldOption(option, currentUserId);
				}).toList();
				addFieldOptions.addAll(fieldOptions);
			}
		});
		if (CollectionUtils.isNotEmpty(addFields)) {
			moduleFieldMapper.batchInsert(addFields);
		}
		if (CollectionUtils.isNotEmpty(updateFields)) {
			updateFields.forEach(field -> moduleFieldMapper.update(field));
			extModuleFieldOptionMapper.deleteByFieldIds(updateFields.stream().map(ModuleField::getId).toList());
		}
		if (CollectionUtils.isNotEmpty(addFieldOptions)) {
			moduleFieldOptionMapper.batchInsert(addFieldOptions);
		}
		// 表单属性
		ModuleForm form = saveParam.getForm();
		form.setUpdateTime(System.currentTimeMillis());
		form.setUpdateUser(currentUserId);
		if (form.getId() == null) {
			form.setId(IDGenerator.nextStr());
			form.setModuleId(saveParam.getModuleId());
			form.setCreateTime(System.currentTimeMillis());
			form.setCreateUser(currentUserId);
			moduleFormMapper.insert(form);
		} else {
			moduleFormMapper.updateById(form);
		}
		return saveParam.getFields();
	}

	/**
	 * 生成字段
	 * @param field 字段DTO
	 * @param currentUserId 当前用户
	 * @param isNew 是否新增
	 * @return 模块字段
	 */
	public ModuleField buildField(ModuleFieldDTO field, String currentUserId, boolean isNew) {
		ModuleField moduleField = new ModuleField();
		if (isNew) {
			field.setCreateTime(System.currentTimeMillis());
			field.setCreateUser(currentUserId);
		}
		field.setUpdateTime(System.currentTimeMillis());
		field.setUpdateUser(currentUserId);
		BeanUtils.copyBean(moduleField, field);
		return moduleField;
	}

	/**
	 * 生成字段选项
	 * @param fieldOption 字段选项
	 * @param currentUserId 当前用户
	 * @return 字段选项
	 */
	public ModuleFieldOption buildFieldOption(ModuleFieldOption fieldOption, String currentUserId) {
		fieldOption.setCreateTime(System.currentTimeMillis());
		fieldOption.setCreateUser(currentUserId);
		fieldOption.setUpdateTime(System.currentTimeMillis());
		fieldOption.setUpdateUser(currentUserId);
		return fieldOption;
	}
}
