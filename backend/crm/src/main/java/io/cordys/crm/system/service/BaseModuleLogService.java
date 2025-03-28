package io.cordys.crm.system.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.LogColumn;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseModuleLogService {

    abstract public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId);

    protected List<ModuleField> getModuleFieldList(String orgId, List<String> formKeys) {
        ExtModuleFieldMapper extModuleFieldMapper = CommonBeanFactory.getBean(ExtModuleFieldMapper.class);
        List<ModuleField> moduleFieldList = extModuleFieldMapper.getModuleField(orgId, formKeys);
        return moduleFieldList;
    }


    protected Map<String, List<OptionDTO>> getModuleOptionMap(String orgId, String formKey, List<BaseModuleFieldValue> fields) {
        ModuleFormConfigDTO customerFormConfig = CommonBeanFactory.getBean(ModuleFormCacheService.class).getBusinessFormConfig(formKey, orgId);
        Map<String, List<OptionDTO>> optionMap = CommonBeanFactory.getBean(ModuleFormService.class).getOptionMap(customerFormConfig, fields);
        return optionMap;
    }

    protected Map<String, String> getOwnerMap(String orgId) {
        List<OptionDTO> userOptions = CommonBeanFactory.getBean(OrganizationUserService.class).getUserOptions(orgId);
        Map<String, String> ownerMap = userOptions.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
        return ownerMap;
    }

    protected void handleModuleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId, String formKey) {
        List<ModuleField> moduleFieldList = getModuleFieldList(orgId, List.of(formKey));
        Map<String, ModuleField> moduleFieldMap = moduleFieldList.stream().collect(Collectors.toMap(ModuleField::getId, Function.identity()));
        List<BaseModuleFieldValue> fields = new ArrayList<>();
        differenceDTOS.forEach(differ -> {
            if (moduleFieldMap.containsKey(differ.getColumn())) {
                BaseModuleFieldValue field = new BaseModuleFieldValue();
                field.setFieldId(differ.getColumn());
                field.setFieldValue(differ.getOldValue());
                fields.add(field);
            }
        });

        Map<String, List<OptionDTO>> optionMap = getModuleOptionMap(orgId, formKey, fields);
        Map<String, String> ownerMap = getOwnerMap(orgId);

        differenceDTOS.forEach(differ -> {
            if (moduleFieldMap.containsKey(differ.getColumn())) {
                differ.replace(differ.getOldValue(), differ.getNewValue());
                ModuleField moduleField = moduleFieldMap.get(differ.getColumn());
                differ.setColumnName(moduleField.getName());
                Optional.ofNullable(optionMap.get(differ.getColumn())).ifPresentOrElse(option -> {
                    differ.setOldValueName(optionMap.get(differ.getColumn()).get(Integer.parseInt(differ.getOldValue())).getName());
                }, () -> {
                    handleFields(differ, ownerMap);
                });

                Optional.ofNullable(optionMap.get(differ.getColumn())).ifPresentOrElse(option -> {
                    differ.setNewValueName(optionMap.get(differ.getColumn()).get(Integer.parseInt(differ.getNewValue())).getName());
                }, () -> {
                    handleFields(differ, ownerMap);
                });
            } else {
                //主表字段
                differ.setColumnName(Translator.get("log_" + differ.getColumn()));
                differ.setOldValueName(JSON.parseObject(differ.getOldValue()));
                differ.setNewValueName(JSON.parseObject(differ.getNewValue()));

            }
        });

    }

    protected void handleFields(JsonDifferenceDTO differ, Map<String, String> ownerMap) {
        switch (differ.getColumn()) {
            case LogColumn.owner:
                differ.setOldValueName(ownerMap.get(differ.getOldValue()));
                differ.setNewValueName(ownerMap.get(differ.getNewValue()));
                break;
            case LogColumn.contact:
                //todo
                break;
            default:
                differ.setOldValueName(JSON.parseObject(differ.getOldValue()));
                differ.setNewValueName(JSON.parseObject(differ.getNewValue()));
                break;
        }
    }
}