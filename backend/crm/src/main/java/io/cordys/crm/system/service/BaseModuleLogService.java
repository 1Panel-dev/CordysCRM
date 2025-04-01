package io.cordys.crm.system.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseModuleLogService {

    abstract public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId);

    /**
     * 处理非业务字段的自定义字段
     * 同时会翻译其他字段的 ColumnName
     * @param differenceDTOS
     * @param orgId
     * @param formKey
     */
    protected void handleModuleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId, String formKey) {
        ModuleFormConfigDTO customerFormConfig = CommonBeanFactory.getBean(ModuleFormCacheService.class)
                .getBusinessFormConfig(formKey, orgId);

        // 模块字段 map
        Map<String, BaseField> moduleFieldMap = customerFormConfig.getFields()
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        // 记录选项字段的字段值
        List<BaseModuleFieldValue> optionFieldValues = new ArrayList<>();
        differenceDTOS.forEach(differ -> {
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            if (moduleField != null && moduleField.hasOptions()) {
                if (differ.getOldValue() != null) {
                    BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
                    fieldValue.setFieldId(differ.getColumn());
                    fieldValue.setFieldValue(differ.getOldValue());
                    optionFieldValues.add(fieldValue);
                }
                if (differ.getNewValue() != null) {
                    BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
                    fieldValue.setFieldId(differ.getColumn());
                    fieldValue.setFieldValue(differ.getNewValue());
                    optionFieldValues.add(fieldValue);
                }
            }
        });

        Map<String, List<OptionDTO>> optionMap = CommonBeanFactory.getBean(ModuleFormService.class)
                .getOptionMap(customerFormConfig, optionFieldValues);

        differenceDTOS.forEach(differ -> {
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            if (moduleField != null) {
                differ.setColumnName(moduleField.getName());
                // 设置字段值名称
                setColumnValueName(optionMap, differ);
            } else {
                translatorDifferInfo(differ);
            }
        });
    }

    /**
     * 翻译字段名称
     * 赋值旧值名称和新值名称
     * @param differ
     */
    public static void translatorDifferInfo(JsonDifferenceDTO differ) {
        //主表字段
        differ.setColumnName(Translator.get("log." + differ.getColumn()));
        differ.setOldValueName(differ.getOldValue());
        differ.setNewValueName(differ.getNewValue());
    }

    private void setColumnValueName(Map<String, List<OptionDTO>> optionMap, JsonDifferenceDTO differ) {
        List<OptionDTO> options = optionMap.get(differ.getColumn());
        if (options == null) {
            differ.setOldValueName(differ.getOldValue());
            differ.setNewValueName(differ.getNewValue());
            return;
        }

        for (OptionDTO option : options) {
            if (differ.getOldValue() instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                // 设置旧值名称
                differ.setOldValueName(option.getName());
            }
            if (differ.getNewValue() instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                // 设置新值名称
                differ.setNewValueName(option.getName());
            }
        }
    }

    protected void setUserFieldName(JsonDifferenceDTO differ) {
        BaseService baseService = CommonBeanFactory.getBean(BaseService.class);
        if (differ.getOldValue() != null) {
            String userName = baseService.getUserName(differ.getOldValue().toString());
            differ.setOldValueName(userName);
        }
        if (differ.getNewValue() != null) {
            String userName = baseService.getUserName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }
}