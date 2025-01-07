package io.cordys.crm.system.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.system.domain.SysModuleField;
import io.cordys.crm.system.domain.SysModuleFieldOption;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleFieldSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;
import io.cordys.crm.system.mapper.ExtModuleFieldOptionMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModuleFieldService {

	@Resource
	private BaseMapper<SysModuleField> moduleFieldMapper;
	@Resource
	private BaseMapper<SysModuleFieldOption> moduleFieldOptionMapper;
	@Resource
	private ExtModuleFieldMapper extModuleFieldMapper;
	@Resource
	private ExtModuleFieldOptionMapper extModuleFieldOptionMapper;

	/**
	 * 获取模块字段集合
	 * @param request 请求参数
	 * @return 字段集合
	 */
	public List<ModuleFieldDTO> getFieldList(ModuleFieldRequest request) {
		List<ModuleFieldDTO> fieldDTOList = new ArrayList<>();
		LambdaQueryWrapper<SysModuleField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SysModuleField::getModuleId, request.getModuleId());
		List<SysModuleField> fields = moduleFieldMapper.selectListByLambda(queryWrapper);
		if (CollectionUtils.isEmpty(fields)) {
			return fieldDTOList;
		}
		List<String> fieldIds = fields.stream().map(SysModuleField::getId).toList();
		LambdaQueryWrapper<SysModuleFieldOption> optionQueryWrapper = new LambdaQueryWrapper<>();
		optionQueryWrapper.in(SysModuleFieldOption::getFieldId, fieldIds);
		List<SysModuleFieldOption> fieldOptions = moduleFieldOptionMapper.selectListByLambda(optionQueryWrapper);
		Map<String, List<SysModuleFieldOption>> fieldOptionMap = fieldOptions.stream().collect(Collectors.groupingBy(SysModuleFieldOption::getFieldId));
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
	public List<ModuleFieldDTO> save(ModuleFieldSaveRequest saveParam, String currentUserId) {
		// remove deleted fields
		if (CollectionUtils.isNotEmpty(saveParam.getDeleteFieldIds())) {
			extModuleFieldMapper.deleteByIds(saveParam.getDeleteFieldIds());
			extModuleFieldOptionMapper.deleteByFieldIds(saveParam.getDeleteFieldIds());
		}
		if (CollectionUtils.isEmpty(saveParam.getFields())) {
			return List.of();
		}
		List<SysModuleField> addFields = new ArrayList<>();
		List<SysModuleField> updateFields = new ArrayList<>();
		List<SysModuleFieldOption> addFieldOptions = new ArrayList<>();
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
				List<SysModuleFieldOption> fieldOptions = field.getOptions().stream().map(option -> {
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
			extModuleFieldOptionMapper.deleteByFieldIds(updateFields.stream().map(SysModuleField::getId).toList());
		}
		if (CollectionUtils.isNotEmpty(addFieldOptions)) {
			moduleFieldOptionMapper.batchInsert(addFieldOptions);
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
	public SysModuleField buildField(ModuleFieldDTO field, String currentUserId, boolean isNew) {
		SysModuleField moduleField = new SysModuleField();
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
	public SysModuleFieldOption buildFieldOption(SysModuleFieldOption fieldOption, String currentUserId) {
		fieldOption.setCreateTime(System.currentTimeMillis());
		fieldOption.setCreateUser(currentUserId);
		fieldOption.setUpdateTime(System.currentTimeMillis());
		fieldOption.setUpdateUser(currentUserId);
		return fieldOption;
	}
}
