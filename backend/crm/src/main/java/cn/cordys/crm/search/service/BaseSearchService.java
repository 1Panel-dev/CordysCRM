package cn.cordys.crm.search.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.condition.CombineSearch;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.search.constants.SearchPhoneEnum;
import cn.cordys.crm.search.domain.SearchFieldMaskConfig;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.Module;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtModuleFieldMapper;
import cn.cordys.crm.system.mapper.ExtProductMapper;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseSearchService<T extends BasePageRequest, R> {

    @Resource
    private BaseMapper<Module> moduleMapper;

    @Resource
    private BaseMapper<UserSearchConfig> userSearchConfigBaseMapper;

    @Resource
    private BaseMapper<SearchFieldMaskConfig> searchFieldMaskConfigBaseMapper;

    @Resource
    private ExtProductMapper extProductMapper;

    @Resource
    private ExtCustomerMapper extCustomerMapper;

    @Resource
    private BaseMapper<CustomerPool> customerPoolMapper;

    @Resource
    private BaseMapper<CluePool> cluePoolMapper;

    @Resource
    private ExtModuleFieldMapper extModuleFieldMapper;

    @Resource
    private ModuleFormService moduleFormService;

    @Resource
    private UserExtendService userExtendService;

    public PagerWithOption<List<R>> startSearch(T request, String orgId, String userId) {
        return new PagerWithOption<>();
    }

    public Pager<List<R>> startSearchNoOption(T request, String orgId, String userId) {
        return new Pager<>();
    }

    /**
     * 获取当前组织下所有已开启的模块key
     *
     * @return List<String>EnabledModuleKeys
     */
    public List<String> getEnabledModules() {
        return moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, OrganizationContext.getOrganizationId())
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey)
                .toList();
    }

    /**
     * 获取过滤条件
     *
     * @param name     字段名称
     * @param value    字段值
     * @param operator 操作符
     * @param type     字段类型
     * @return FilterCondition
     */
    public FilterCondition getFilterCondition(String name, Object value, String operator, String type) {
        FilterCondition nameCondition = new FilterCondition();
        nameCondition.setName(name);
        nameCondition.setValue(value);
        nameCondition.setOperator(operator);
        nameCondition.setMultipleValue(Strings.CI.equals(type, FieldType.DATA_SOURCE_MULTIPLE.toString()));
        nameCondition.setType(type);
        return nameCondition;
    }


    /**
     * 获取产品ID和名称的映射关系
     *
     * @param orgId 组织ID
     * @return 产品ID和名称的映射关系
     */
    public Map<String, String> getProductNameMap(String orgId) {
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        return productOption.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    /**
     * 通过关键字和组织ID获取客户ID列表
     *
     * @param keyword 关键字
     * @param orgId   组织ID
     * @return 客户ID列表
     */
    public List<String> getCustomerIds(String keyword, String orgId) {
        List<OptionDTO> customerOptions = extCustomerMapper.getCustomerOptions(keyword, orgId);
        if (CollectionUtils.isEmpty(customerOptions)) {
            return new ArrayList<>();
        }
        return customerOptions.stream()
                .map(OptionDTO::getId)
                .toList();
    }

    /**
     * 获取用户有权限的公海池
     *
     * @param orgId  组织ID
     * @param userId 用户ID
     * @return 公海池ID和名称的映射关系
     */
    public Map<String, String> getUserCustomerPool(String orgId, String userId) {
        Map<String, String> poolMap = new HashMap<>();
        LambdaQueryWrapper<CustomerPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getOrganizationId, orgId);
        poolWrapper.orderByDesc(CustomerPool::getUpdateTime);
        List<CustomerPool> pools = customerPoolMapper.selectListByLambda(poolWrapper);
        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), orgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), orgId);
            if (scopeIds.contains(userId) || ownerIds.contains(userId) || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
                poolMap.put(pool.getId(), pool.getName());
            }
        });
        return poolMap;
    }


    /**
     * 获取用户有权限的线索池
     *
     * @param orgId  组织ID
     * @param userId 用户ID
     * @return 线索池ID和名称的映射关系
     */
    public Map<String, String> getUserCluePool(String orgId, String userId) {
        Map<String, String> poolMap = new HashMap<>();
        LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, orgId);
        poolWrapper.orderByDesc(CluePool::getUpdateTime);
        List<CluePool> pools = cluePoolMapper.selectListByLambda(poolWrapper);
        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), orgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), orgId);
            if (scopeIds.contains(userId) || ownerIds.contains(userId) || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
                poolMap.put(pool.getId(), pool.getName());
            }
        });
        return poolMap;
    }

    /**
     * 构建其他类型的过滤条件
     *
     * @param orgId            组织ID
     * @param userSearchConfig 用户搜索配置
     * @param keyword          关键字
     * @param conditions       过滤条件列表
     */
    public void buildOtherFilterCondition(String orgId, UserSearchConfig userSearchConfig, String keyword, List<FilterCondition> conditions) {
        String name = "";
        if (StringUtils.isNotBlank(userSearchConfig.getBusinessKey())) {
            name = userSearchConfig.getBusinessKey();
        } else {
            name = userSearchConfig.getFieldId();
        }
        //如果是businessKey不为空，说明是固定列,则name 给businessKey
        // 如果不是数据源类型的字段，直接添加到查询条件中
        if (StringUtils.isBlank(userSearchConfig.getDataSourceType()) && !Strings.CI.equals(userSearchConfig.getType(), FieldType.PHONE.toString())) {
            FilterCondition filterCondition = getFilterCondition(name, keyword, FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
            conditions.add(filterCondition);
            return;
        }
        // 如果是PHONE类型的字段，使用精确查询
        if (Strings.CI.equals(userSearchConfig.getType(), FieldType.PHONE.toString())) {
            StringUtils.deleteWhitespace(keyword);
            List<String> phoneList = new ArrayList<>();
            phoneList.add(keyword);
            for (String value : SearchPhoneEnum.VALUES) {
                phoneList.add(value + keyword);
            }
            FilterCondition filterCondition = getFilterCondition(name, phoneList, FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
            conditions.add(filterCondition);
            return;
        }
        // 如果是数据源类型的字段，使用IN查询
        if (Strings.CI.equals(userSearchConfig.getType(), FieldType.DATA_SOURCE.toString()) || Strings.CI.equals(userSearchConfig.getType(), FieldType.DATA_SOURCE_MULTIPLE.toString())) {
            List<String> ids = new ArrayList<>();

            Map<String, String> sourceMap = moduleFormService.initTypeSourceMap();
            String tableName = sourceMap.get(userSearchConfig.getDataSourceType());
            if (StringUtils.isBlank(tableName)) {
                return; // 如果数据源类型不在已知范围内，直接返回
            }
            extModuleFieldMapper.getSourceOptionsByName(tableName, keyword, orgId).forEach(option -> ids.add(option.getId()));
            if (CollectionUtils.isEmpty(ids)) {
                return; // 如果没有匹配的数据，直接返回
            }
            FilterCondition filterCondition = getFilterCondition(name, ids, FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
            conditions.add(filterCondition);
        }
    }

    /**
     * 获取搜索字段脱敏配置
     *
     * @param orgId 组织ID
     * @return 搜索字段脱敏配置列表
     */
    public List<SearchFieldMaskConfig> getSearchFieldMaskConfigs(String orgId, String moduleType) {
        LambdaQueryWrapper<SearchFieldMaskConfig> sysWrapper = new LambdaQueryWrapper<>();
        sysWrapper.eq(SearchFieldMaskConfig::getModuleType, moduleType).eq(SearchFieldMaskConfig::getOrganizationId, orgId);
        return searchFieldMaskConfigBaseMapper.selectListByLambda(sysWrapper);
    }

    /**
     * 获取用户搜索配置
     *
     * @param userId 用户ID
     * @return 用户搜索配置列表
     */
    public List<UserSearchConfig> getUserSearchConfigs(String userId, String orgId) {
        LambdaQueryWrapper<UserSearchConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSearchConfig::getUserId, userId)
                .eq(UserSearchConfig::getOrganizationId, orgId);
        return userSearchConfigBaseMapper.selectListByLambda(wrapper);
    }

    /**
     * 获取自定义字段值
     *
     * @param fieldIdSet               用户配置的字段ID集合
     * @param id                       数据ID
     * @param moduleFiledMap           数据ID和自定义字段值的映射关系
     * @param moduleFormConfigDTO      模块表单配置
     * @param searchFieldMaskConfigMap 脱敏配置映射关系
     * @param hasPermission            是否有权限查看
     * @return 自定义字段值列表
     */
    public List<BaseModuleFieldValue> getBaseModuleFieldValues(Set<String> fieldIdSet, String id, Map<String, List<BaseModuleFieldValue>> moduleFiledMap, ModuleFormConfigDTO moduleFormConfigDTO, Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap, boolean hasPermission) {
        List<BaseModuleFieldValue> returnBaseModuleFieldValues = new ArrayList<>();
        //此条数据所带的自定义字段及其值
        List<BaseModuleFieldValue> baseModuleFieldValues = moduleFiledMap.get(id);
        if (CollectionUtils.isEmpty(baseModuleFieldValues)) {
            return new ArrayList<>();
        }
        Map<String, Object> fieldValueMap = baseModuleFieldValues.stream().collect(Collectors.toMap(BaseModuleFieldValue::getFieldId, BaseModuleFieldValue::getFieldValue));
        //获取自定义字段选项，用于补充数据源的值
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(moduleFormConfigDTO, baseModuleFieldValues);
        List<BaseField> fields = moduleFormConfigDTO.getFields();
        Map<String, String> fieldTypeMap = fields.stream().collect(Collectors.toMap(BaseField::getId, BaseField::getType));
        for (BaseModuleFieldValue baseModuleFieldValue : baseModuleFieldValues) {
            String fieldId = baseModuleFieldValue.getFieldId();
            if (fieldIdSet.contains(fieldId)) {
                SearchFieldMaskConfig searchFieldMaskConfig = searchFieldMaskConfigMap.get(fieldId);
                BaseModuleFieldValue returnBaseModuleFieldValue = new BaseModuleFieldValue();
                returnBaseModuleFieldValue.setFieldId(fieldId);
                Object fieldValue = fieldValueMap.get(fieldId);
                if (fieldValue instanceof String) {
                    //数据源类型的字段，进行值的转换
                    if (Strings.CI.equals(fieldTypeMap.get(fieldId), FieldType.DATA_SOURCE.toString())) {
                        List<OptionDTO> optionDTOs = optionMap.get(fieldId);
                        if (CollectionUtils.isEmpty(optionDTOs)) {
                            continue;
                        }
                        for (OptionDTO optionDTO : optionDTOs) {
                            if (Strings.CI.equals(optionDTO.getId(), (String) fieldValue)) {
                                fieldValue = optionDTO.getName();
                                break;
                            }
                        }
                    }
                    //无权限根据类型进行脱敏
                    if (!hasPermission) {
                        if (searchFieldMaskConfig != null) {
                            fieldValue = setStringFieldValue(searchFieldMaskConfig, fieldValue);
                        }
                    }
                    returnBaseModuleFieldValue.setFieldValue(fieldValue);
                } else if (fieldValue instanceof List<?>) {
                    List<String> fieldValueList = new ArrayList<>();
                    for (Object element : (List<?>) fieldValue) {
                        String s = element == null ? null : element.toString();
                        List<OptionDTO> optionDTOs = optionMap.get(fieldId);
                        if (CollectionUtils.isEmpty(optionDTOs)) {
                            continue;
                        }
                        for (OptionDTO optionDTO : optionDTOs) {
                            if (Strings.CI.equals(optionDTO.getId(), s)) {
                                String name = optionDTO.getName();
                                if (!hasPermission) {
                                    getInputFieldValue(name, name.length());
                                }
                                fieldValueList.add(name);
                            }
                        }
                    }
                    returnBaseModuleFieldValue.setFieldValue(fieldValueList);
                }
                returnBaseModuleFieldValues.add(returnBaseModuleFieldValue);
            }
        }
        return returnBaseModuleFieldValues;
    }

    /**
     * 设置字符串类型字段的脱敏值
     *
     * @param searchFieldMaskConfig 脱敏配置
     * @param fieldValue            字段值
     * @return 脱敏后的字段值
     */
    private Object setStringFieldValue(SearchFieldMaskConfig searchFieldMaskConfig, Object fieldValue) {
        int length = ((String) fieldValue).length();
        if (Strings.CI.equals(searchFieldMaskConfig.getType(), FieldType.PHONE.toString()) || Strings.CI.equals(searchFieldMaskConfig.getType(), FieldType.SERIAL_NUMBER.toString())) {
            //fieldValue后6位以*替代
            fieldValue = getPhoneFieldValue(fieldValue, length);
        } else if (Strings.CI.equals(searchFieldMaskConfig.getType(), FieldType.INPUT.toString()) || Strings.CI.equals(searchFieldMaskConfig.getType(), FieldType.DATA_SOURCE.toString())) {
            fieldValue = getInputFieldValue(fieldValue, length);
        }
        return fieldValue;
    }

    @NotNull
    public Object getPhoneFieldValue(Object fieldValue, int length) {
        if (length > 6) {
            fieldValue = ((String) fieldValue).substring(0, length - 6) + "******";
        } else {
            fieldValue = "******";
        }
        return fieldValue;
    }

    @NotNull
    public Object getInputFieldValue(Object fieldValue, int length) {
        //fieldValue保留第一位字符，后面全部用*代替
        if (length > 1) {
            fieldValue = ((String) fieldValue).charAt(0) + "*".repeat(length - 1);
        } else {
            fieldValue = "*";
        }
        return fieldValue;
    }

    /**
     * 通过产品ID列表获取产品名称列表
     *
     * @param products       产品ID列表
     * @param productNameMap 产品ID和名称的映射关系
     * @return 产品名称列表
     */
    public List<String> getProductNames(List<String> products, Map<String, String> productNameMap) {
        if (CollectionUtils.isEmpty(products)) {
            return new ArrayList<>();
        }
        List<String> productNames = new ArrayList<>();
        for (String product : products) {
            String productName = productNameMap.get(product);
            if (StringUtils.isNotBlank(productName)) {
                productNames.add(productName);
            }
        }
        return productNames;
    }

    /**
     * 构建组合查询条件
     *
     * @param conditions 过滤条件列表
     * @param request    分页请求
     */
    public void buildCombineSearch(List<FilterCondition> conditions, BasePageRequest request) {
        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setSearchMode(CombineSearch.SearchMode.OR.toString());
        combineSearch.setConditions(conditions);
        request.setCombineSearch(combineSearch);
        request.setKeyword(null);
    }

    /**
     * 构建内置字段
     *
     * @param internalKeyMap 内置字段映射关系
     * @param entity         实体对象
     * @param clazz          实体类
     * @param <A>            实体类型
     * @return 内置字段列表
     */
    public <A> List<BaseModuleFieldValue> buildInternalField(Map<String, String> internalKeyMap, Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap, boolean hasPermission, A entity,
                                                             Class<A> clazz) {
        List<BaseModuleFieldValue> returnOpportunityFields = new ArrayList<>();
        if (entity == null) {
            return returnOpportunityFields;
        }
        List<String> capitalizedList = internalKeyMap.values().stream()
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .toList();
        for (String s : capitalizedList) {
            try {
                String methodName = "get" + s;
                String value = (String) clazz.getMethod(methodName).invoke(entity);
                BaseModuleFieldValue baseModuleFieldValue = new BaseModuleFieldValue();
                baseModuleFieldValue.setFieldValue(value);
                internalKeyMap.forEach((k, v) -> {
                    if (Strings.CI.equals(v, s)) {
                        baseModuleFieldValue.setFieldId(k);
                    }
                });
                SearchFieldMaskConfig searchFieldMaskConfig = searchFieldMaskConfigMap.get(baseModuleFieldValue.getFieldId());
                if (!hasPermission && searchFieldMaskConfig != null) {
                    Object fieldValue = setStringFieldValue(searchFieldMaskConfig, value);
                    baseModuleFieldValue.setFieldValue(fieldValue);
                }
                returnOpportunityFields.add(baseModuleFieldValue);
            } catch (Exception e) {
                LogUtils.error(e);
            }
        }
        return returnOpportunityFields;
    }

}
