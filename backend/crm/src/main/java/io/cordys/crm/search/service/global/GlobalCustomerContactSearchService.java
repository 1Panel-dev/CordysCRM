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
import io.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.opportunity.service.OpportunityFieldService;
import io.cordys.crm.search.constants.SearchModuleEnum;
import io.cordys.crm.search.domain.SearchFieldMaskConfig;
import io.cordys.crm.search.domain.UserSearchConfig;
import io.cordys.crm.search.response.global.GlobalCustomerContactResponse;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormCacheService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GlobalCustomerContactSearchService extends BaseSearchService<BasePageRequest, GlobalCustomerContactResponse> {

    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ExtCustomerMapper extCustomerMapper;

    @Override
    public Pager<List<GlobalCustomerContactResponse>> startSearchNoOption(BasePageRequest request, String orgId, String userId) {
        //获取查询关键字
        String keyword = request.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfo(page, null);
        }
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }
        //查询当前用户搜索配置
        List<UserSearchConfig> userSearchConfigs = getUserSearchConfigs(userId, orgId);
        //记住当前一共有多少字段，避免固定展示列与自由选择列字段重复
        Set<String> fieldIdSet = new HashSet<>();
        List<FilterCondition> conditions = new ArrayList<>();
        //用户配置设置:
        // 1.用户没配置过，设置默认查询条件;
        // 2.用户有配置，使用用户配置的查询条件;
        // 3.用户当前模块没配置，直接返回;
        if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
            List<UserSearchConfig> contactSearchConfigs = userSearchConfigs.stream().filter(t -> Strings.CI.equals(t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CONTACT)).toList();
            if (CollectionUtils.isEmpty(contactSearchConfigs)) {
                Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
                return PageUtils.setPageInfo(page, null);
            }
            for (UserSearchConfig userSearchConfig : contactSearchConfigs) {
                //如果和固定展示列名重复不加入fieldIdSet
                if (StringUtils.isBlank(userSearchConfig.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                } else if (!Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CUSTOMER_CONTACT_PHONE.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                }
                buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
            }
        } else {
            //设置默认查询属性
            List<String> list = getCustomerIds(keyword, orgId);
            FilterCondition phoneCondition = getFilterCondition("phone", keyword, FilterCondition.CombineConditionOperator.EQUALS.toString(), FieldType.PHONE.toString());
            conditions.add(phoneCondition);
            if (CollectionUtils.isNotEmpty(list)) {
                FilterCondition customerCondition = getFilterCondition("customerId", list, FilterCondition.CombineConditionOperator.IN.toString(), FieldType.DATA_SOURCE.toString());
                conditions.add(customerCondition);
            }
        }

        //构造查询参数
        buildCombineSearch(conditions, request);
        //搜索客户
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<GlobalCustomerContactResponse> globalContactResponses = extCustomerContactMapper.globalSearchList(request, orgId);
        if (CollectionUtils.isEmpty(globalContactResponses)) {
            return PageUtils.setPageInfo(page, null);
        }
        //获取系统设置的脱敏字段
        List<SearchFieldMaskConfig> searchFieldMaskConfigs = getSearchFieldMaskConfigs(orgId,SearchModuleEnum.SEARCH_ADVANCED_CONTACT);
        List<GlobalCustomerContactResponse> buildList = buildListData(globalContactResponses, orgId, userId, searchFieldMaskConfigs, fieldIdSet);
        return PageUtils.setPageInfo(page, buildList);
    }

    private List<GlobalCustomerContactResponse> buildListData(List<GlobalCustomerContactResponse> list, String orgId, String userId, List<SearchFieldMaskConfig> searchFieldMaskConfigs, Set<String> fieldIdSet) {
        List<String> customerContactIds = list.stream().map(GlobalCustomerContactResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> customerFiledMap = opportunityFieldService.getResourceFieldMap(customerContactIds, true);

        List<String> ownerIds = list.stream()
                .map(GlobalCustomerContactResponse::getOwner)
                .distinct()
                .toList();

        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        List<String> customerIds = list.stream().map(GlobalCustomerContactResponse::getCustomerId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> customNameMap = extCustomerMapper.selectOptionByIds(customerIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap = searchFieldMaskConfigs.stream().collect(Collectors.toMap(SearchFieldMaskConfig::getFieldId, t -> t));
        list.forEach(customerContactResponse -> {
            // 判断该数据是否有权限
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, customerContactResponse.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ);
            // 处理自定义字段数据
            if (CollectionUtils.isNotEmpty(fieldIdSet)) {
                List<BaseModuleFieldValue> returnCustomerFields = getBaseModuleFieldValues(fieldIdSet, customerContactResponse.getId(), customerFiledMap, customerFormConfig, searchFieldMaskConfigMap, hasPermission);

                customerContactResponse.setModuleFields(returnCustomerFields);
            }
            customerContactResponse.setOwnerName(userNameMap.get(customerContactResponse.getOwner()));
            UserDeptDTO userDeptDTO = userDeptMap.get(customerContactResponse.getOwner());
            if (userDeptDTO != null) {
                customerContactResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
            String customerName = customNameMap.get(customerContactResponse.getCustomerId());
            //固定展示列脱敏设置
            if (!hasPermission) {
                searchFieldMaskConfigMap.forEach((fieldId, searchFieldMaskConfig) -> {
                    if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "customerId")) {
                        customerContactResponse.setCustomerName((String) getInputFieldValue(customerName, customerName.length()));
                    }
                    if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "name")) {
                        customerContactResponse.setName((String) getInputFieldValue(customerContactResponse.getName(), customerContactResponse.getName().length()));
                    }
                    if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "phone")) {
                        customerContactResponse.setPhone((String) getPhoneFieldValue(customerContactResponse.getPhone(), customerContactResponse.getPhone().length()));
                    }
                });
            } else {
                customerContactResponse.setCustomerName(customerName);
            }
            customerContactResponse.setHasPermission(hasPermission);
        });
        return list;
    }
}
