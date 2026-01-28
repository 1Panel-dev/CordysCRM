package cn.cordys.crm.search.service.advanced;

import cn.cordys.common.constants.*;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.domain.CluePoolRecycleRule;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.clue.service.CluePoolService;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.search.response.advanced.AdvancedCluePoolResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

@Service
public class AdvancedCluePoolSearchService
    extends BaseSearchService<BasePageRequest, AdvancedCluePoolResponse> {

  @Resource private ExtClueMapper extClueMapper;
  @Resource private ClueFieldService clueFieldService;
  @Resource private BaseService baseService;
  @Resource private CluePoolService cluePoolService;
  @Resource private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
  @Resource private DictService dictService;
  @Resource private ModuleFormCacheService moduleFormCacheService;
  @Resource private ModuleFormService moduleFormService;
  @Resource private ExtProductMapper extProductMapper;
  @Resource private DataScopeService dataScopeService;

  @Override
  public PagerWithOption<List<AdvancedCluePoolResponse>> startSearch(
      BasePageRequest request, String orgId, String userId) {
    // 查询当前组织下已启用的模块列表
    List<String> enabledModules = getEnabledModules();

    // 检查：如果线索模块未启用，抛出异常
    if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
      throw new GenericException(SystemResultCode.MODULE_ENABLE);
    }
    ConditionFilterUtils.parseCondition(request);
    // 查询重复线索池线索列表
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    List<AdvancedCluePoolResponse> list = extClueMapper.cluePoolList(request, orgId);
    if (CollectionUtils.isEmpty(list)) {
      return PageUtils.setPageInfoWithOption(page, List.of(), Map.of());
    }
    List<AdvancedCluePoolResponse> buildList = buildListData(list, orgId, userId);
    Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
    return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
  }

  public List<AdvancedCluePoolResponse> buildListData(
      List<AdvancedCluePoolResponse> list, String orgId, String userId) {
    if (CollectionUtils.isEmpty(list)) {
      return list;
    }
    List<String> clueIds = list.stream().map(ClueListResponse::getId).collect(Collectors.toList());

    Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap =
        clueFieldService.getResourceFieldMap(clueIds, true);

    List<String> ownerIds = list.stream().map(ClueListResponse::getOwner).distinct().toList();

    List<String> followerIds = list.stream().map(ClueListResponse::getFollower).distinct().toList();
    List<String> createUserIds =
        list.stream().map(ClueListResponse::getCreateUser).distinct().toList();
    List<String> updateUserIds =
        list.stream().map(ClueListResponse::getUpdateUser).distinct().toList();
    List<String> userIds =
        Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
            .flatMap(Collection::stream)
            .distinct()
            .toList();
    Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

    // 获取负责人线索池信息
    Map<String, CluePool> ownersDefaultPoolMap =
        cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
    List<String> poolIds =
        ownersDefaultPoolMap.values().stream().map(CluePool::getId).distinct().toList();
    Map<String, CluePoolRecycleRule> recycleRuleMap;
    if (CollectionUtils.isEmpty(poolIds)) {
      recycleRuleMap = Map.of();
    } else {
      LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
      recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
      List<CluePoolRecycleRule> recycleRules =
          recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
      recycleRuleMap =
          recycleRules.stream()
              .collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
    }

    Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

    // 线索池原因
    DictConfigDTO dictConf = dictService.getDictConf(DictModule.CLUE_POOL_RS.name(), orgId);
    List<Dict> dictList = dictConf.getDictList();
    Map<String, String> dictMap =
        dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

    // 获取用户线索池
    Map<String, String> userPoolMap = getUserCluePool(orgId, userId);

    list.forEach(
        clueListResponse -> {
          boolean hasPermission = getHasPermission(userId, orgId, clueListResponse, userPoolMap);
          // 设置回收公海
          CluePool reservePool = ownersDefaultPoolMap.get(clueListResponse.getOwner());
          // 获取自定义字段
          if (hasPermission) {
            List<BaseModuleFieldValue> clueFields =
                caseCustomFiledMap.get(clueListResponse.getId());
            clueListResponse.setModuleFields(clueFields);
            clueListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            clueListResponse.setReasonName(dictMap.get(clueListResponse.getReasonId()));

          } else {
            clueListResponse.setModuleFields(new ArrayList<>());
            clueListResponse.setRecyclePoolName(null);
            clueListResponse.setReasonName(null);
            clueListResponse.setPhone(null);
            clueListResponse.setContact(null);
          }
          // 计算剩余归属天数
          clueListResponse.setReservedDays(
              cluePoolService.calcReservedDay(
                  reservePool,
                  reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                  clueListResponse.getCollectionTime(),
                  clueListResponse.getCreateTime()));

          UserDeptDTO userDeptDTO = userDeptMap.get(clueListResponse.getOwner());
          if (userDeptDTO != null) {
            clueListResponse.setDepartmentId(userDeptDTO.getDeptId());
            clueListResponse.setDepartmentName(userDeptDTO.getDeptName());
          }
          clueListResponse.setFollowerName(userNameMap.get(clueListResponse.getFollower()));
          clueListResponse.setCreateUserName(userNameMap.get(clueListResponse.getCreateUser()));
          clueListResponse.setUpdateUserName(userNameMap.get(clueListResponse.getUpdateUser()));
          clueListResponse.setOwnerName(userNameMap.get(clueListResponse.getOwner()));

          clueListResponse.setHasPermission(hasPermission);
          clueListResponse.setPoolName(userPoolMap.get(clueListResponse.getPoolId()));
        });

    return list;
  }

  private boolean getHasPermission(
      String userId,
      String orgId,
      AdvancedCluePoolResponse clueListResponse,
      Map<String, String> userPoolMap) {
    if (Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
      return true;
    }
    if (MapUtils.isEmpty(userPoolMap)) {
      return false;
    }
    boolean hasPool = userPoolMap.containsKey(clueListResponse.getPoolId());
    boolean hasPermission =
        dataScopeService.hasDataPermission(
            userId, orgId, new ArrayList<>(), PermissionConstants.CLUE_MANAGEMENT_POOL_READ);

    return hasPool && hasPermission;
  }

  public Map<String, List<OptionDTO>> buildOptionMap(
      String orgId, List<AdvancedCluePoolResponse> list, List<AdvancedCluePoolResponse> buildList) {
    // 处理自定义字段选项数据
    ModuleFormConfigDTO customerFormConfig =
        moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
    // 获取所有模块字段的值
    List<BaseModuleFieldValue> moduleFieldValues =
        moduleFormService.getBaseModuleFieldValues(list, ClueListResponse::getModuleFields);
    // 获取选项值对应的 option
    Map<String, List<OptionDTO>> optionMap =
        moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

    // 补充负责人选项
    List<OptionDTO> ownerFieldOption =
        moduleFormService.getBusinessFieldOption(
            buildList, ClueListResponse::getOwner, ClueListResponse::getOwnerName);
    optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

    // 意向产品选项
    List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
    optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
    return optionMap;
  }
}
