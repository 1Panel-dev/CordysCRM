package cn.cordys.crm.system.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.*;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseModuleLogService {

    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;
    @Resource
    private BaseMapper<Product> productMapper;

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

    abstract public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId);

    /**
     * 处理非业务字段的自定义字段
     * 同时会翻译其他字段的 ColumnName
     *
     * @param differences
     * @param orgId
     * @param formKey
     */
    protected List<JsonDifferenceDTO> handleModuleLogField(List<JsonDifferenceDTO> differences, String orgId, String formKey) {
        ModuleFormConfigDTO customerFormConfig = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormCacheService.class))
                .getBusinessFormConfig(formKey, orgId);

        // 模块字段 map
        Map<String, BaseField> moduleFieldMap = customerFormConfig.getFields()
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        Map<String, BaseField> subFieldMap = new HashMap<>();
        for (BaseField field : customerFormConfig.getFields()) {
            if (field instanceof SubField) {
                subFieldMap.put(field.getBusinessKey(), field);
            }
        }

        List<JsonDifferenceDTO> modifiable = new ArrayList<>(differences);
        modifiable.removeIf(differ -> {
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            return moduleField != null && Strings.CI.equals(moduleField.getType(), FieldType.SERIAL_NUMBER.name());
        });
        differences = modifiable;
        // 记录选项字段的字段值
        List<BaseModuleFieldValue> optionFieldValues = new ArrayList<>();
        // 记录子表选项字段的字段值
        List<BaseModuleFieldValue> optionSubFieldValues = new ArrayList<>();

        differences.forEach(differ -> {
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            if (moduleField != null && moduleField.hasSingleOptions()) {
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
            subFieldMap.forEach((key, value) -> {
                if (value.isSubField()) {
                    if (value instanceof SubField) {
                        // 子表字段
                        for (BaseField subField : ((SubField) value).getSubFields()) {
                            String subKey = StringUtils.isNotBlank(subField.getBusinessKey()) ? subField.getBusinessKey() : subField.getId();
                            //如果differ.getColumn() 包含"-",则截取最后一个-后面的字符串
                            String differColumn = differ.getColumn();
                            if (StringUtils.isNotBlank(differ.getColumn())) {
                                differColumn = differ.getColumn().substring(differ.getColumn().lastIndexOf("-") + 1);
                            }
                            if (Strings.CS.equals(subKey, differColumn)) {
                                if (differ.getOldValue() != null) {
                                    BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
                                    fieldValue.setFieldId(subKey);
                                    fieldValue.setFieldValue(differ.getOldValue());
                                    optionSubFieldValues.add(fieldValue);
                                }
                                if (differ.getNewValue() != null) {
                                    BaseModuleFieldValue fieldValue = new BaseModuleFieldValue();
                                    fieldValue.setFieldId(subKey);
                                    fieldValue.setFieldValue(differ.getNewValue());
                                    optionSubFieldValues.add(fieldValue);
                                }
                            }
                        }

                    }
                }
            });
        });

        Map<String, List<OptionDTO>> optionMap = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getOptionMap(customerFormConfig, optionFieldValues);
        // 子表选项字段的选项值 map
        Map<String, List<OptionDTO>> subOptionMap = Objects.requireNonNull(CommonBeanFactory.getBean(ModuleFormService.class))
                .getOptionMap(customerFormConfig, optionSubFieldValues);

        Set<String> subFieldIds = optionSubFieldValues.stream().map(BaseModuleFieldValue::getFieldId).collect(Collectors.toSet());
        differences.forEach(differ -> {
            String originDifferColumn = differ.getColumn();
            String differColumn = differ.getColumn();
            String prefix = differ.getColumn();
            // 子表字段处理
            if (StringUtils.isNotBlank(differ.getColumn()) && differ.getColumn().contains("-")) {
                differColumn = originDifferColumn.substring(differ.getColumn().lastIndexOf("-") + 1);
                prefix = originDifferColumn.substring(0, originDifferColumn.lastIndexOf("-"));
            }
            BaseField moduleField = moduleFieldMap.get(differ.getColumn());
            if (moduleField != null) {
                differ.setColumnName(moduleField.getName());
                // 设置字段值名称
                setColumnValueName(optionMap.get(differ.getColumn()), differ, moduleField);
            } else {
                if (optionMap.containsKey(differ.getColumn())) {
                    differ.setColumnName(Translator.get("log." + differ.getColumn()));
                    // 设置字段值名称
                    setColumnValueName(optionMap.get(differColumn), differ, moduleField);
                } else if (subFieldIds.contains(differColumn)) {
                    // 子表字段处理
                    if (subOptionMap.containsKey(differColumn)) {
                        differ.setColumn(prefix);
                        String finalDifferColumn = differColumn;
                        subFieldMap.forEach((key, value) -> {
                            if (value.isSubField()) {
                                if (value instanceof SubField) {
                                    // 子表字段
                                    for (BaseField subField : ((SubField) value).getSubFields()) {
                                        String subKey = StringUtils.isNotBlank(subField.getBusinessKey()) ? subField.getBusinessKey() : subField.getId();
                                        if (Strings.CS.equals(subKey, finalDifferColumn)) {
                                            // 设置字段值名称
                                            setColumnValueName(subOptionMap.get(finalDifferColumn), differ, subField);
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        //如果prefix = differ.column，则说明没有-符号，设置为Translator.get("log."+differ.getColumn()),否则设置为prefix
                        if (Strings.CS.equals(prefix, differ.getColumn())) {
                            differ.setColumnName(Translator.get("log." + differ.getColumn()));
                        } else {
                            differ.setColumn(prefix);
                        }
                        differ.setOldValueName(differ.getOldValue());
                        differ.setNewValueName(differ.getNewValue());
                    }
                } else {
                    translatorDifferInfo(differ);
                }
            }

        });

        return differences;
    }

    private void setColumnValueName(List<OptionDTO> options, JsonDifferenceDTO differ, BaseField moduleField) {
        if (options == null) {
            //解析各种数据
            parseValue(moduleField, differ);
            return;
        }
        List<String> oldNameList = new ArrayList<>();
        List<String> newNameList = new ArrayList<>();
        for (OptionDTO option : options) {
            if (differ.getOldValue() instanceof String strValue && Strings.CS.equals(option.getId(), strValue)) {
                // 设置旧值名称
                differ.setOldValueName(option.getName());
            }
            if (differ.getNewValue() instanceof String strValue && Strings.CS.equals(option.getId(), strValue)) {
                // 设置新值名称
                differ.setNewValueName(option.getName());
            }
            if (differ.getOldValue() instanceof List<?> oldValueList) {
                for (Object oldValue : oldValueList) {
                    if (oldValue instanceof String strValue && Strings.CS.equals(option.getId(), strValue)) {
                        // 设置旧值名称
                        oldNameList.add(option.getName());
                    }
                }

            }
            if (differ.getNewValue() instanceof List<?> newValueList) {

                for (Object newValue : newValueList) {
                    if (newValue instanceof String strValue && Strings.CS.equals(option.getId(), strValue)) {
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
            if (Strings.CI.equalsAny(moduleField.getType(), FieldType.DATE_TIME.name())) {
                // 日期时间类型
                setFormatDataTimeFieldValueName(differ);
            } else if (moduleField instanceof DatasourceMultipleField || moduleField instanceof DatasourceField) {
                String dataSourceType = moduleField instanceof DatasourceMultipleField ?
                        ((DatasourceMultipleField) moduleField).getDataSourceType() :
                        ((DatasourceField) moduleField).getDataSourceType();
                if (Strings.CS.equals(dataSourceType, FieldSourceType.CONTACT.name())) {
                    dataSourceType = "customer_contact";
                }
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
        if (differ.getNewValue() != null) {
            differ.setNewValueName(formatDataTime(differ.getNewValue().toString()));
        }
    }

    private String formatDataTime(String value) {
        if (StringUtils.isBlank(value) || Strings.CI.equals(value, "null")) {
            return StringUtils.EMPTY;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(value));
    }

    private void setResourceValueName(JsonDifferenceDTO differ, String tableName) {
        if (differ.getOldValue() != null) {
            List<OptionDTO> oldOptions = extModuleFieldMapper.getSourceOptionsByIds(tableName, JSON.parseArray(differ.getOldValue().toString(), String.class));
            differ.setOldValueName(oldOptions.stream().map(OptionDTO::getName).collect(Collectors.joining(",")));
        }
        if (differ.getNewValue() != null) {
            List<OptionDTO> newOptions = extModuleFieldMapper.getSourceOptionsByIds(tableName, JSON.parseArray(differ.getNewValue().toString(), String.class));
            differ.setNewValueName(newOptions.stream().map(OptionDTO::getName).collect(Collectors.joining(",")));
        }
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
     * 线索名称
     *
     * @param differ
     */
    protected void setClueName(JsonDifferenceDTO differ) {
        ClueService clueService = CommonBeanFactory.getBean(ClueService.class);
        assert clueService != null;
        if (differ.getOldValue() != null) {
            String customerName = clueService.getClueName(differ.getOldValue().toString());
            differ.setOldValueName(customerName);
        }
        if (differ.getNewValue() != null) {
            String userName = clueService.getClueName(differ.getNewValue().toString());
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