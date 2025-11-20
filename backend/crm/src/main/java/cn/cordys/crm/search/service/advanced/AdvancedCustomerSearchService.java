package cn.cordys.crm.search.service.advanced;

import cn.cordys.common.constants.*;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.service.CustomerFieldService;
import cn.cordys.crm.customer.service.CustomerPoolService;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.search.response.advanced.AdvancedClueResponse;
import cn.cordys.crm.search.response.advanced.AdvancedCustomerResponse;
import cn.cordys.crm.search.response.advanced.OpportunityRepeatResponse;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.SystemResultCode;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.request.RepeatCustomerDetailPageRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.system.service.DictService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdvancedCustomerSearchService extends BaseSearchService<CustomerPageRequest, AdvancedCustomerResponse> {

    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private CustomerPoolService customerPoolService;
    @Resource
    private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private DictService dictService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private BaseService baseService;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExtProductMapper extProductMapper;

    @Override
    public PagerWithOption<List<AdvancedCustomerResponse>> startSearch(CustomerPageRequest request, String orgId, String userId) {

        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = getEnabledModules();
        // 检查：如果客户模块未启用，抛出异常
        if (!enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        boolean isAdmin = Strings.CI.equals(userId, InternalUser.ADMIN.getValue());

        ConditionFilterUtils.parseCondition(request);
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 查询重复客户列表
        List<AdvancedCustomerResponse> customers = extCustomerMapper.checkRepeatCustomer(request, orgId, userId);
        if (CollectionUtils.isEmpty(customers)) {
            return PageUtils.setPageInfoWithOption(
                    page, List.of(), Map.of());
        }
        List<AdvancedCustomerResponse> buildList = buildCustomerList(orgId, userId, isAdmin, customers, enabledModules);
        Map<String, List<OptionDTO>> optionMap = buildCustomerOptionMap(orgId, customers, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    private List<AdvancedCustomerResponse> buildCustomerList(String organizationId, String userId, boolean isAdmin, List<AdvancedCustomerResponse> customers, List<String> enabledModules) {
        // 查询用户权限
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(userId);
        // 获取商机和线索的重复数量映射
        Map<String, String> opportunityCounts = getOpportunityCounts(permissions, isAdmin, customers, enabledModules);
        Map<String, String> clueCounts = getClueCounts(permissions, isAdmin, customers, enabledModules);

        // 检查相关模块是否启用
        boolean isClueModuleEnabled = enabledModules.contains(ModuleKey.CLUE.getKey());
        boolean isOpportunityModuleEnabled = enabledModules.contains(ModuleKey.BUSINESS.getKey());

        List<String> customerIds = customers.stream().map(AdvancedCustomerResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = customerFieldService.getResourceFieldMap(customerIds, true);

        List<String> ownerIds = customers.stream()
                .map(AdvancedCustomerResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = customers.stream()
                .map(AdvancedCustomerResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = customers.stream()
                .map(AdvancedCustomerResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = customers.stream()
                .map(AdvancedCustomerResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, organizationId);

        // 获取负责人默认公海信息
        Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, organizationId);
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
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CUSTOMER_POOL_RS.name(), organizationId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        // 处理每个客户返回结果
        return customers.stream()
                .peek(customer -> {
                    boolean hasPermission = dataScopeService.hasDataPermission(userId, organizationId, customer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
                    // 设置商机数量
                    CustomerPool reservePool = ownersDefaultPoolMap.get(customer.getOwner());
                    if (!hasPermission) {
                        customer.setOpportunityCount(null);
                        // 设置线索数量
                        customer.setClueCount(null);
                        customer.setModuleFields(new ArrayList<>());
                        customer.setRecyclePoolName(null);
                        customer.setReasonName(null);
                    } else {
                        customer.setOpportunityCount(parseCount(opportunityCounts.get(customer.getId())));
                        customer.setClueCount(parseCount(clueCounts.get(customer.getName())));
                        // 获取自定义字段
                        List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customer.getId());
                        customer.setModuleFields(customerFields);
                        // 设置回收公海
                        customer.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
                    }
                    // 计算剩余归属天数
                    customer.setReservedDays(customerPoolService.calcReservedDay(reservePool,
                            reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                            customer.getCollectionTime(), customer.getCreateTime()));
                    // 设置模块启用状态
                    customer.setOpportunityModuleEnable(isOpportunityModuleEnabled);
                    customer.setClueModuleEnable(isClueModuleEnabled);
                    customer.setHasPermission(hasPermission);
                    UserDeptDTO userDeptDTO = userDeptMap.get(customer.getOwner());
                    if (userDeptDTO != null) {
                        customer.setDepartmentId(userDeptDTO.getDeptId());
                        customer.setDepartmentName(userDeptDTO.getDeptName());
                    }

                    String followerName = baseService.getAndCheckOptionName(userNameMap.get(customer.getFollower()));
                    customer.setFollowerName(followerName);
                    String createUserName = baseService.getAndCheckOptionName(userNameMap.get(customer.getCreateUser()));
                    customer.setCreateUserName(createUserName);
                    String updateUserName = baseService.getAndCheckOptionName(userNameMap.get(customer.getUpdateUser()));
                    customer.setUpdateUserName(updateUserName);
                    customer.setOwnerName(userNameMap.get(customer.getOwner()));
                    String reasonName = baseService.getAndCheckOptionName(dictMap.get(customer.getReasonId()));
                    customer.setReasonName(reasonName);
                })
                .toList();
    }

    public Map<String, List<OptionDTO>> buildCustomerOptionMap(String orgId, List<AdvancedCustomerResponse> list, List<AdvancedCustomerResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, AdvancedCustomerResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                AdvancedCustomerResponse::getOwner, AdvancedCustomerResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        return optionMap;
    }

    /**
     * 获取客户关联的商机数量
     *
     * @param permissions    用户权限列表
     * @param isAdmin        是否是管理员
     * @param customers      客户列表
     * @param enabledModules 已启用模块列表
     *
     * @return 商机数量映射(客户ID - > 数量)
     */
    private Map<String, String> getOpportunityCounts(List<String> permissions,
                                                     boolean isAdmin,
                                                     List<AdvancedCustomerResponse> customers,
                                                     List<String> enabledModules) {
        // 没有商机读取权限且不是管理员返回空map
        if (!permissions.contains(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ) && !isAdmin) {
            return Collections.emptyMap();
        }
        // 商机模块未启用返回空map
        if (!enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            return Collections.emptyMap();
        }

        // 获取客户ID列表并查询商机数量
        List<String> customerIds = customers.stream()
                .map(AdvancedCustomerResponse::getId)
                .toList();

        return extOpportunityMapper.getRepeatCountMap(customerIds).stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    /**
     * 获取客户关联的线索数量
     *
     * @param permissions    用户权限列表
     * @param isAdmin        是否是管理员
     * @param customers      客户列表
     * @param enabledModules 已启用模块列表
     *
     * @return 线索数量映射(客户名称 - > 数量)
     */
    private Map<String, String> getClueCounts(List<String> permissions,
                                              boolean isAdmin,
                                              List<AdvancedCustomerResponse> customers,
                                              List<String> enabledModules) {
        // 没有线索读取权限且不是管理员返回空map
        if (!permissions.contains(PermissionConstants.CLUE_MANAGEMENT_READ) && !permissions.contains(PermissionConstants.CLUE_MANAGEMENT_POOL_READ) && !isAdmin) {
            return Collections.emptyMap();
        }
        // 线索模块未启用返回空map
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
            return Collections.emptyMap();
        }

        // 获取客户名称列表并查询线索数量
        List<String> customerNames = customers.stream()
                .map(AdvancedCustomerResponse::getName)
                .toList();

        return extClueMapper.getRepeatCountMap(customerNames).stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    /**
     * 安全解析数量字符串
     *
     * @param count 数量字符串
     *
     * @return 解析后的整型数量(空字符串返回0)
     */
    private int parseCount(String count) {
        return StringUtils.isBlank(count) ? 0 : Integer.parseInt(count);
    }

    private void getProductNames(List<OptionDTO> productOption, List<AdvancedClueResponse> list) {
        // 设置产品名称
        Map<String, String> productMap = productOption.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
        list.forEach(clue -> {
            // 设置产品名称
            List<String> productNames = new ArrayList<>();
            List<String> productIds = clue.getProducts();
            productIds.forEach(product -> {
                if (productMap.containsKey(product)) {
                    productNames.add(productMap.get(product));
                }
            });
            clue.setProductNameList(productNames);
        });
    }

    public List<AdvancedClueResponse> getRepeatClueDetail(RepeatCustomerDetailPageRequest request,
                                                          String organizationId) {
        List<AdvancedClueResponse> repeatClueList = extClueMapper.getRepeatClueList(request.getName(), organizationId);
        if (CollectionUtils.isEmpty(repeatClueList)) {
            return repeatClueList;
        }
        List<OptionDTO> productOption = extProductMapper.getOptions(organizationId);
        // 设置产品名称
        getProductNames(productOption, repeatClueList);

        return repeatClueList;
    }

    /**
     * 获取重复商机详情列表
     *
     * @param request 包含商机ID的查询请求
     *
     * @return 重复商机响应列表(包含产品名称信息)
     */
    public List<OpportunityRepeatResponse> getRepeatOpportunityDetail(
            RepeatCustomerDetailPageRequest request) {

        // 1. 获取基础重复商机列表
        List<OpportunityRepeatResponse> responses = extOpportunityMapper.getRepeatList(request.getId());

        if (CollectionUtils.isEmpty(responses)) {
            return responses;
        }

        // 2. 获取所有不重复的产品ID列表
        List<String> productIds = responses.stream()
                .flatMap(response -> response.getProducts().stream())
                .distinct()
                .toList();

        // 3. 批量获取产品名称映射(优化数据库查询)
        Map<String, String> productNameMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : extProductMapper.listIdNameByIds(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Product::getName,
                        (existing, replacement) -> existing)); // 处理可能的重复键

        // 4. 填充每个商机的产品名称
        responses.forEach(response -> {
            List<String> names = response.getProducts().stream()
                    .map(productNameMap::get)
                    .filter(Objects::nonNull) // 过滤掉null值
                    .toList();
            response.setProductNames(names);
        });

        return responses;
    }

}
