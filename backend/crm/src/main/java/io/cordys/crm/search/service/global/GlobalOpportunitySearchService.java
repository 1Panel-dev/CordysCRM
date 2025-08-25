package io.cordys.crm.search.service.global;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.service.ClueService;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.CustomerContactService;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.opportunity.service.OpportunityFieldService;
import io.cordys.crm.search.constants.SearchModuleEnum;
import io.cordys.crm.search.domain.UserSearchConfig;
import io.cordys.crm.search.response.global.GlobalOpportunityResponse;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.ProductService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GlobalOpportunitySearchService extends BaseSearchService<BasePageRequest, GlobalOpportunityResponse> {
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private BaseMapper<UserSearchConfig>userSearchConfigBaseMapper;
    private static final ClueService clueService;
    private static final CustomerContactService contactService;
    private static final ProductService productService;

    static {
        clueService = CommonBeanFactory.getBean(ClueService.class);
        contactService = CommonBeanFactory.getBean(CustomerContactService.class);
        productService = CommonBeanFactory.getBean(ProductService.class);
    }

    @Override
    public Pager<List<GlobalOpportunityResponse>> startSearchNoOption(BasePageRequest request, String orgId, String userId) {
        //1. 获取查询关键字
        String keyword =request.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfo(page, null);
        }
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }
        //2. 查询客户源数据
        List<OptionDTO> customerOptions = extCustomerMapper.getCustomerOptions(keyword, orgId);
        //3. 商机默认查询属性
        List<FilterCondition> conditions = new ArrayList<>();
        FilterCondition nameCondition = getFilterCondition("name",keyword,FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
        conditions.add(nameCondition);
        List<String> list = customerOptions.stream()
                .map(OptionDTO::getId)
                .toList();
        if (CollectionUtils.isNotEmpty(list)) {
            FilterCondition customerCondition = getFilterCondition("customerId",list,FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
            conditions.add(customerCondition);
        }
        //4.查询当前用户搜索配置
        LambdaQueryWrapper<UserSearchConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSearchConfig::getUserId, userId)
                .eq(UserSearchConfig::getModuleType, SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY);
        List<UserSearchConfig> userSearchConfigs = userSearchConfigBaseMapper.selectListByLambda(wrapper);
        for (UserSearchConfig userSearchConfig : userSearchConfigs) {
            //TODO：如果和固定列重名，过滤

            // 如果不是数据源类型的字段，直接添加到查询条件中
            if (StringUtils.isBlank(userSearchConfig.getDataSourceType()) && !Strings.CI.equals(userSearchConfig.getType(),FieldType.PHONE.toString())) {
                FilterCondition filterCondition = getFilterCondition(userSearchConfig.getFieldId(),keyword,FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
                conditions.add(filterCondition);
            }
            // 如果是PHONE类型的字段，使用精确查询
            if (Strings.CI.equals(userSearchConfig.getType(),FieldType.PHONE.toString())) {
                FilterCondition filterCondition = getFilterCondition(userSearchConfig.getFieldId(),keyword,FilterCondition.CombineConditionOperator.EQUALS.toString(), FieldType.PHONE.toString());
                conditions.add(filterCondition);
            }
            // 如果是数据源类型的字段，使用IN查询
            if (Strings.CI.equals(userSearchConfig.getType(), FieldType.DATA_SOURCE.toString())) {
                List<String>ids = new ArrayList<>();
                if (Strings.CI.equals(userSearchConfig.getDataSourceType(), "CUSTOMER")) {
                    // 如果是客户数据源，使用客户ID查询
                    if (CollectionUtils.isEmpty(list)) {
                        continue; // 如果没有匹配的客户ID，跳过
                    }
                    ids.addAll(list);
                } else if (Strings.CI.equals(userSearchConfig.getDataSourceType(), "OPPORTUNITY")) {
                    // 如果是商机数据源，使用商机ID查询
                    List<OptionDTO> opportunitys = extOpportunityMapper.getOpportunityOptions(keyword, orgId);
                    if (CollectionUtils.isEmpty(opportunitys)) {
                        continue; // 如果没有匹配的商机ID，跳过
                    }
                    List<String> opportunityIds = opportunitys.stream().map(OptionDTO::getId)
                            .toList();
                    ids.addAll(opportunityIds);
                } else if (Strings.CI.equals(userSearchConfig.getDataSourceType(), "CLUE")) {
                    // 如果是线索数据源，使用线索ID查询
                    List<String> clueIds = clueService.getClueListByNames(List.of(keyword)).stream()
                            .map(Clue::getId)
                            .toList();
                    if (CollectionUtils.isEmpty(clueIds)) {
                        continue; // 如果没有匹配的线索ID，跳过
                    }
                    ids.addAll(clueIds);
                }
                else if (Strings.CI.equals(userSearchConfig.getDataSourceType(), "CONTACT")) {
                    // 如果是线索数据源，使用线索ID查询
                    List<String> clueIds = contactService.getContactListByNames(List.of(keyword)).stream()
                            .map(CustomerContact::getId)
                            .toList();
                    if (CollectionUtils.isEmpty(clueIds)) {
                        continue; // 如果没有匹配的线索ID，跳过
                    }
                    ids.addAll(clueIds);
                }
                else if (Strings.CI.equals(userSearchConfig.getDataSourceType(), "PRODUCT")) {
                    // 如果是线索数据源，使用线索ID查询
                    List<String> productIds = productService.getProductListByNames(List.of(keyword)).stream()
                            .map(Product::getId)
                            .toList();
                    if (CollectionUtils.isEmpty(productIds)) {
                        continue; // 如果没有匹配的线索ID，跳过
                    }
                    ids.addAll(productIds);
                }
                FilterCondition filterCondition = getFilterCondition(userSearchConfig.getFieldId(),ids,FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
                conditions.add(filterCondition);
            }
        }
        //5. 构造查询参数
        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setSearchMode(CombineSearch.SearchMode.OR.toString());
        combineSearch.setConditions(conditions);
        request.setCombineSearch(combineSearch);
        request.setKeyword(null);
        // 查询重复商机列表
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<GlobalOpportunityResponse> globalOpportunityResponses = extOpportunityMapper.globalSearchList(request, orgId);
        if (CollectionUtils.isEmpty(globalOpportunityResponses)) {
            return PageUtils.setPageInfo(page, null);
        }
        //TODO： 获取系统设置的脱敏字段
        List<GlobalOpportunityResponse> buildList = buildListData(globalOpportunityResponses, orgId, userId);
        return PageUtils.setPageInfo(page, buildList);
    }

    public List<GlobalOpportunityResponse> buildListData(List<GlobalOpportunityResponse> list, String orgId, String userId) {
        List<String> opportunityIds = list.stream().map(GlobalOpportunityResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> opportunityFiledMap = opportunityFieldService.getResourceFieldMap(opportunityIds, true);

        List<String> ownerIds = list.stream()
                .map(GlobalOpportunityResponse::getOwner)
                .distinct()
                .toList();

        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);


        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, opportunityListResponse.getOwner(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);
            List<BaseModuleFieldValue> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());


            if (!hasPermission) {
                for (BaseModuleFieldValue opportunityField : opportunityFields) {
                    //如果没权限设置脱敏字段
                    //TODO:根据当前用户的设置字段与系统设置的脱敏字段比较，进行脱敏处理，如果是数据源，查出数据源字段，再脱敏
                }
                opportunityListResponse.setModuleFields(new ArrayList<>());
            } else {
                opportunityListResponse.setModuleFields(opportunityFields);
            }

            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));

            UserDeptDTO userDeptDTO = userDeptMap.get(opportunityListResponse.getOwner());
            if (userDeptDTO != null) {
                opportunityListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            opportunityListResponse.setHasPermission(hasPermission);
        });
        return list;
    }


    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<GlobalOpportunityResponse> list, List<GlobalOpportunityResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, GlobalOpportunityResponse::getModuleFields);
        //TODO:根据当前用户的设置字段与系统设置的脱敏字段比较，进行脱敏处理（这里是指处理数据源）
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                GlobalOpportunityResponse::getOwner, GlobalOpportunityResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);


        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);

        return optionMap;

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
    @NotNull
    private FilterCondition getFilterCondition(String name, Object value, String operator, String type) {
        FilterCondition nameCondition = new FilterCondition();
        nameCondition.setName(name);
        nameCondition.setValue(value);
        nameCondition.setOperator(operator);
        nameCondition.setMultipleValue(false);
        nameCondition.setType(type);
        return nameCondition;
    }
}
