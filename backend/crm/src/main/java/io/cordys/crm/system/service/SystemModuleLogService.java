package io.cordys.crm.system.service;

import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.form.FormProp;
import io.cordys.crm.system.dto.form.base.LinkProp;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class SystemModuleLogService extends BaseModuleLogService{

	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;

	@Override
	public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
		differenceDTOS.forEach(differ -> {
			if ("linkFields".equals(differ.getColumn())) {
				differ.setColumnName(Translator.get("log." + differ.getColumn()));
				handleLinkFieldsLogDetail(differ);
			}
		});
	}

	private void handleLinkFieldsLogDetail(JsonDifferenceDTO differ) {
		Object oldValue = differ.getOldValue();
		Object newValue = differ.getNewValue();
		Map<String, String> oldLinkFieldMap = new HashMap<>();
		if (oldValue != null && oldValue instanceof List linkFields) {
			linkFields.forEach(linkField -> {
				Map linkMap = (Map) linkField;
				oldLinkFieldMap.put(linkMap.get("current").toString(), linkMap.get("link").toString());
			});
		}
		Map<String, String> newLinkFieldMap = new HashMap<>();
		if (newValue != null && newValue instanceof List linkFields) {
			linkFields.forEach(linkField -> {
				Map linkMap = (Map) linkField;
				newLinkFieldMap.put(linkMap.get("current").toString(), linkMap.get("link").toString());
			});
		}
		List<String> fieldIds = new ArrayList<>();
		oldLinkFieldMap.forEach((key, value) -> {
			fieldIds.add(key);
			fieldIds.add(value);
		});
		newLinkFieldMap.forEach((key, value) -> {
			fieldIds.add(key);
			fieldIds.add(value);
		});
		LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(ModuleField::getId, fieldIds);
		List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
		Map<String, String> fieldMap = moduleFields.stream().collect(Collectors.toMap(ModuleField::getId, ModuleField::getName, (v1, v2) -> v1));
		if (!oldLinkFieldMap.isEmpty()) {
			List<String> oldVal = new ArrayList<>();
			oldLinkFieldMap.forEach((k, v) -> {
				oldVal.add(fieldMap.getOrDefault(k, k) + "-" + fieldMap.getOrDefault(v, v));
			});
			differ.setOldValueName(oldVal);
		}
		if (!newLinkFieldMap.isEmpty()) {
			List<String> newVal = new ArrayList<>();
			newLinkFieldMap.forEach((k, v) -> {
				newVal.add(fieldMap.getOrDefault(k, k) + "-" + fieldMap.getOrDefault(v, v));
			});
			differ.setNewValueName(newVal);
		}
	}
}
