package io.cordys.crm.search.service.global;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.dto.condition.CombineSearch;
import io.cordys.common.dto.condition.FilterCondition;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.opportunity.service.OpportunityFieldService;
import io.cordys.crm.search.constants.SearchModuleEnum;
import io.cordys.crm.search.domain.SearchFieldMaskConfig;
import io.cordys.crm.search.domain.UserSearchConfig;
import io.cordys.crm.search.response.global.GlobalClueResponse;
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
public class GlobalClueSearchService extends BaseSearchService<BasePageRequest, GlobalClueResponse> {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;



    @Override
    public Pager<List<GlobalClueResponse>> startSearchNoOption(BasePageRequest request, String orgId, String userId) {
        //获取查询关键字
        String keyword = request.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
            return PageUtils.setPageInfo(page, null);
        }
        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果有商机读取权限但商机模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
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
            List<UserSearchConfig> clueSearchConfigs = userSearchConfigs.stream().filter(t -> Strings.CI.equals(t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CLUE)).toList();
            if (CollectionUtils.isEmpty(clueSearchConfigs)) {
                Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
                return PageUtils.setPageInfo(page, null);
            }
            for (UserSearchConfig userSearchConfig : clueSearchConfigs) {
                //如果和固定展示列名重复不加入fieldIdSet
                if (StringUtils.isBlank(userSearchConfig.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                } else if (!Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_NAME.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey()) &&
                        !Strings.CI.equals(userSearchConfig.getBusinessKey(), BusinessModuleField.CLUE_PRODUCTS.getBusinessKey())) {
                    fieldIdSet.add(userSearchConfig.getFieldId());
                }
                buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
            }
        } else {
            //设置默认查询属性
            FilterCondition nameCondition = getFilterCondition("name", keyword, FilterCondition.CombineConditionOperator.CONTAINS.toString(), FieldType.INPUT.toString());
            conditions.add(nameCondition);
        }

        //构造查询参数
        buildCombineSearch(conditions, request);
        // 查询重复商机列表
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<GlobalClueResponse> globalClueResponses = extClueMapper.globalSearchList(request, orgId);
        if (CollectionUtils.isEmpty(globalClueResponses)) {
            return PageUtils.setPageInfo(page, null);
        }
        //获取系统设置的脱敏字段
        List<SearchFieldMaskConfig> searchFieldMaskConfigs = getSearchFieldMaskConfigs(orgId);
        List<GlobalClueResponse> buildList = buildListData(globalClueResponses, orgId, userId, searchFieldMaskConfigs, fieldIdSet);
        return PageUtils.setPageInfo(page, buildList);
    }


    public List<GlobalClueResponse> buildListData(List<GlobalClueResponse> list, String orgId, String userId, List<SearchFieldMaskConfig> searchFieldMaskConfigs, Set<String> fieldIdSet) {
        List<String> clueIds = list.stream().map(GlobalClueResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> opportunityFiledMap = opportunityFieldService.getResourceFieldMap(clueIds, true);

        List<String> ownerIds = list.stream()
                .map(GlobalClueResponse::getOwner)
                .distinct()
                .toList();

        Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);

        Map<String, String> productNameMap = getProductNameMap(orgId);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);
        // 处理自定义字段选项数据
        ModuleFormConfigDTO clueFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
        Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap = searchFieldMaskConfigs.stream().collect(Collectors.toMap(SearchFieldMaskConfig::getFieldId, t -> t));
        list.forEach(globalClueResponse -> {
            // 判断该数据是否有权限
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, globalClueResponse.getOwner(), PermissionConstants.CLUE_MANAGEMENT_READ);
            // 处理自定义字段数据
            if (CollectionUtils.isNotEmpty(fieldIdSet)) {
                List<BaseModuleFieldValue> returnOpportunityFields = getBaseModuleFieldValues(fieldIdSet, globalClueResponse.getId(), opportunityFiledMap, clueFormConfig, searchFieldMaskConfigMap, hasPermission);

                globalClueResponse.setModuleFields(returnOpportunityFields);
            }

            globalClueResponse.setOwnerName(userNameMap.get(globalClueResponse.getOwner()));

            UserDeptDTO userDeptDTO = userDeptMap.get(globalClueResponse.getOwner());
            if (userDeptDTO != null) {
                globalClueResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
            List<String> productNames = getProductNames(globalClueResponse.getProducts(), productNameMap);
            globalClueResponse.setProducts(productNames);
            globalClueResponse.setHasPermission(hasPermission);
        });
        return list;
    }

}
