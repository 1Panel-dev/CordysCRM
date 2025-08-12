package io.cordys.crm.search.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.*;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.JSON;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.service.ClueFieldService;
import io.cordys.crm.clue.service.CluePoolService;
import io.cordys.crm.search.response.GlobalCluePoolResponse;
import io.cordys.crm.system.constants.DictModule;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Dict;
import io.cordys.crm.system.dto.DictConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.service.DictService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GlobalSearchCluePoolService extends GlobalSearchBaseService<BasePageRequest, GlobalCluePoolResponse> {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private BaseService baseService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private DictService dictService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private BaseMapper<CluePool> poolMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private DataScopeService dataScopeService;

    @Override
    public PagerWithOption<List<GlobalCluePoolResponse>> globalSearch(BasePageRequest request, String orgId, String userId) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());

        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();

        // 检查：如果线索模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        // 查询重复线索池线索列表
        List<GlobalCluePoolResponse> list = extClueMapper.cluePoolList(request, orgId);
        if (CollectionUtils.isEmpty(list)) {
            return PageUtils.setPageInfoWithOption(page, null, null);
        }
        List<GlobalCluePoolResponse> buildList = buildListData(list, orgId, userId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }


    public List<GlobalCluePoolResponse> buildListData(List<GlobalCluePoolResponse> list, String orgId, String userId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> clueIds = list.stream().map(ClueListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = clueFieldService.getResourceFieldMap(clueIds, true);

        List<String> ownerIds = list.stream()
                .map(ClueListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(ClueListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(ClueListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(ClueListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        // 获取负责人线索池信息
        Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CluePool::getId).distinct().toList();
        Map<String, CluePoolRecycleRule> recycleRuleMap;
        if (CollectionUtils.isEmpty(poolIds)) {
            recycleRuleMap = Map.of();
        } else {
            LambdaQueryWrapper<CluePoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
            recycleRuleWrapper.in(CluePoolRecycleRule::getPoolId, poolIds);
            List<CluePoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CluePoolRecycleRule::getPoolId, rule -> rule));
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        // 线索池原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CLUE_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        //获取用户线索池
        Map<String, String> userPoolMap = getUserPool(orgId, userId);

        list.forEach(clueListResponse -> {
            boolean hasPermission = getHasPermission(userId, orgId, clueListResponse, userPoolMap);
            // 设置回收公海
            CluePool reservePool = ownersDefaultPoolMap.get(clueListResponse.getOwner());
            // 获取自定义字段
            if (hasPermission) {
                List<BaseModuleFieldValue> clueFields = caseCustomFiledMap.get(clueListResponse.getId());
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
            clueListResponse.setReservedDays(cluePoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    clueListResponse));

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

    private boolean getHasPermission(String userId, String orgId, GlobalCluePoolResponse clueListResponse, Map<String, String> userPoolMap) {
        if (StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }
        if (MapUtils.isEmpty(userPoolMap)) {
            return false;
        }
        boolean hasPool = userPoolMap.containsKey(clueListResponse.getPoolId());
        boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, new ArrayList<>(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);

        if (hasPool && hasPermission) {
            return true;
        }
        return false;
    }

    private Map<String, String> getUserPool(String orgId, String userId) {
        Map<String, String> poolMap = new HashMap<>();
        LambdaQueryWrapper<CluePool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CluePool::getEnable, true).eq(CluePool::getOrganizationId, orgId);
        poolWrapper.orderByDesc(CluePool::getUpdateTime);
        List<CluePool> pools = poolMapper.selectListByLambda(poolWrapper);
        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), orgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), orgId);
            if (scopeIds.contains(userId) || ownerIds.contains(userId) || StringUtils.equals(userId, InternalUser.ADMIN.getValue())) {
                poolMap.put(pool.getId(), pool.getName());
            }
        });
        return poolMap;
    }

    @NotNull
    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<GlobalCluePoolResponse> list, List<GlobalCluePoolResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ClueListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                ClueListResponse::getOwner, ClueListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        return optionMap;
    }
}
