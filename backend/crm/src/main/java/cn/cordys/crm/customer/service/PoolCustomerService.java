package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.ChartAnalysisDbRequest;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseChartService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.*;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.customer.domain.*;
import cn.cordys.crm.customer.dto.CustomerPoolDTO;
import cn.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import cn.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import cn.cordys.crm.customer.dto.request.CustomerChartAnalysisDbRequest;
import cn.cordys.crm.customer.dto.request.CustomerPoolImportRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerChartAnalysisRequest;
import cn.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import cn.cordys.crm.customer.mapper.ExtCustomerCapacityMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerOwnerMapper;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.FilterConditionDTO;
import cn.cordys.crm.system.dto.RuleConditionDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.PoolBatchPickRequest;
import cn.cordys.crm.system.dto.request.PoolFreezeRequest;
import cn.cordys.crm.system.dto.request.PoolUnfreezeRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PoolCustomerService {

    public static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
    private static final String AUTO_UNFREEZE_REASON = "冻结到期，自动解冻";
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<CustomerOwner> ownerMapper;
    @Resource
    private BaseMapper<CustomerPool> poolMapper;
    @Resource
    private BaseMapper<CustomerPoolPickRule> pickRuleMapper;
    @Resource
    private BaseMapper<CustomerPoolRecycleRule> recycleRuleMapper;
    @Resource
    private ExtCustomerCapacityMapper extCustomerCapacityMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private LogService logService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private CustomerPoolService customerPoolService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private BaseChartService baseChartService;
    @Resource
    private ExtCustomerOwnerMapper extCustomerOwnerMapper;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;
    @Resource
    private BaseMapper<CustomerFieldBlob> customerFieldBlobMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private BaseService baseService;
    @Resource
    private ApprovalFlowService approvalFlowService;

    /**
     * 获取当前用户公海选项
     *
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     * @return 公海选项
     */
    public List<CustomerPoolDTO> getPoolOptions(String currentUser, String currentOrgId) {
        var options = new ArrayList<CustomerPoolDTO>();
        var poolWrapper = new LambdaQueryWrapper<CustomerPool>();
        poolWrapper.eq(CustomerPool::getEnable, true)
                .eq(CustomerPool::getOrganizationId, currentOrgId)
                .orderByDesc(CustomerPool::getUpdateTime);

        List<CustomerPool> pools = poolMapper.selectListByLambda(poolWrapper);
        if (CollectionUtils.isEmpty(pools)) {
            return options;
        }

        List<String> userIds = pools.stream()
                .flatMap(pool -> Stream.of(pool.getCreateUser(), pool.getUpdateUser())).toList();

        List<User> createOrUpdateUsers = userMapper.selectByIds(userIds.toArray(new String[0]));
        Map<String, String> userMap = createOrUpdateUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<String> poolIds = pools.stream()
                .map(CustomerPool::getId)
                .toList();
        var pickRuleWrapper = new LambdaQueryWrapper<CustomerPoolPickRule>();
        pickRuleWrapper.in(CustomerPoolPickRule::getPoolId, poolIds);

        List<CustomerPoolPickRule> pickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        Map<String, CustomerPoolPickRule> pickRuleMap = pickRules.stream()
                .collect(Collectors.toMap(CustomerPoolPickRule::getPoolId, pickRule -> pickRule));

        var recycleRuleWrapper = new LambdaQueryWrapper<CustomerPoolRecycleRule>();
        recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);

        List<CustomerPoolRecycleRule> recycleRules = recycleRuleMapper.selectListByLambda(recycleRuleWrapper);
        Map<String, CustomerPoolRecycleRule> recycleRuleMap = recycleRules.stream()
                .collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, recycleRule -> recycleRule));

        Map<String, List<CustomerPoolHiddenField>> hiddenFieldMap = customerPoolService
                .getCustomerPoolHiddenFieldByPoolIds(poolIds)
                .stream()
                .collect(Collectors.groupingBy(CustomerPoolHiddenField::getPoolId));

        List<BaseField> fields = moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), currentOrgId)
                .getFields();

        pools.forEach(pool -> {
            List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class),
                    currentOrgId);
            List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class),
                    currentOrgId);
            if (scopeIds.contains(currentUser) || ownerIds.contains(currentUser)
                    || Strings.CS.equals(currentUser, InternalUser.ADMIN.getValue())) {
                var poolDTO = new CustomerPoolDTO();
                BeanUtils.copyBean(poolDTO, pool);

                poolDTO.setMembers(userExtendService.getScope(JSON.parseArray(pool.getScopeId(), String.class)));
                poolDTO.setOwners(userExtendService.getScope(JSON.parseArray(pool.getOwnerId(), String.class)));
                poolDTO.setCreateUserName(userMap.get(pool.getCreateUser()));
                poolDTO.setUpdateUserName(userMap.get(pool.getUpdateUser()));

                var pickRule = new CustomerPoolPickRuleDTO();
                if (pickRuleMap.get(pool.getId()) != null) {
                    BeanUtils.copyBean(pickRule, pickRuleMap.get(pool.getId()));
                }
                var recycleRule = new CustomerPoolRecycleRuleDTO();
                CustomerPoolRecycleRule customerPoolRecycleRule = recycleRuleMap.get(pool.getId());
                if (customerPoolRecycleRule != null) {
                    BeanUtils.copyBean(recycleRule, customerPoolRecycleRule);
                    recycleRule.setConditions(
                            JSON.parseArray(customerPoolRecycleRule.getCondition(), RuleConditionDTO.class));
                }
                poolDTO.setPickRule(pickRule);
                poolDTO.setRecycleRule(recycleRule);
                poolDTO.setEditable(ownerIds.contains(currentUser));

                Set<String> hiddenFieldIds;
                if (hiddenFieldMap.get(pool.getId()) != null) {
                    hiddenFieldIds = hiddenFieldMap.get(pool.getId()).stream()
                            .map(CustomerPoolHiddenField::getFieldId)
                            .collect(Collectors.toSet());
                } else {
                    hiddenFieldIds = Set.of();
                }

                poolDTO.setFieldConfigs(customerPoolService.getFieldConfigs(fields, hiddenFieldIds));

                options.add(poolDTO);
            }
        });
        return options;
    }

    /**
     * 领取客户
     *
     * @param request      请求参数
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     */
    public void pick(PoolCustomerPickRequest request, String currentUser, String currentOrgId) {
        validateNotFrozen(request.getCustomerId());
        CustomerPool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        validateCapacity(1, currentUser, currentOrgId);
        var pickRuleWrapper = new LambdaQueryWrapper<CustomerPoolPickRule>();
        pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
        List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
        boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser,
                currentOrgId);
        if (!poolAdmin) {
            validateDailyPickNum(1, currentUser, pickRule);
        }
        ownCustomer(request.getCustomerId(), currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin);
    }

    /**
     * 分配客户
     *
     * @param id           客户ID
     * @param assignUserId 分配用户ID
     */
    public void assign(String id, String assignUserId, String currentOrgId, String currentUser) {
        validateNotFrozen(id);
        validateCapacity(1, assignUserId, currentOrgId);
        ownCustomer(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false);
    }

    /**
     * 删除客户（带审批校验）
     * <p>
     * 公海数据就是客户表里的一行, 审批同样走客户的 formType。
     * 引用校验必须赶在提审之前: 切面命中删除审批流时会直接提审并跳过方法体,
     * 校验若留在被拦截的方法里, 挂着商机/联系人的客户会先被提审、直到审批通过才报错, 白跑一趟审批。
     *
     * @param id     客户ID
     * @param userId 用户ID
     * @param orgId  组织ID
     */
    public void deleteWithApprovalCheck(String id, String userId, String orgId) {
        Objects.requireNonNull(CommonBeanFactory.getBean(CustomerService.class)).checkResourceRef(List.of(id));
        // 自调用不走代理, 需从容器取代理对象才能命中审批切面
        Objects.requireNonNull(CommonBeanFactory.getBean(PoolCustomerService.class)).deleteTriggerApproval(id, userId,
                orgId);
    }

    /**
     * 删除客户（审批入口, 命中删除审批流时由切面提审并跳过删除）
     *
     * @param id     客户ID
     * @param userId 用户ID
     * @param orgId  组织ID
     */
    @HitApproval(formKey = FormKey.CUSTOMER, executeType = ExecuteTimingEnum.DELETE, resourceId = "{#id}", operatorId = "{#userId}")
    public void deleteTriggerApproval(String id, String userId, String orgId) {
        // 自调用 bypass 代理, delete 上的 @OperationLog 不会生效; 从容器取代理对象才能记上删除日志
        Objects.requireNonNull(CommonBeanFactory.getBean(PoolCustomerService.class)).delete(id);
    }

    /**
     * 删除客户
     *
     * @param id 客户ID
     */
    @OperationLog(module = LogModule.CUSTOMER_POOL, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerService customerService = CommonBeanFactory.getBean(CustomerService.class);
        Objects.requireNonNull(customerService).checkResourceRef(List.of(id));
        customerService.deleteCustomerResource(List.of(id));

        // 设置操作对象
        OperationLogContext.setResourceName(customer.getName());
    }

    /**
     * 批量领取客户
     *
     * @param request      请求参数
     * @param currentUser  当前用户ID
     * @param currentOrgId 当前组织ID
     */
    public void batchPick(PoolBatchPickRequest request, String currentUser, String currentOrgId) {
        validateBatchNotFrozen(request.getBatchIds());
        CustomerPool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        validateCapacity(request.getBatchIds().size(), currentUser, currentOrgId);
        var pickRuleWrapper = new LambdaQueryWrapper<CustomerPoolPickRule>();
        pickRuleWrapper.eq(CustomerPoolPickRule::getPoolId, request.getPoolId());
        List<CustomerPoolPickRule> customerPoolPickRules = pickRuleMapper.selectListByLambda(pickRuleWrapper);
        CustomerPoolPickRule pickRule = customerPoolPickRules.getFirst();
        boolean poolAdmin = userExtendService.isPoolAdmin(JSON.parseArray(pool.getOwnerId(), String.class), currentUser,
                currentOrgId);
        if (!poolAdmin) {
            validateDailyPickNum(request.getBatchIds().size(), currentUser, pickRule);
        }
        request.getBatchIds().forEach(
                id -> ownCustomer(id, currentUser, pickRule, currentUser, LogType.PICK, currentOrgId, poolAdmin));
    }

    /**
     * 批量分配客户
     *
     * @param request      请求参数
     * @param assignUserId 分配用户ID
     * @param currentOrgId 当前组织ID
     */
    public void batchAssign(PoolBatchAssignRequest request, String assignUserId, String currentOrgId,
            String currentUser) {
        validateBatchNotFrozen(request.getBatchIds());
        validateCapacity(request.getBatchIds().size(), assignUserId, currentOrgId);
        request.getBatchIds()
                .forEach(id -> ownCustomer(id, assignUserId, null, currentUser, LogType.ASSIGN, currentOrgId, false));
    }

    /**
     * 批量删除客户
     *
     * @param ids    客户ID集合
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 删除结果: success 实际删除数, skip 已提交审批数, fail 不存在或无权操作数
     */
    public BatchAffectReasonResponse batchDelete(List<String> ids, String userId, String orgId) {
        List<Customer> customers = customerMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(customers)) {
            return BatchAffectReasonResponse.builder().success(0).fail(ids.size()).skip(0)
                    .errorMessages(Translator.get("customer.not.exist")).build();
        }
        // 状态权限校验: 与客户批量删除一致, 过滤出当前用户有权删除的客户。
        // 审批流的状态权限配置里登记的是 CUSTOMER_MANAGEMENT:*, 公海权限是另一个分组, 故按资源查客户权限、按角色校验公海权限
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CUSTOMER.getValue(),
                customers,
                PermissionConstants.CUSTOMER_MANAGEMENT_DELETE,
                orgId,
                Customer::getId,
                Customer::getApprovalStatus,
                PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE);
        int fail = customers.size() - permittedIds.size();
        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(fail).skip(0)
                    .errorMessages(Translator.get("no.operation.permission")).build();
        }

        // 引用校验要赶在提审之前: 进了审批的那些如果留到审批通过才校验, 审批人白跑一趟
        Objects.requireNonNull(CommonBeanFactory.getBean(CustomerService.class)).checkResourceRef(permittedIds);

        // 命中删除审批流的客户不直接删除, 走审批
        Map<String, String> nameMap = customers.stream()
                .filter(customer -> permittedIds.contains(customer.getId()))
                .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        List<String> approvalIds = CommonBeanFactory.getBean(ApprovalResourceService.class)
                .batchDeleteTriggerApproval(permittedIds, FormKey.CUSTOMER, orgId, userId, nameMap);
        List<String> deleteIds = permittedIds.stream().filter(id -> !approvalIds.contains(id)).toList();

        if (!CollectionUtils.isEmpty(deleteIds)) {
            Objects.requireNonNull(CommonBeanFactory.getBean(CustomerService.class)).deleteCustomerResource(deleteIds);

            List<LogDTO> logs = customers.stream()
                    .filter(customer -> deleteIds.contains(customer.getId()))
                    .map(customer -> new LogDTO(orgId, customer.getId(), userId, LogType.DELETE,
                            LogModule.CUSTOMER_POOL, customer.getName()))
                    .toList();
            logService.batchAdd(logs);
        }

        return BatchAffectReasonResponse.builder()
                .success(deleteIds.size()).fail(fail).skip(approvalIds.size())
                .errorMessages(Translator.get("batch.operate.reason")).build();
    }

    public void freeze(PoolFreezeRequest request, String userId, String orgId) {
        Customer customer = getPoolCustomer(request.getId(), orgId);
        long now = System.currentTimeMillis();
        if (isFrozen(customer, now)) {
            throw new GenericException(Translator.getWithArgs("pool.resource.already_frozen", customer.getName()));
        }
        unfreezeExpired(customer, now);

        Long unfreezeTime = request.isPermanentFreeze()
                ? null
                : now + request.getFreezeDays() * DAY_MILLIS;
        if (extCustomerMapper.freeze(customer.getId(), orgId, request.getReason(), unfreezeTime) == 0) {
            throw new GenericException(Translator.getWithArgs("pool.resource.already_frozen", customer.getName()));
        }
        addFreezeLog(customer, orgId, userId, LogType.FREEZE, request.getReason());
    }

    public void unfreeze(PoolUnfreezeRequest request, String userId, String orgId) {
        Customer customer = getPoolCustomer(request.getId(), orgId);
        long now = System.currentTimeMillis();
        if (!isFrozen(customer, now)) {
            if (unfreezeExpired(customer, now)) {
                return;
            }
            throw new GenericException(Translator.getWithArgs("pool.resource.not_frozen", customer.getName()));
        }
        if (extCustomerMapper.unfreeze(customer.getId(), orgId, now, false) == 0) {
            Customer current = customerMapper.selectByPrimaryKey(customer.getId());
            if (current != null && unfreezeExpired(current, System.currentTimeMillis())) {
                return;
            }
            throw new GenericException(Translator.getWithArgs("pool.resource.not_frozen", customer.getName()));
        }
        addFreezeLog(customer, orgId, userId, LogType.UNFREEZE, request.getReason());
    }

    public void unfreezeExpired() {
        long now = System.currentTimeMillis();
        extCustomerMapper.selectExpiredFrozen(now).forEach(customer -> unfreezeExpired(customer, now));
    }

    /**
     * 校验库容
     *
     * @param processCount 处理数量
     * @param ownUserId    负责人用户ID
     * @param currentOrgId 当前组织ID
     */
    public void validateCapacity(int processCount, String ownUserId, String currentOrgId) {
        // 实际可处理条数 = 负责人库容容量 - 所领取的数量(部分客户满足不计入条件的需排除) < 处理数量, 提示库容不足.
        CustomerCapacity customerCapacity = getUserCapacity(ownUserId, currentOrgId);
        if (customerCapacity == null || customerCapacity.getCapacity() == null) {
            return;
        }
        List<FilterConditionDTO> conditions = StringUtils.isEmpty(customerCapacity.getFilter()) ? new ArrayList<>()
                : JSON.parseArray(customerCapacity.getFilter(), FilterConditionDTO.class);
        conditions = conditions.stream().filter(condition -> StringUtils.isNotEmpty(condition.getColumn())
                && StringUtils.isNotEmpty(condition.getOperator()) && CollectionUtils.isNotEmpty(condition.getValue()))
                .toList();
        int filter = 0;
        if (CollectionUtils.isNotEmpty(conditions)) {
            filter = (int) extCustomerMapper.filterOwnerCount(ownUserId, conditions);
        }
        var customerWrapper = new LambdaQueryWrapper<Customer>();
        customerWrapper.eq(Customer::getOwner, ownUserId).eq(Customer::getInSharedPool, false);
        int ownCount = customerMapper.selectListByLambda(customerWrapper).size();
        if (customerCapacity.getCapacity() - (ownCount - filter) < processCount) {
            throw new GenericException(Translator.getWithArgs("customer.capacity.over",
                    Math.max(customerCapacity.getCapacity() - ownCount, 0)));
        }
    }

    /**
     * 校验每日领取数量
     *
     * @param pickingCount 领取数量
     * @param ownUserId    负责人用户ID
     * @param pickRule     领取规则
     */
    public void validateDailyPickNum(int pickingCount, String ownUserId, CustomerPoolPickRule pickRule) {
        if (pickRule.getLimitOnNumber()) {
            var customerWrapper = new LambdaQueryWrapper<Customer>();
            customerWrapper
                    .eq(Customer::getOwner, ownUserId)
                    .eq(Customer::getInSharedPool, false)
                    .between(Customer::getCollectionTime, TimeUtils.getTodayStart(),
                            TimeUtils.getTodayStart() + DAY_MILLIS);
            List<Customer> customers = customerMapper.selectListByLambda(customerWrapper);
            int pickedCount = customers.size();
            if (pickingCount + pickedCount > pickRule.getPickNumber()) {
                throw new GenericException(Translator.get("customer.daily.pick.over"));
            }
        }
    }

    /**
     * 获取用户库容
     *
     * @param userId         用户ID
     * @param organizationId 组织ID
     * @return 库容
     */
    public CustomerCapacity getUserCapacity(String userId, String organizationId) {
        List<String> scopeIds = userExtendService.getUserScopeIds(userId, organizationId);
        return extCustomerCapacityMapper.getCapacityByScopeIds(scopeIds, organizationId);
    }

    /**
     * 拥有客户
     *
     * @param customerId 客户ID
     * @param ownerId    拥有人ID
     */
    private void ownCustomer(String customerId, String ownerId, CustomerPoolPickRule pickRule,
            String operateUserId, String logType, String currentOrgId, boolean isPoolAdmin) {
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        if (customer == null) {
            throw new IllegalArgumentException(Translator.get("customer.not.exist"));
        }
        if (!customer.getInSharedPool()) {
            throw new GenericException(Translator.getWithArgs("customer.pool.occupied", customer.getName()));
        }
        validateNotFrozen(customer);

        if (!isPoolAdmin && pickRule != null) {
            if (pickRule.getLimitNew()) {
                LocalDateTime joinPoolTime = Instant.ofEpochMilli(customer.getUpdateTime())
                        .atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime releaseDate = joinPoolTime.plusDays(pickRule.getNewPickInterval());
                if (releaseDate.isAfter(LocalDateTime.now())) {
                    throw new GenericException(Translator.getWithArgs(
                            "pool.data.release.date",
                            releaseDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                }
            }

            if (pickRule.getLimitPreOwner()) {
                List<CustomerOwner> customerOwners = ownerMapper.selectListByLambda(
                        new LambdaQueryWrapper<CustomerOwner>().eq(CustomerOwner::getCustomerId, customerId));
                if (CollectionUtils.isNotEmpty(customerOwners)) {
                    CustomerOwner lastOwner = customerOwners.stream()
                            .max(Comparator.comparingLong(CustomerOwner::getCollectionTime))
                            .orElse(null);
                    if (lastOwner != null && Strings.CS.equals(lastOwner.getOwner(), ownerId)) {
                        long nextPickMillis = lastOwner.getEndTime()
                                + pickRule.getPickIntervalDays() * DAY_MILLIS;
                        if (System.currentTimeMillis() < nextPickMillis) {
                            LocalDateTime nextPickTime = Instant.ofEpochMilli(nextPickMillis)
                                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
                            throw new GenericException(Translator.getWithArgs(
                                    "customer.pre_owner.pick.limit",
                                    nextPickTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                        }
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        customer.setPoolId(null);
        customer.setInSharedPool(false);
        customer.setOwner(ownerId);
        customer.setCollectionTime(now);
        customer.setUpdateUser(ownerId);
        customer.setUpdateTime(now);
        if (extCustomerMapper.updateIncludeNullById(customer) == 0) {
            Customer current = customerMapper.selectByPrimaryKey(customerId);
            if (current != null && Boolean.TRUE.equals(current.getInSharedPool())) {
                validateNotFrozen(current);
            }
            throw new GenericException(Translator.getWithArgs("customer.pool.occupied", customer.getName()));
        }

        // 只更新最近一次销售负责人的联系人（联系人为空的）
        String recentOwner = extCustomerOwnerMapper.getRecentOwner(customerId);
        customerContactService.updatePoolContactOwner(customerId, ownerId, recentOwner, currentOrgId);

        logService.add(new LogDTO(currentOrgId, customer.getId(), operateUserId, logType,
                LogModule.CUSTOMER_POOL, customer.getName()));

        if (Strings.CS.equals(logType, LogType.ASSIGN)) {
            commonNoticeSendService.sendNotice(
                    NotificationConstants.Module.CUSTOMER,
                    NotificationConstants.Event.HIGH_SEAS_CUSTOMER_DISTRIBUTED,
                    customer.getName(), operateUserId, currentOrgId,
                    List.of(ownerId), true);
        }
    }

    /**
     * 校验当前用户是否为公海成员（成员或管理员均可访问）
     *
     * @param poolId 公海ID
     * @param userId 当前用户ID
     * @param orgId  组织ID
     */
    public void checkPoolMember(String poolId, String userId, String orgId) {
        CustomerPool pool = poolMapper.selectByPrimaryKey(poolId);
        if (pool == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND, Translator.get("customer_pool_not_exist"));
        }
        if (!isPoolMember(pool, userId, orgId)) {
            throw new GenericException(Translator.get("customer_pool_member_access_fail"));
        }
    }

    /**
     * 判断当前用户是否可访问当前组织的公海，用于无权限时降级展示关联记录。
     *
     * @param poolId 公海ID
     * @param userId 当前用户ID
     * @param orgId  当前组织ID
     * @return 公海属于当前组织，且当前用户为成员或管理员时返回 true
     */
    public boolean hasPoolMember(String poolId, String userId, String orgId) {
        CustomerPool pool = poolMapper.selectByPrimaryKey(poolId);
        return pool != null && orgId != null && orgId.equals(pool.getOrganizationId())
                && isPoolMember(pool, userId, orgId);
    }

    private boolean isPoolMember(CustomerPool pool, String userId, String orgId) {
        List<String> scopeIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getScopeId(), String.class),
                orgId);
        List<String> ownerIds = userExtendService.getScopeOwnerIds(JSON.parseArray(pool.getOwnerId(), String.class),
                orgId);
        return scopeIds.contains(userId) || ownerIds.contains(userId)
                || Strings.CS.equals(userId, InternalUser.ADMIN.getValue());
    }

    /**
     * 根据客户ID获取所属公海ID
     *
     * @param customerId 客户ID
     * @return 公海ID
     */
    public String getPoolIdByCustomerId(String customerId) {
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        if (customer == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND, Translator.get("customer.not.exist"));
        }
        return customer.getPoolId();
    }

    private Customer getPoolCustomer(String id, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        if (customer == null || !Objects.equals(customer.getOrganizationId(), orgId)) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND, Translator.get("customer.not.exist"));
        }
        if (!Boolean.TRUE.equals(customer.getInSharedPool())) {
            throw new GenericException(Translator.getWithArgs("customer.pool.occupied", customer.getName()));
        }
        return customer;
    }

    private void validateNotFrozen(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        if (customer == null) {
            throw new IllegalArgumentException(Translator.get("customer.not.exist"));
        }
        validateNotFrozen(customer);
    }

    private void validateNotFrozen(Customer customer) {
        long now = System.currentTimeMillis();
        if (isFrozen(customer, now)) {
            throw new GenericException(Translator.getWithArgs("pool.resource.frozen", customer.getName()));
        }
        unfreezeExpired(customer, now);
    }

    private void validateBatchNotFrozen(List<String> ids) {
        Map<String, Customer> customerMap = customerMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        for (String id : ids) {
            if (!customerMap.containsKey(id)) {
                throw new IllegalArgumentException(Translator.get("customer.not.exist"));
            }
        }

        long now = System.currentTimeMillis();
        Map<String, String> messageDetail = new LinkedHashMap<>();
        ids.forEach(id -> {
            Customer customer = customerMap.get(id);
            if (isFrozen(customer, now)) {
                messageDetail.put(id, Translator.getWithArgs("pool.resource.frozen", customer.getName()));
            } else {
                unfreezeExpired(customer, now);
            }
        });
        if (!messageDetail.isEmpty()) {
            throw new GenericException(CrmHttpResultCode.VALIDATE_FAILED, messageDetail);
        }
    }

    private boolean isFrozen(Customer customer, long now) {
        return Boolean.TRUE.equals(customer.getFrozen())
                && (customer.getUnfreezeTime() == null || customer.getUnfreezeTime() > now);
    }

    private boolean unfreezeExpired(Customer customer, long now) {
        if (!Boolean.TRUE.equals(customer.getFrozen()) || customer.getUnfreezeTime() == null
                || customer.getUnfreezeTime() > now) {
            return false;
        }
        if (extCustomerMapper.unfreeze(customer.getId(), customer.getOrganizationId(), now, true) == 0) {
            return false;
        }
        addFreezeLog(customer, customer.getOrganizationId(), InternalUser.ADMIN.getValue(),
                LogType.UNFREEZE, AUTO_UNFREEZE_REASON);
        return true;
    }

    private void addFreezeLog(Customer customer, String orgId, String userId, String type, String reason) {
        LogDTO log = new LogDTO(orgId, customer.getId(), userId, type, LogModule.CUSTOMER_POOL, customer.getName());
        log.setDetail(StringUtils.trimToNull(reason));
        logService.add(log);
    }

    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId,
            String organizationId) {
        BaseField field = customerFieldService.getAndCheckField(request.getFieldId(), organizationId);
        List<Customer> originCustomers = customerMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originCustomers)) {
            return BatchAffectReasonResponse.builder().success(0).fail(request.getIds().size()).skip(0)
                    .errorMessages(Translator.get("customer.not.exist")).build();
        }
        // 状态权限校验: 与客户批量编辑一致。放在负责人分支之前, 否则处于无权编辑状态(如审批中)的数据会从那条路径绕过去。
        // 审批流的状态权限配置里登记的是 CUSTOMER_MANAGEMENT:*, 公海权限是另一个分组, 故按资源查客户权限、按角色校验公海权限
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CUSTOMER.getValue(),
                originCustomers,
                PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                organizationId,
                Customer::getId,
                Customer::getApprovalStatus,
                PermissionConstants.CUSTOMER_MANAGEMENT_POOL_UPDATE);
        int fail = originCustomers.size() - permittedIds.size();
        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(fail).skip(0)
                    .errorMessages(Translator.get("no.operation.permission")).build();
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
            // 修改负责人就是一次分配, 分配不接审批, 走批量分配的接口
            var batchAssignRequest = new PoolBatchAssignRequest();
            batchAssignRequest.setBatchIds(permittedIds);
            batchAssignRequest.setAssignUserId(request.getFieldValue().toString());
            batchAssign(batchAssignRequest, batchAssignRequest.getAssignUserId(), organizationId, userId);
            return BatchAffectReasonResponse.builder().success(permittedIds.size()).fail(fail).skip(0)
                    .errorMessages(Translator.get("batch.update.reason")).build();
        }

        // 快照须在写库前落库, 否则审批驳回/撤回时回退到的是改后的值
        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());
        List<Customer> permittedCustomers = originCustomers.stream()
                .filter(customer -> permittedIds.contains(customer.getId()))
                .toList();

        CommonBeanFactory.getBean(ApprovalResourceService.class)
                .batchEditTriggerApproval(permittedIds, request.getFieldId(), FormKey.CUSTOMER, organizationId, userId,
                        field.getName(), request.getFieldValue());
        customerFieldService.batchUpdate(filteredRequest, field, permittedCustomers, Customer.class,
                LogModule.CUSTOMER_POOL, extCustomerMapper::batchUpdate, userId, organizationId, true);

        return BatchAffectReasonResponse.builder().success(permittedIds.size()).fail(fail).skip(0)
                .errorMessages(Translator.get("batch.update.reason")).build();
    }

    public List<ChartResult> chart(PoolCustomerChartAnalysisRequest request, String userId, String orgId,
            DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = Objects.requireNonNull(CommonBeanFactory.getBean(CustomerService.class))
                .getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request,
                formConfig);
        CustomerChartAnalysisDbRequest customerChartAnalysisDbRequest = BeanUtils
                .copyBean(new CustomerChartAnalysisDbRequest(), chartAnalysisDbRequest);
        customerChartAnalysisDbRequest.setPoolId(request.getPoolId());
        List<ChartResult> chartResults = extCustomerMapper.chart(customerChartAnalysisDbRequest, userId, orgId,
                deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }

    /**
     * 下载导入模板
     */
    public void downloadImportTpl(HttpServletResponse response, String orgId) {
        List<List<String>> headList = moduleFormService.getCustomImportHeadsNoRefAndOwner(FormKey.CUSTOMER.getKey(),
                orgId);
        new EasyExcelExporter().exportMultiSheetTplWithSharedHandler(response,
                headList,
                Translator.get("customer_pool.import_tpl.name"), Translator.get(SheetKey.DATA),
                Translator.get(SheetKey.COMMENT),
                new CustomTemplateWriteHandler(
                        moduleFormService.getAllCustomImportFieldsNoOwner(FormKey.CUSTOMER.getKey(), orgId)),
                new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入校验
     */
    public ImportResponse importPreCheck(MultipartFile file, CustomerPoolImportRequest request, String orgId) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        CustomerPool pool = poolMapper.selectByPrimaryKey(request.getPoolId());
        if (pool == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND, Translator.get("customer_pool_not_exist"));
        }
        return checkImportExcel(file, request.getImportType(), orgId);
    }

    private ImportResponse checkImportExcel(MultipartFile file, String importType, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CUSTOMER.getKey(), currentOrg);
            fields.removeIf(baseField -> Strings.CI.equals(baseField.getBusinessKey(),
                    BusinessModuleField.CUSTOMER_OWNER.getBusinessKey()));
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "customer",
                    "customer_field", currentOrg, importType);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet()
                    .doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer pool import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 导入
     */
    public ImportResponse realImport(MultipartFile file, CustomerPoolImportRequest request, String currentOrg,
            String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CUSTOMER.getKey(), currentOrg);
            fields.removeIf(baseField -> Strings.CI.equals(baseField.getBusinessKey(),
                    BusinessModuleField.CUSTOMER_OWNER.getBusinessKey()));
            CustomImportAfterDoConsumer<Customer, BaseResourceSubField> afterDo = (customers, customerFields,
                    customerFieldBlobs) -> {
                var logs = new ArrayList<LogDTO>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                case ADD -> {
                    customers.forEach(customer -> {
                        customer.setInSharedPool(true);
                        customer.setPoolId(request.getPoolId());
                        customer.setFrozen(false);
                        customer.setApprovalStatus(ApprovalStatus.NONE.name());
                        logs.add(new LogDTO(currentOrg, customer.getId(), currentUser, LogType.ADD,
                                LogModule.CUSTOMER_POOL, customer.getName()));
                    });
                    customerMapper.batchInsert(customers);
                    customerFieldMapper.batchInsert(customerFields.stream()
                            .map(field -> BeanUtils.copyBean(new CustomerField(), field)).toList());
                    customerFieldBlobMapper.batchInsert(customerFieldBlobs.stream()
                            .map(field -> BeanUtils.copyBean(new CustomerFieldBlob(), field)).toList());
                    // record logs
                    logService.batchAdd(logs);
                }
                case UPDATE -> {
                    List<String> ids = customers.stream().map(Customer::getId).toList();
                    if (CollectionUtils.isEmpty(ids)) {
                        break;
                    }
                    //原数据
                    List<Customer> originCustomerList = customerMapper.selectByIds(ids);
                    if (CollectionUtils.isEmpty(originCustomerList)) {
                        break;
                    }
                    Map<String, Customer> originCustomerMaps = originCustomerList.stream()
                            .collect(Collectors.toMap(Customer::getId, Function.identity()));
                    Map<String, List<BaseModuleFieldValue>> originFieldValueMap = customerFieldService
                            .getResourceFieldMap(ids, true);

                    List<CustomerField> insertField = new ArrayList<>();
                    List<CustomerFieldBlob> insertFieldBlob = new ArrayList<>();
                    SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                    ExtCustomerMapper customerBatchMapper = sqlSession.getMapper(ExtCustomerMapper.class);
                    CommonMapper commonMapper = sqlSession.getMapper(CommonMapper.class);

                    if (CollectionUtils.isNotEmpty(customers)) {
                        customers.forEach(customer -> {
                            customer.setInSharedPool(true);
                            customer.setPoolId(request.getPoolId());
                            customerBatchMapper.updateCustomer(customer);
                        });
                    }

                    if (CollectionUtils.isNotEmpty(customerFields)) {
                        List<CustomerField> fieldList = customerFieldMapper
                                .selectByIds(customerFields.stream().map(BaseResourceSubField::getId).toList());
                        Map<String, CustomerField> fieldMap = fieldList.stream()
                                .collect(Collectors.toMap(CustomerField::getId, Function.identity()));
                        customerFields.forEach(customerField -> {
                            if (fieldMap.containsKey(customerField.getId())) {
                                commonMapper.updateCustomerField("customer_field", customerField);
                            } else {
                                insertField.add(BeanUtils.copyBean(new CustomerField(), customerField));
                            }
                        });
                    }

                    if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
                        List<CustomerFieldBlob> blobList = customerFieldBlobMapper
                                .selectByIds(customerFieldBlobs.stream().map(BaseResourceSubField::getId).toList());
                        Map<String, CustomerFieldBlob> blobMap = blobList.stream()
                                .collect(Collectors.toMap(CustomerFieldBlob::getId, Function.identity()));
                        customerFieldBlobs.forEach(customerFieldBlob -> {
                            if (blobMap.containsKey(customerFieldBlob.getId())) {
                                commonMapper.updateCustomerField("customer_field_blob", customerFieldBlob);
                            } else {
                                insertFieldBlob.add(BeanUtils.copyBean(new CustomerFieldBlob(), customerFieldBlob));
                            }
                        });
                    }

                    sqlSession.flushStatements();
                    SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

                    if (CollectionUtils.isNotEmpty(insertField)) {
                        customerFieldMapper.batchInsert(insertField);
                    }
                    if (CollectionUtils.isNotEmpty(insertFieldBlob)) {
                        customerFieldBlobMapper.batchInsert(insertFieldBlob);
                    }

                    SqlSession currentSession = SqlSessionUtils.getSqlSession(sqlSessionFactory);
                    currentSession.clearCache();

                    Map<String, Customer> modifiedCustomerMaps = customerMapper.selectByIds(ids).stream()
                            .collect(Collectors.toMap(Customer::getId, Function.identity()));
                    Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = customerFieldService
                            .getResourceFieldMap(ids, true);

                    ids.forEach(id -> {
                        Customer originDate = originCustomerMaps.get(id);
                        Customer modifiedDate = modifiedCustomerMaps.get(id);
                        baseService.handleUpdateLog(originDate, modifiedDate, originFieldValueMap.get(id),
                                modifiedFieldValueMap.get(id), id, modifiedDate.getName());
                        LogContextInfo contextInfo = OperationLogContext.getContext();
                        if (contextInfo != null) {
                            LogDTO logDTO = new LogDTO(currentOrg, id, currentUser, LogType.UPDATE,
                                    LogModule.CUSTOMER_POOL, modifiedDate.getName());
                            logDTO.setOriginalValue(contextInfo.getOriginalValue());
                            logDTO.setModifiedValue(contextInfo.getModifiedValue());
                            logs.add(logDTO);
                            OperationLogContext.clear();
                        }
                    });
                    logService.batchAdd(logs);
                }
                }
            };
            CustomFieldImportEventListener<Customer> eventListener = new CustomFieldImportEventListener<>(fields,
                    Customer.class, currentOrg, currentUser,
                    "customer_field", "customer_field_blob", afterDo, 2000, null, null, request.getImportType());
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet()
                    .doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer import error: {}", e.getMessage());
            throw new GenericException("导入异常，请检查文件数据！" + e);
        }
    }
}
