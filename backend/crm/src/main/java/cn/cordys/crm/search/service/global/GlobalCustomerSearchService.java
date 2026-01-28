package cn.cordys.crm.search.service.global;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.service.CustomerFieldService;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.search.domain.SearchFieldMaskConfig;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.response.global.GlobalCustomerResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

@Service
public class GlobalCustomerSearchService
    extends BaseSearchService<BasePageRequest, GlobalCustomerResponse> {

  @Resource private ExtCustomerMapper extCustomerMapper;
  @Resource private CustomerFieldService customerFieldService;
  @Resource private ModuleFormCacheService moduleFormCacheService;
  @Resource private DataScopeService dataScopeService;
  @Resource private BaseService baseService;

  @Override
  public Pager<List<GlobalCustomerResponse>> startSearchNoOption(
      BasePageRequest request, String orgId, String userId) {
    // 获取查询关键字
    String keyword = request.getKeyword();
    if (StringUtils.isBlank(keyword)) {
      Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
      return PageUtils.setPageInfo(page, List.of());
    }
    // 查询当前组织下已启用的模块列表
    List<String> enabledModules = getEnabledModules();
    // 检查：如果有商机读取权限但商机模块未启用，抛出异常
    if (!enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
      throw new GenericException(SystemResultCode.MODULE_ENABLE);
    }
    // 查询当前用户搜索配置
    List<UserSearchConfig> userSearchConfigs = getUserSearchConfigs(userId, orgId);
    // 记住当前一共有多少字段，避免固定展示列与自由选择列字段重复
    Set<String> fieldIdSet = new HashSet<>();
    List<FilterCondition> conditions = new ArrayList<>();
    // 用户配置设置:
    // 1.用户没配置过，设置默认查询条件;
    // 2.用户有配置，使用用户配置的查询条件;
    // 3.用户当前模块没配置，直接返回;
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> customerSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(
                          t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER))
              .toList();
      if (CollectionUtils.isEmpty(customerSearchConfigs)) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, List.of());
      }
      for (UserSearchConfig userSearchConfig : customerSearchConfigs) {
        // 如果和固定展示列名重复不加入fieldIdSet
        if (StringUtils.isBlank(userSearchConfig.getBusinessKey())) {
          fieldIdSet.add(userSearchConfig.getFieldId());
        } else if (!Strings.CI.equals(
                userSearchConfig.getBusinessKey(),
                BusinessModuleField.CUSTOMER_NAME.getBusinessKey())
            && !Strings.CI.equals(
                userSearchConfig.getBusinessKey(),
                BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
          fieldIdSet.add(userSearchConfig.getFieldId());
        }
        buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
      }
    } else {
      // 设置默认查询属性
      FilterCondition nameCondition =
          getFilterCondition(
              "name",
              keyword,
              FilterCondition.CombineConditionOperator.CONTAINS.toString(),
              FieldType.INPUT.toString());
      conditions.add(nameCondition);
    }
    if (CollectionUtils.isEmpty(conditions)) {
      Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
      return PageUtils.setPageInfo(page, List.of());
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    // 搜索客户
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    List<GlobalCustomerResponse> globalCustomerResponses =
        extCustomerMapper.globalSearchList(request, orgId);
    if (CollectionUtils.isEmpty(globalCustomerResponses)) {
      return PageUtils.setPageInfo(page, List.of());
    }
    // 获取系统设置的脱敏字段
    List<SearchFieldMaskConfig> searchFieldMaskConfigs =
        getSearchFieldMaskConfigs(orgId, SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER);
    List<GlobalCustomerResponse> buildList =
        buildListData(globalCustomerResponses, orgId, userId, searchFieldMaskConfigs, fieldIdSet);
    return PageUtils.setPageInfo(page, buildList);
  }

  private List<GlobalCustomerResponse> buildListData(
      List<GlobalCustomerResponse> list,
      String orgId,
      String userId,
      List<SearchFieldMaskConfig> searchFieldMaskConfigs,
      Set<String> fieldIdSet) {
    List<String> customerIds =
        list.stream().map(GlobalCustomerResponse::getId).collect(Collectors.toList());
    Map<String, List<BaseModuleFieldValue>> customerFiledMap =
        customerFieldService.getResourceFieldMap(customerIds, true);

    List<String> ownerIds = list.stream().map(GlobalCustomerResponse::getOwner).distinct().toList();

    Map<String, String> userNameMap = baseService.getUserNameMap(ownerIds);

    Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

    // 处理自定义字段选项数据
    ModuleFormConfigDTO customerFormConfig =
        moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
    Map<String, SearchFieldMaskConfig> searchFieldMaskConfigMap =
        searchFieldMaskConfigs.stream()
            .collect(Collectors.toMap(SearchFieldMaskConfig::getFieldId, t -> t));
    list.forEach(
        customerResponse -> {
          // 判断该数据是否有权限
          boolean hasPermission =
              dataScopeService.hasDataPermission(
                  userId,
                  orgId,
                  customerResponse.getOwner(),
                  PermissionConstants.CUSTOMER_MANAGEMENT_READ);
          // 处理自定义字段数据
          if (CollectionUtils.isNotEmpty(fieldIdSet)) {
            List<BaseModuleFieldValue> returnCustomerFields =
                getBaseModuleFieldValues(
                    fieldIdSet,
                    customerResponse.getId(),
                    customerFiledMap,
                    customerFormConfig,
                    searchFieldMaskConfigMap,
                    hasPermission);

            customerResponse.setModuleFields(returnCustomerFields);
          }
          customerResponse.setOwnerName(userNameMap.get(customerResponse.getOwner()));
          UserDeptDTO userDeptDTO = userDeptMap.get(customerResponse.getOwner());
          if (userDeptDTO != null) {
            customerResponse.setDepartmentName(userDeptDTO.getDeptName());
          }
          // 固定展示列脱敏设置
          if (!hasPermission) {
            searchFieldMaskConfigMap.forEach(
                (fieldId, searchFieldMaskConfig) -> {
                  if (Strings.CI.equals(searchFieldMaskConfig.getBusinessKey(), "name")) {
                    customerResponse.setName(
                        (String)
                            getInputFieldValue(
                                customerResponse.getName(), customerResponse.getName().length()));
                  }
                });
          }
          customerResponse.setHasPermission(hasPermission);
        });
    return list;
  }
}
