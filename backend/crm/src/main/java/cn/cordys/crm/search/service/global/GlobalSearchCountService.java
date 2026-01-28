package cn.cordys.crm.search.service.global;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.OptionCountDTO;
import cn.cordys.common.dto.condition.FilterCondition;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.search.constants.SearchPhoneEnum;
import cn.cordys.crm.search.domain.UserSearchConfig;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

@Service
public class GlobalSearchCountService extends BaseSearchService<BasePageRequest, OptionCountDTO> {
  @Resource private ExtUserRoleMapper extUserRoleMapper;
  @Resource private ExtCustomerMapper extCustomerMapper;
  @Resource private ExtCustomerContactMapper extCustomerContactMapper;
  @Resource private ExtClueMapper extClueMapper;
  @Resource private ExtOpportunityMapper extOpportunityMapper;

  public List<OptionCountDTO> startSearchModuleCount(String keyword, String orgId, String userId) {
    List<OptionCountDTO> list = new ArrayList<>();
    if (StringUtils.isBlank(keyword)) {
      return new ArrayList<>();
    }
    // 处理参数中（+区号）这种会被spring RequestParam参数处理为空格的情况
    if (keyword.contains("(") && keyword.contains(")")) {
      // 去掉首尾空格
      keyword = keyword.trim();
      // 匹配形如 ( 86) 或 (86) 这样的区号
      keyword = keyword.replaceAll("\\(\\s*(\\d+)\\)", "(+$1)");
    }
    List<String> permissions =
        extUserRoleMapper.selectPermissionsByUserId(SessionUtils.getUserId());
    BasePageRequest request = new BasePageRequest();
    request.setCurrent(1);
    request.setPageSize(1);

    List<String> enabledModules = getEnabledModules();
    // 查询当前用户搜索配置
    // 用户配置设置:
    // 1.用户没配置过，设置默认查询条件;
    // 2.用户有配置，使用用户配置的查询条件;
    // 3.用户当前模块没配置，直接返回;
    List<UserSearchConfig> userSearchConfigs = getUserSearchConfigs(userId, orgId);
    List<FilterCondition> conditions;
    if (enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
      if (permissions.contains(PermissionConstants.CUSTOMER_MANAGEMENT_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        // 查客户
        conditions = new ArrayList<>();
        long customerCount =
            getCustomerCount(keyword, orgId, userSearchConfigs, list, conditions, request);
        list.add(
            new OptionCountDTO(SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER, (int) customerCount));
      }
      if (permissions.contains(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        // 查客户联系人
        conditions = new ArrayList<>();
        long contactCount =
            getContactCount(keyword, orgId, userSearchConfigs, list, conditions, request);
        list.add(new OptionCountDTO(SearchModuleEnum.SEARCH_ADVANCED_CONTACT, (int) contactCount));
      }
      if (permissions.contains(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        // 查公海
        conditions = new ArrayList<>();
        long publicCount =
            getPublicCount(keyword, orgId, userSearchConfigs, list, conditions, request);
        list.add(new OptionCountDTO(SearchModuleEnum.SEARCH_ADVANCED_PUBLIC, (int) publicCount));
      }
    }
    if (enabledModules.contains(ModuleKey.CLUE.getKey())) {
      if (permissions.contains(PermissionConstants.CLUE_MANAGEMENT_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        conditions = new ArrayList<>();
        long clueCount = getClueCount(keyword, orgId, userSearchConfigs, conditions, request);
        list.add(new OptionCountDTO(SearchModuleEnum.SEARCH_ADVANCED_CLUE, (int) clueCount));
      }
      if (permissions.contains(PermissionConstants.CLUE_MANAGEMENT_POOL_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        conditions = new ArrayList<>();
        long cluePoolCount =
            getCluePoolCount(keyword, orgId, userSearchConfigs, list, conditions, request);
        list.add(
            new OptionCountDTO(SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL, (int) cluePoolCount));
      }
    }

    if (enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
      if (permissions.contains(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ)
          || Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
        conditions = new ArrayList<>();
        long opportunityCount =
            getOpportunityCount(keyword, orgId, userSearchConfigs, list, conditions, request);
        list.add(
            new OptionCountDTO(
                SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY, (int) opportunityCount));
      }
    }
    return list;
  }

  /**
   * 获取商机数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param list 返回结果集
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getOpportunityCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<OptionCountDTO> list,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    List<String> customerIds = getCustomerIds(keyword, orgId);
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> opportunitySearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(
                          t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY))
              .toList();
      if (CollectionUtils.isEmpty(opportunitySearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : opportunitySearchConfigs) {
          // 如果和固定展示列名重复不加入fieldIdSet
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
      }
    } else {
      // 设置商机默认查询属性
      // 查询客户源数据
      FilterCondition nameCondition =
          getFilterCondition(
              "name",
              keyword,
              FilterCondition.CombineConditionOperator.CONTAINS.toString(),
              FieldType.INPUT.toString());
      conditions.add(nameCondition);
      if (CollectionUtils.isNotEmpty(list)) {
        FilterCondition customerCondition =
            getFilterCondition(
                "customerId",
                customerIds,
                FilterCondition.CombineConditionOperator.IN.toString(),
                FieldType.DATA_SOURCE.toString());
        conditions.add(customerCondition);
      }
    }
    if (CollectionUtils.isEmpty(conditions)) {
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    return extOpportunityMapper.globalSearchListCount(request, orgId);
  }

  /**
   * 获取线索池数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param list 返回结果集
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getCluePoolCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<OptionCountDTO> list,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> cluePoolSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(
                          t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL))
              .toList();
      if (CollectionUtils.isEmpty(cluePoolSearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : cluePoolSearchConfigs) {
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
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
      StringUtils.deleteWhitespace(keyword);
      List<String> phoneList = new ArrayList<>();
      phoneList.add(keyword);
      for (String value : SearchPhoneEnum.VALUES) {
        phoneList.add(value + keyword);
      }
      FilterCondition phoneCondition =
          getFilterCondition(
              "phone",
              phoneList,
              FilterCondition.CombineConditionOperator.IN.toString(),
              FieldType.DATA_SOURCE.toString());
      conditions.add(phoneCondition);
    }
    if (CollectionUtils.isEmpty(conditions)) {
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);

    return extClueMapper.globalPoolSearchListCount(request, orgId);
  }

  /**
   * 获取线索数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getClueCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> clueSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t -> Strings.CI.equals(t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CLUE))
              .toList();
      if (CollectionUtils.isEmpty(clueSearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : clueSearchConfigs) {
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
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
      StringUtils.deleteWhitespace(keyword);
      List<String> phoneList = new ArrayList<>();
      phoneList.add(keyword);
      for (String value : SearchPhoneEnum.VALUES) {
        phoneList.add(value + keyword);
      }
      FilterCondition phoneCondition =
          getFilterCondition(
              "phone",
              phoneList,
              FilterCondition.CombineConditionOperator.IN.toString(),
              FieldType.DATA_SOURCE.toString());
      conditions.add(phoneCondition);
    }
    if (CollectionUtils.isEmpty(conditions)) {
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    return extClueMapper.globalSearchListCount(request, orgId);
  }

  /**
   * 获取公海数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param list 返回结果集
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getPublicCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<OptionCountDTO> list,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> customerPoolSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_PUBLIC))
              .toList();
      if (CollectionUtils.isEmpty(customerPoolSearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : customerPoolSearchConfigs) {
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
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
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    return extCustomerMapper.globalPoolSearchListCount(request, orgId);
  }

  /**
   * 获取客户联系人数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param list 返回结果集
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getContactCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<OptionCountDTO> list,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    List<String> customerIds = getCustomerIds(keyword, orgId);
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> contactSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(
                          t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CONTACT))
              .toList();
      if (CollectionUtils.isEmpty(contactSearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : contactSearchConfigs) {
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
      }
    } else {
      // 设置默认查询属性
      if (CollectionUtils.isNotEmpty(customerIds)) {
        FilterCondition customerCondition =
            getFilterCondition(
                "customerId",
                list,
                FilterCondition.CombineConditionOperator.IN.toString(),
                FieldType.DATA_SOURCE.toString());
        conditions.add(customerCondition);
      }
      StringUtils.deleteWhitespace(keyword);
      List<String> phoneList = new ArrayList<>();
      phoneList.add(keyword);
      for (String value : SearchPhoneEnum.VALUES) {
        phoneList.add(value + keyword);
      }
      FilterCondition phoneCondition =
          getFilterCondition(
              "phone",
              phoneList,
              FilterCondition.CombineConditionOperator.IN.toString(),
              FieldType.DATA_SOURCE.toString());
      conditions.add(phoneCondition);
    }
    if (CollectionUtils.isEmpty(conditions)) {
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    return extCustomerContactMapper.globalSearchListCount(request, orgId);
  }

  /**
   * 获取客户数量
   *
   * @param keyword 关键字
   * @param orgId 组织ID
   * @param userSearchConfigs 用户搜索配置
   * @param list 返回结果集
   * @param conditions 查询条件
   * @param request 分页参数
   * @return 数量
   */
  private long getCustomerCount(
      String keyword,
      String orgId,
      List<UserSearchConfig> userSearchConfigs,
      List<OptionCountDTO> list,
      List<FilterCondition> conditions,
      BasePageRequest request) {
    if (CollectionUtils.isNotEmpty(userSearchConfigs)) {
      List<UserSearchConfig> customerSearchConfigs =
          userSearchConfigs.stream()
              .filter(
                  t ->
                      Strings.CI.equals(
                          t.getModuleType(), SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER))
              .toList();
      if (CollectionUtils.isEmpty(customerSearchConfigs)) {
        return 0L;
      } else {
        for (UserSearchConfig userSearchConfig : customerSearchConfigs) {
          buildOtherFilterCondition(orgId, userSearchConfig, keyword, conditions);
        }
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
      return 0L;
    }
    // 构造查询参数
    buildCombineSearch(conditions, request);
    return extCustomerMapper.globalSearchListCount(request, orgId);
  }
}
