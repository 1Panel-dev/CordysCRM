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
import io.cordys.common.utils.ConditionFilterUtils;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.customer.service.CustomerFieldService;
import io.cordys.crm.customer.service.CustomerPoolService;
import io.cordys.crm.search.response.GlobalCustomerPoolResponse;
import io.cordys.crm.system.constants.DictModule;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Dict;
import io.cordys.crm.system.dto.DictConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.DictService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.UserExtendService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GlobalSearchCustomerPoolService extends GlobalSearchBaseService<BasePageRequest, GlobalCustomerPoolResponse> {

    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private BaseMapper<CustomerPool> poolMapper;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerPoolService customerPoolService;
    @Resource
    private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
    @Resource
    private DictService dictService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private UserExtendService userExtendService;


    @Override
    public PagerWithOption<List<GlobalCustomerPoolResponse>> globalSearch(BasePageRequest request, String orgId, String userId) {

        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();

        // 检查：如果客户模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        ConditionFilterUtils.parseCondition(request);
        // 查询重复公海客户列表
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<GlobalCustomerPoolResponse> list = extCustomerMapper.customerPoolList(request, orgId);
        if (CollectionUtils.isEmpty(list)) {
            return PageUtils.setPageInfoWithOption(page, null, null);
        }
        List<GlobalCustomerPoolResponse> buildList = buildListData(list, orgId, userId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }


    public List<GlobalCustomerPoolResponse> buildListData(List<GlobalCustomerPoolResponse> list, String orgId, String userId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> customerIds = list.stream().map(CustomerListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerFieldService.getResourceFieldMap(customerIds, true);

        List<String> ownerIds = list.stream()
                .map(CustomerListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(CustomerListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(CustomerListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(CustomerListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        // 获取负责人默认公海信息
        Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CustomerPool::getId).distinct().toList();
        Map<String, CustomerPoolRecycleRule> recycleRuleMap;
        if (CollectionUtils.isEmpty(poolIds)) {
            recycleRuleMap = Map.of();
        } else {
            LambdaQueryWrapper<CustomerPoolRecycleRule> recycleRuleWrapper = new LambdaQueryWrapper<>();
            recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);
            List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, rule -> rule));
        }

        // 公海原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CUSTOMER_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        //获取用户公海
        Map<String, String> userPoolMap = getUserPool(orgId, userId);

        list.forEach(customerListResponse -> {
            boolean hasPermission = getHasPermission(userId, orgId, customerListResponse, userPoolMap);
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);
            // 设置回收公海
            CustomerPool reservePool = ownersDefaultPoolMap.get(customerListResponse.getOwner());
            customerListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            customerListResponse.setReservedDays(customerPoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    customerListResponse));

            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            String followerName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getFollower()));
            customerListResponse.setFollowerName(followerName);
            String createUserName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getCreateUser()));
            customerListResponse.setCreateUserName(createUserName);
            String updateUserName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getUpdateUser()));
            customerListResponse.setUpdateUserName(updateUserName);
            customerListResponse.setOwnerName(userNameMap.get(customerListResponse.getOwner()));
            String reasonName = baseService.getAndCheckOptionName(dictMap.get(customerListResponse.getReasonId()));
            customerListResponse.setReasonName(reasonName);

            customerListResponse.setHasPermission(hasPermission);
            if (!hasPermission) {
                customerListResponse.setModuleFields(new ArrayList<>());
                customerListResponse.setReasonName(null);
                customerListResponse.setRecyclePoolName(null);
            }
            customerListResponse.setPoolName(userPoolMap.get(customerListResponse.getPoolId()));
        });

        return list;
    }

    private Map<String, String> getUserPool(String orgId, String userId) {
        Map<String, String> poolMap = new HashMap<>();
        LambdaQueryWrapper<CustomerPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.eq(CustomerPool::getEnable, true).eq(CustomerPool::getOrganizationId, orgId);
        poolWrapper.orderByDesc(CustomerPool::getUpdateTime);
        List<CustomerPool> pools = poolMapper.selectListByLambda(poolWrapper);
        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class), orgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class), orgId);
            if (scopeIds.contains(userId) || ownerIds.contains(userId) || Strings.CS.equals(userId, InternalUser.ADMIN.getValue())) {
                poolMap.put(pool.getId(), pool.getName());
            }
        });
        return poolMap;
    }


    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<GlobalCustomerPoolResponse> list, List<GlobalCustomerPoolResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, CustomerListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                CustomerListResponse::getOwner, CustomerListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        return optionMap;
    }

    private boolean getHasPermission(String userId, String orgId, GlobalCustomerPoolResponse customerPoolResponse, Map<String, String> userPoolMap) {

        if (Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
            return true;
        }

        if (MapUtils.isEmpty(userPoolMap)) {
            return false;
        }

        boolean hasPool = userPoolMap.containsKey(customerPoolResponse.getPoolId());
        boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, new ArrayList<>(), PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ);

        if (hasPool && hasPermission) {
            return true;
        }
        return false;
    }
}
