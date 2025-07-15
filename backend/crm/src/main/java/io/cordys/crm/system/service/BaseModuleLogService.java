package io.cordys.crm.system.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.field.*;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtModuleFieldMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseModuleLogService {

    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private BaseMapper<Product> productMapper;

    abstract public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId);

    /**
     * 处理非业务字段的自定义字段
     * 同时会翻译其他字段的 ColumnName
     *
     * @param differenceDTOS
     * @param orgId
     * @param formKey
     */
    protected void handleModuleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId, String formKey) {
        ModuleFormConfigDTO customerFormConfig = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormCacheService.class))
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

        Map<String, List<OptionDTO>> optionMap = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getOptionMap(customerFormConfig, optionFieldValues);

        differenceDTOS.forEach(differ -> {
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            if (moduleField != null) {
                differ.setColumnName(moduleField.getName());
                // 设置字段值名称
                setColumnValueName(optionMap, differ, moduleField);
            } else {
                if (optionMap.containsKey(differ.getColumn())) {
                    differ.setColumnName(Translator.get("log." + differ.getColumn()));
                    // 设置字段值名称
                    setColumnValueName(optionMap, differ, moduleField);
                } else {
                    translatorDifferInfo(differ);
                }

            }
        });
    }

    /**
     * 翻译字段名称
     * 赋值旧值名称和新值名称
     *
     * @param differ
     */
    public static void translatorDifferInfo(JsonDifferenceDTO differ) {
        //主表字段
        differ.setColumnName(Translator.get("log." + differ.getColumn()));
        differ.setOldValueName(differ.getOldValue());
        differ.setNewValueName(differ.getNewValue());
    }

    private void setColumnValueName(Map<String, List<OptionDTO>> optionMap, JsonDifferenceDTO differ, BaseField moduleField) {
        List<OptionDTO> options = optionMap.get(differ.getColumn());
        if (options == null) {
            //解析各种数据
            parseValue(moduleField, differ);
            return;
        }
        List<String> oldNameList = new ArrayList<>();
        List<String> newNameList = new ArrayList<>();
        for (OptionDTO option : options) {
            if (differ.getOldValue() instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                // 设置旧值名称
                differ.setOldValueName(option.getName());
            }
            if (differ.getNewValue() instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                // 设置新值名称
                differ.setNewValueName(option.getName());
            }
            if (differ.getOldValue() instanceof List<?> oldValueList) {
                for (Object oldValue : oldValueList) {
                    if (oldValue instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                        // 设置旧值名称
                        oldNameList.add(option.getName());
                    }
                }

            }
            if (differ.getNewValue() instanceof List<?> newValueList) {

                for (Object newValue : newValueList) {
                    if (newValue instanceof String strValue && StringUtils.equals(option.getId(), strValue)) {
                        // 设置新值名称
                        newNameList.add(option.getName());
                    }
                }
            }

        }
        if (!oldNameList.isEmpty()) {
            differ.setOldValueName(String.join(",", oldNameList));
        }
        if (!newNameList.isEmpty()) {
            differ.setNewValueName(String.join(",", newNameList));
        }

    }

    private void parseValue(BaseField moduleField, JsonDifferenceDTO differ) {
        if (moduleField != null) {
            if (StringUtils.equalsAnyIgnoreCase(moduleField.getType(), FieldType.DATE_TIME.name())) {
                // 日期时间类型
                setFormatDataTimeFieldValueName(differ);
            } else if (moduleField instanceof DatasourceMultipleField || moduleField instanceof DatasourceField) {
                String dataSourceType = moduleField instanceof DatasourceMultipleField ?
                        ((DatasourceMultipleField) moduleField).getDataSourceType() :
                        ((DatasourceField) moduleField).getDataSourceType();
                setResourceValueName(differ, dataSourceType);
            } else if (moduleField instanceof MemberMultipleField || moduleField instanceof MemberField) {
                setResourceValueName(differ, "sys_user");
            } else if (moduleField instanceof DepartmentMultipleField || moduleField instanceof DepartmentField) {
                setResourceValueName(differ, "sys_department");
            } else {
                differ.setOldValueName(differ.getOldValue());
                differ.setNewValueName(differ.getNewValue());
            }

        } else {
            differ.setOldValueName(differ.getOldValue());
            differ.setNewValueName(differ.getNewValue());
        }
    }

    public void setFormatDataTimeFieldValueName(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            differ.setOldValueName(formatDataTime(differ.getOldValue().toString()));
        }
        if (differ.getOldValue() != null) {
            differ.setNewValueName(formatDataTime(differ.getNewValue().toString()));
        }
    }

    private String formatDataTime(String value) {
        if(StringUtils.isBlank(value)||StringUtils.equalsIgnoreCase(value, "null")){
            return StringUtils.EMPTY;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(value));
    }

    private JsonDifferenceDTO setResourceValueName(JsonDifferenceDTO differ, String tableName) {
        if (differ.getOldValue() != null) {
            List<OptionDTO> oldOptions = extModuleFieldMapper.getSourceOptionsByIds(tableName, JSON.parseArray(differ.getOldValue().toString(), String.class));
            differ.setOldValueName(oldOptions.stream().map(OptionDTO::getName).collect(Collectors.joining(",")));
        }
        if (differ.getNewValue() != null) {
            List<OptionDTO> newOptions = extModuleFieldMapper.getSourceOptionsByIds(tableName, JSON.parseArray(differ.getNewValue().toString(), String.class));
            differ.setNewValueName(newOptions.stream().map(OptionDTO::getName).collect(Collectors.joining(",")));
        }
        return differ;
    }

    protected void setUserFieldName(JsonDifferenceDTO differ) {
        BaseService baseService = CommonBeanFactory.getBean(BaseService.class);
        assert baseService != null;
        if (differ.getOldValue() != null) {
            String userName = baseService.getUserName(differ.getOldValue().toString());
            differ.setOldValueName(userName);
        }
        if (differ.getNewValue() != null) {
            String userName = baseService.getUserName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }


    /**
     * 客户名称
     *
     * @param differ
     */
    protected void setCustomerName(JsonDifferenceDTO differ) {
        CustomerService customerService = CommonBeanFactory.getBean(CustomerService.class);
        assert customerService != null;
        if (differ.getOldValue() != null) {
            String customerName = customerService.getCustomerName(differ.getOldValue().toString());
            differ.setOldValueName(customerName);
        }
        if (differ.getNewValue() != null) {
            String userName = customerService.getCustomerName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }

    /**
     * 商机名称
     *
     * @param differ
     */
    protected void setOpportunityName(JsonDifferenceDTO differ) {
        OpportunityService opportunityService = CommonBeanFactory.getBean(OpportunityService.class);
        assert opportunityService != null;
        if (differ.getOldValue() != null) {
            String customerName = opportunityService.getOpportunityName(differ.getOldValue().toString());
            differ.setOldValueName(customerName);
        }
        if (differ.getNewValue() != null) {
            String userName = opportunityService.getOpportunityName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }


    /**
     * 联系人
     *
     * @param differ
     */
    protected void setContactFieldName(JsonDifferenceDTO differ) {
        CustomerContactService customerContactService = CommonBeanFactory.getBean(CustomerContactService.class);
        assert customerContactService != null;
        if (differ.getOldValue() != null) {
            String customerName = customerContactService.getContactName(differ.getOldValue().toString());
            differ.setOldValueName(customerName);
        }
        if (differ.getNewValue() != null) {
            String userName = customerContactService.getContactName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }


    /**
     * 产品
     *
     * @param differ
     */
    protected void setProductName(JsonDifferenceDTO differ) {
        Optional.ofNullable(differ.getOldValue()).ifPresent(oldValue -> {
            List<String> ids = ((Collection<?>) oldValue).stream()
                    .map(String::valueOf)
                    .toList();
            List<Product> products = productMapper.selectByIds(ids);
            differ.setOldValueName(products.stream().map(Product::getName).toList());
        });

        Optional.ofNullable(differ.getNewValue()).ifPresent(newValue -> {
            List<String> ids = ((Collection<?>) newValue).stream()
                    .map(String::valueOf)
                    .toList();
            List<Product> products = productMapper.selectByIds(ids);
            differ.setNewValueName(products.stream().map(Product::getName).toList());
        });
    }
}