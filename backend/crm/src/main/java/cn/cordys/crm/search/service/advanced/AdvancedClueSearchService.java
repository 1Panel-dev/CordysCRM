package cn.cordys.crm.search.service.advanced;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.domain.CluePoolRecycleRule;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueFieldService;
import cn.cordys.crm.clue.service.CluePoolService;
import cn.cordys.crm.search.response.advanced.AdvancedClueResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtProductMapper;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
public class AdvancedClueSearchService extends BaseSearchService<CluePageRequest, AdvancedClueResponse> {

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
    public PagerWithOption<List<AdvancedClueResponse>> startSearch(CluePageRequest request, String orgId, String userId) {

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
        List<AdvancedClueResponse> buildList = buildClueList(list, orgId, userId);

        Map<String, List<OptionDTO>> optionMap = buildClueOptionMap(orgId, list, buildList);

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public Map<String, List<OptionDTO>> buildClueOptionMap(String orgId, List<ClueListResponse> list, List<AdvancedClueResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, ClueListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                AdvancedClueResponse::getOwner, AdvancedClueResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);
        return optionMap;
    }


    public List<AdvancedClueResponse> buildClueList(List<ClueListResponse> list, String orgId, String userId) {
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

        List<AdvancedClueResponse> returnList = new ArrayList<>();

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
            AdvancedClueResponse advancedClueResponse = new AdvancedClueResponse();
            BeanUtils.copyBean(advancedClueResponse, clueListResponse);
            boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, clueListResponse.getOwner(), PermissionConstants.CLUE_MANAGEMENT_READ);
            advancedClueResponse.setHasPermission(hasPermission);
            if (!hasPermission) {
                advancedClueResponse.setPhone(null);
                advancedClueResponse.setContact(null);
                advancedClueResponse.setModuleFields(new ArrayList<>());
                advancedClueResponse.setReasonName(null);
                advancedClueResponse.setRecyclePoolName(null);
            }
            returnList.add(advancedClueResponse);
        });
        return returnList;
    }
}
