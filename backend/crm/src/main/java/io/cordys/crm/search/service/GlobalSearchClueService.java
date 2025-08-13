package io.cordys.crm.search.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.utils.ConditionFilterUtils;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.clue.service.ClueFieldService;
import io.cordys.crm.clue.service.CluePoolService;
import io.cordys.crm.search.response.GlobalClueResponse;
import io.cordys.crm.system.constants.DictModule;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Dict;
import io.cordys.crm.system.dto.DictConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.service.DictService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GlobalSearchClueService extends GlobalSearchBaseService<CluePageRequest, GlobalClueResponse> {

    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private DictService dictService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BaseService baseService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;


    @Override
    public PagerWithOption<List<GlobalClueResponse>> globalSearch(CluePageRequest request, String orgId, String userId) {

        /// 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果客户模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }
        request.setPoolId(null);
        ConditionFilterUtils.parseCondition(request);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 4. 查询并返回相似线索列表
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, null);
        if (CollectionUtils.isEmpty(list)) {
            return PageUtils.setPageInfoWithOption(page, null, null);
        }
        List<GlobalClueResponse> buildList = buildClueList(list, orgId, userId);

        Map<String, List<OptionDTO>> optionMap = buildClueOptionMap(orgId, list, buildList);

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public Map<String, List<OptionDTO>> buildClueOptionMap(String orgId, List<ClueListResponse> list, List<GlobalClueResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ClueListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                GlobalClueResponse::getOwner, GlobalClueResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        return optionMap;
    }


    public List<GlobalClueResponse> buildClueList(List<ClueListResponse> list, String orgId, String userId) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
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

        List<GlobalClueResponse> returnList = new ArrayList<>();

        list.forEach(clueListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> clueFields = caseCustomFiledMap.get(clueListResponse.getId());
            clueListResponse.setModuleFields(clueFields);

            // 设置回收公海
            CluePool reservePool = ownersDefaultPoolMap.get(clueListResponse.getOwner());
            clueListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
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
            clueListResponse.setReasonName(dictMap.get(clueListResponse.getReasonId()));
            GlobalClueResponse globalClueResponse = new GlobalClueResponse();
            BeanUtils.copyBean(globalClueResponse, clueListResponse);
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, clueListResponse.getOwner(), PermissionConstants.CLUE_MANAGEMENT_ADD);
            globalClueResponse.setHasPermission(hasPermission);
            if (!hasPermission) {
                globalClueResponse.setPhone(null);
                globalClueResponse.setContact(null);
                globalClueResponse.setModuleFields(new ArrayList<>());
                globalClueResponse.setReasonName(null);
                globalClueResponse.setRecyclePoolName(null);
            }
            returnList.add(globalClueResponse);
        });
        return returnList;
    }
}
