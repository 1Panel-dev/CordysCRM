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
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.opportunity.service.OpportunityFieldService;
import io.cordys.crm.search.response.global.GlobalOpportunityResponse;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
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


    @Override
    public PagerWithOption<List<GlobalOpportunityResponse>> startSearch(BasePageRequest basePageRequest, String orgId, String userId) {
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        //1. 获取查询关键字
        String keyword =basePageRequest.getKeyword();
        //2. 查询客户源数据
        List<OptionDTO> customerOptions = extCustomerMapper.getCustomerOptions(keyword, orgId);
        //3. 商机默认查询属性
        List<FilterCondition> conditions = new ArrayList<>();
        FilterCondition nameCondition = getFilterCondition("name",keyword,FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
        conditions.add(nameCondition);
        List<String> list = customerOptions.stream()
                .map(OptionDTO::getId)
                .toList();
        FilterCondition customerCondition = getFilterCondition("customerId",list,FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
        conditions.add(customerCondition);
        //4. TODO: 查询当前用户搜索配置
        //5. 构造查询参数
        CombineSearch combineSearch = new CombineSearch();
        combineSearch.setSearchMode(CombineSearch.SearchMode.OR.toString());
        combineSearch.setConditions(conditions);
        basePageRequest.setCombineSearch(combineSearch);
        basePageRequest.setKeyword(null);
        // 查询重复商机列表
        Page<Object> page = PageHelper.startPage(basePageRequest.getCurrent(), basePageRequest.getPageSize());
        List<GlobalOpportunityResponse> globalOpportunityResponses = extOpportunityMapper.globalSearchList(basePageRequest, orgId);
        if (CollectionUtils.isEmpty(globalOpportunityResponses)) {
            return PageUtils.setPageInfoWithOption(page, null, null);
        }
        //TODO： 获取系统设置的脱敏字段
        List<GlobalOpportunityResponse> buildList = buildListData(globalOpportunityResponses, orgId, userId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, globalOpportunityResponses, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);

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
                    //TODO:根据当前用户的设置字段与系统设置的脱敏字段比较，进行脱敏处理，如果是数据源不处理
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
