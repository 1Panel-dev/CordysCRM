package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.constants.CustomerResultCode;
import cn.cordys.crm.customer.domain.*;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
import cn.cordys.crm.system.dto.response.ImportResponse;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.excel.CustomImportAfterDoConsumer;
import cn.cordys.crm.system.excel.handler.CustomHeadColWidthStyleStrategy;
import cn.cordys.crm.system.excel.handler.CustomTemplateWriteHandler;
import cn.cordys.crm.system.excel.listener.CustomFieldCheckEventListener;
import cn.cordys.crm.system.excel.listener.CustomFieldImportEventListener;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.*;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerService {

    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private CustomerFieldService customerFieldService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private CustomerOwnerHistoryService customerOwnerHistoryService;
    @Resource
    private CustomerPoolService customerPoolService;
    @Resource
    private BaseMapper<CustomerPoolRecycleRule> customerPoolRecycleRuleMapper;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CustomerRelationService customerRelationService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private LogService logService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private ExtCustomerPoolMapper extCustomerPoolMapper;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private DictService dictService;
    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;
    @Resource
    private BaseMapper<CustomerFieldBlob> customerFieldBlobMapper;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtFollowUpRecordMapper extFollowUpRecordMapper;
    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;

    public PagerWithOption<List<CustomerListResponse>> list(CustomerPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId, userId, deptDataPermission);
        List<CustomerListResponse> buildList = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public PagerWithOption<List<CustomerListResponse>> transitionList(CustomerPageRequest request, String userId, String orgId) {
        /*
         * 数据范围: 当前用户所在公海&所有私海客户
         */
        List<String> scopeIds = userExtendService.getUserScopeIds(userId, orgId);
        List<CustomerPool> pools = extCustomerPoolMapper.getPoolByScopeIds(scopeIds, orgId);
        request.setTransitionPoolIds(pools.stream().map(CustomerPool::getId).toList());
        request.setTransition(true);
        return list(request, userId, orgId, null);
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<CustomerListResponse> list, List<CustomerListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
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

    private ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
    }

    public PagerWithOption<List<CustomerListResponse>> sourceList(CustomerPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerListResponse> list = extCustomerMapper.sourceList(request, orgId, userId, deptDataPermission);
        List<CustomerListResponse> buildList = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public List<CustomerListResponse> buildListData(List<CustomerListResponse> list, String orgId) {
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

        list.forEach(customerListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> customerFields = caseCustomFiledMap.get(customerListResponse.getId());
            customerListResponse.setModuleFields(customerFields);
            // 设置回收公海
            CustomerPool reservePool = ownersDefaultPoolMap.get(customerListResponse.getOwner());
            customerListResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            customerListResponse.setReservedDays(customerPoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    customerListResponse.getCollectionTime(), customerListResponse.getCreateTime()));

            UserDeptDTO userDeptDTO = userDeptMap.get(customerListResponse.getOwner());
            if (userDeptDTO != null) {
                customerListResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

            if (StringUtils.isNotEmpty(customerListResponse.getFollower())) {
                String followerName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getFollower()));
                customerListResponse.setFollowerName(followerName);
            }
            String createUserName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getCreateUser()));
            customerListResponse.setCreateUserName(createUserName);
            String updateUserName = baseService.getAndCheckOptionName(userNameMap.get(customerListResponse.getUpdateUser()));
            customerListResponse.setUpdateUserName(updateUserName);
            customerListResponse.setOwnerName(userNameMap.get(customerListResponse.getOwner()));
            if (StringUtils.isNotBlank(customerListResponse.getReasonId())) {
                String reasonName = baseService.getAndCheckOptionName(dictMap.get(customerListResponse.getReasonId()));
                customerListResponse.setReasonName(reasonName);
            }
        });

        return list;
    }

    public CustomerGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        CustomerGetResponse getResponse = get(id, orgId);

        boolean hasPermission = dataScopeService.hasDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_READ);
        if (!hasPermission) {
            List<CustomerCollaboration> collaborations = customerCollaborationService.selectByCustomerIdAndUserId(getResponse.getId(), userId);
            if (CollectionUtils.isEmpty(collaborations)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            } else {
                getResponse.setCollaborationType(collaborations.getFirst().getCollaborationType());
            }
        }

        return getResponse;
    }

    public CustomerGetResponse get(String id, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);
        customerGetResponse = baseService.setCreateUpdateOwnerUserName(customerGetResponse);
        // 获取模块字段
        List<BaseModuleFieldValue> customerFields = customerFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, customerFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(customerGetResponse,
                CustomerGetResponse::getOwner, CustomerGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        customerGetResponse.setOptionMap(optionMap);
        customerGetResponse.setModuleFields(customerFields);

        // 公海原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CUSTOMER_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        if (customerGetResponse.getOwner() != null) {
            // 获取负责人默认公海信息
            Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(List.of(customerGetResponse.getOwner()), orgId);
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

            // 设置回收公海
            CustomerPool reservePool = ownersDefaultPoolMap.get(customerGetResponse.getOwner());
            customerGetResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            customerGetResponse.setReservedDays(customerPoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    customerGetResponse.getCollectionTime(), customerGetResponse.getCreateTime()));


            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(customerGetResponse.getOwner(), orgId);
            if (userDeptDTO != null) {
                customerGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                customerGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        if (customerGetResponse.getFollower() != null) {
            Map<String, String> userNameMap = baseService.getUserNameMap(List.of(customerGetResponse.getFollower()));
            String followerName = baseService.getAndCheckOptionName(userNameMap.get(customerGetResponse.getFollower()));
            customerGetResponse.setFollowerName(followerName);
        }

        if (StringUtils.isNotBlank(customerGetResponse.getReasonId())) {
            String reasonName = baseService.getAndCheckOptionName(dictMap.get(customerGetResponse.getReasonId()));
            customerGetResponse.setReasonName(reasonName);
        }

        // 附件信息
        customerGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, customerFields));

        return customerGetResponse;
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
    public Customer add(CustomerAddRequest request, String userId, String orgId) {
        Customer customer = BeanUtils.copyBean(new Customer(), request);
        if (StringUtils.isBlank(request.getOwner())) {
            customer.setOwner(userId);
        }
        poolCustomerService.validateCapacity(1, customer.getOwner(), orgId);
        customer.setCreateTime(System.currentTimeMillis());
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setCollectionTime(customer.getCreateTime());
        customer.setUpdateUser(userId);
        customer.setCreateUser(userId);
        customer.setOrganizationId(orgId);
        customer.setId(IDGenerator.nextStr());
        customer.setInSharedPool(false);

        //保存自定义字段
        customerFieldService.saveModuleField(customer, orgId, userId, request.getModuleFields(), false);

        customerMapper.insert(customer);

        baseService.handleAddLog(customer, request.getModuleFields());
        // 通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_ADD, customer.getName(), userId,
                orgId, List.of(customer.getOwner()), true);
        return customer;
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Customer update(CustomerUpdateRequest request, String userId, String orgId) {
        Customer originCustomer = customerMapper.selectByPrimaryKey(request.getId());
        if (!Strings.CS.equals(originCustomer.getOwner(), request.getOwner())) {
            poolCustomerService.validateCapacity(1, request.getOwner(), orgId);
        }
        dataScopeService.checkDataPermission(userId, orgId, originCustomer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);

        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!Strings.CS.equals(request.getOwner(), originCustomer.getOwner())) {
                //客户负责人变更，联系人同步更新
                customerContactService.updateContactOwner(request.getId(), request.getOwner(), originCustomer.getOwner(), orgId);


                // 如果责任人有修改，则添加责任人历史
                customerOwnerHistoryService.add(originCustomer, userId, false);
                sendTransferNotice(List.of(originCustomer), request.getOwner(), userId, orgId);
                // 重置领取时间
                customer.setCollectionTime(System.currentTimeMillis());
            }
        }

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = customerFieldService.getModuleFieldValuesByResourceId(request.getId());

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            customerFieldService.updateModuleFieldByAgent(customer, originCustomerFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(customer, request.getModuleFields(), orgId, userId);
        }

        customerMapper.update(customer);

        customer = customerMapper.selectByPrimaryKey(request.getId());
        baseService.handleUpdateLog(originCustomer, customer, originCustomerFields, request.getModuleFields(), originCustomer.getId(), originCustomer.getName());
        return customer;
    }

    private void updateModuleField(Customer customer, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        customerFieldService.deleteByResourceId(customer.getId());
        // 再保存
        customerFieldService.saveModuleField(customer, orgId, userId, moduleFields, true);
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Customer originCustomer = customerMapper.selectByPrimaryKey(id);
        dataScopeService.checkDataPermission(userId, orgId, originCustomer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_DELETE);
        checkResourceRef(List.of(id));
        deleteCustomerResource(List.of(id));

        // 设置操作对象
        OperationLogContext.setResourceName(originCustomer.getName());

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_DELETED, originCustomer.getName(), userId,
                orgId, List.of(originCustomer.getOwner()), true);
    }

    public void batchTransfer(CustomerBatchTransferRequest request, String userId, String orgId) {
        List<Customer> originCustomers = customerMapper.selectByIds(request.getIds());
        List<String> owners = getOwners(originCustomers);
        long processCount = originCustomers.stream().filter(customer -> !Strings.CS.equals(customer.getOwner(), request.getOwner())).count();
        poolCustomerService.validateCapacity((int) processCount, request.getOwner(), orgId);

        dataScopeService.checkDataPermission(userId, orgId, owners, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
        // 添加责任人历史
        customerOwnerHistoryService.batchAdd(request, userId);
        extCustomerMapper.batchTransfer(request, userId);

        // 记录日志
        List<LogDTO> logs = originCustomers.stream()
                .map(customer -> {
                    Customer originCustomer = new Customer();
                    originCustomer.setOwner(customer.getOwner());
                    Customer modifieCustomer = new Customer();
                    modifieCustomer.setOwner(request.getOwner());
                    LogDTO logDTO = new LogDTO(orgId, customer.getId(), userId, LogType.UPDATE, LogModule.CUSTOMER_INDEX, customer.getName());
                    logDTO.setOriginalValue(originCustomer);
                    logDTO.setModifiedValue(modifieCustomer);
                    return logDTO;
                }).toList();

        logService.batchAdd(logs);

        sendTransferNotice(originCustomers, request.getOwner(), userId, orgId);
    }

    private void sendTransferNotice(List<Customer> originCustomers, String toUser, String userId, String orgId) {
        originCustomers.forEach(customer -> commonNoticeSendService.sendNotice(
                NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_TRANSFERRED_CUSTOMER,
                customer.getName(),
                userId,
                orgId,
                List.of(toUser),
                true
        ));
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<Customer> customers = customerMapper.selectByIds(ids);
        List<String> owners = getOwners(customers);
        dataScopeService.checkDataPermission(userId, orgId, owners, PermissionConstants.CUSTOMER_MANAGEMENT_DELETE);

        checkResourceRef(ids);

        deleteCustomerResource(ids);

        List<LogDTO> logs = customers.stream()
                .map(customer ->
                        new LogDTO(orgId, customer.getId(), userId, LogType.DELETE, LogModule.CUSTOMER_INDEX, customer.getName())
                )
                .toList();
        logService.batchAdd(logs);

        // 消息通知
        customers.forEach(customer ->
                commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                        NotificationConstants.Event.CUSTOMER_DELETED, customer.getName(), userId,
                        orgId, List.of(customer.getOwner()), true)
        );
    }

    public void deleteCustomerResource(List<String> ids) {
        // 删除客户
        customerMapper.deleteByIds(ids);
        // 删除客户模块字段
        customerFieldService.deleteByResourceIds(ids);
        // 删除客户协作人
        customerCollaborationService.deleteByCustomerIds(ids);
        // 删除责任人历史
        customerOwnerHistoryService.deleteByCustomerIds(ids);
        // 删除客户关系
        customerRelationService.deleteByCustomerIds(ids);
        // 删除跟进记录
        followUpRecordService.deleteByCustomerIds(ids);
        // 删除跟进计划
        followUpPlanService.deleteByCustomerIds(ids);
    }

    public void checkResourceRef(List<String> ids) {
        if (extCustomerMapper.hasRefOpportunity(ids) || extCustomerMapper.hasRefContact(ids)) {
            throw new GenericException(CustomerResultCode.CUSTOMER_RESOURCE_REF);
        }
    }

    private List<String> getOwners(List<Customer> customers) {
        return customers.stream().map(Customer::getOwner)
                .distinct()
                .toList();
    }

    /**
     * 批量移入公海
     *
     * @param request     请求参数
     * @param orgId       组织ID
     * @param currentUser 当前用户
     */
    public BatchAffectResponse batchToPool(BatchPoolReasonRequest request, String currentUser, String orgId) {
        List<Customer> customers = customerMapper.selectByIds(request.getIds());
        List<String> owners = getOwners(customers);
        dataScopeService.checkDataPermission(currentUser, orgId, owners, PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE);

        List<String> ownerIds = getOwners(customers);
        Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, orgId);

        int success = 0;
        List<LogDTO> logs = new ArrayList<>();
        for (Customer customer : customers) {
            CustomerPool customerPool = ownersDefaultPoolMap.get(customer.getOwner());
            if (customerPool == null) {
                // 未找到默认公海，不移入
                continue;
            }
            //更新责任人
            customerContactService.updateContactOwner(customer.getId(), "-", customer.getOwner(), orgId);


            // 日志
            LogDTO logDTO = new LogDTO(orgId, customer.getId(), currentUser, LogType.MOVE_TO_CUSTOMER_POOL, LogModule.CUSTOMER_INDEX, customer.getName());
            String detail = Translator.getWithArgs("customer.to.pool", customer.getName(),
                    customerPool.getName());
            logDTO.setDetail(detail);
            logs.add(logDTO);
            // 消息通知
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                    NotificationConstants.Event.CUSTOMER_MOVED_HIGH_SEAS, customer.getName(), currentUser,
                    orgId, List.of(customer.getOwner()), true);
            // 插入责任人历史
            customer.setReasonId(request.getReasonId());
            customerOwnerHistoryService.add(customer, currentUser, true);
            customer.setPoolId(customerPool.getId());
            customer.setInSharedPool(true);
            customer.setOwner(null);
            customer.setCollectionTime(null);
            customer.setUpdateUser(currentUser);
            customer.setUpdateTime(System.currentTimeMillis());
            // 回收客户至公海
            extCustomerMapper.moveToPool(customer);
            success++;
        }

        logService.batchAdd(logs);

        return BatchAffectResponse.builder().success(success).fail(request.getIds().size() - success).build();
    }

    /**
     * 移入公海
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public BatchAffectResponse toPool(PoolReasonRequest request, String currentUser, String orgId) {
        BatchPoolReasonRequest batchRequest = new BatchPoolReasonRequest();
        batchRequest.setReasonId(request.getReasonId());
        batchRequest.setIds(List.of(request.getId()));
        return batchToPool(batchRequest, currentUser, orgId);
    }

    public List<OptionDTO> getCustomerOptions(String keyword, String organizationId) {
        return extCustomerMapper.getCustomerOptions(keyword, organizationId);
    }

    public String getCustomerName(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(customer).map(Customer::getName).orElse(null);
    }

    public List<Customer> getCustomerListByNames(List<String> names) {
        LambdaQueryWrapper<Customer> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Customer::getName, names);
        return customerMapper.selectListByLambda(lambdaQueryWrapper);
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CUSTOMER_MANAGEMENT_READ, rolePermissions);
    }


    public String getCustomerNameByIds(List<String> ids) {
        List<Customer> customerList = customerMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(customerList)) {
            List<String> names = customerList.stream().map(Customer::getName).toList();
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        // 客户表单字段
        List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CUSTOMER.getKey(), currentOrg);

        new EasyExcelExporter(Objects.class)
                .exportMultiSheetTplWithSharedHandler(response, fields.stream().map(field -> Collections.singletonList(field.getName())).toList(),
                        Translator.get("customer.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT), new CustomTemplateWriteHandler(fields), new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入检查
     *
     * @param file       导入文件
     * @param currentOrg 当前组织
     *
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, currentOrg);
    }

    /**
     * 客户导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     *
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CUSTOMER.getKey(), currentOrg);
            CustomImportAfterDoConsumer<Customer, BaseResourceField> afterDo = (customers, customerFields, customerFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                customers.forEach(customer -> {
                    customer.setCollectionTime(customer.getCreateTime());
                    customer.setInSharedPool(false);
                    logs.add(new LogDTO(currentOrg, customer.getId(), currentUser, LogType.ADD, LogModule.CUSTOMER_INDEX, customer.getName()));
                });
                customerMapper.batchInsert(customers);
                customerFieldMapper.batchInsert(customerFields.stream().map(field -> BeanUtils.copyBean(new CustomerField(), field)).toList());
                customerFieldBlobMapper.batchInsert(customerFieldBlobs.stream().map(field -> BeanUtils.copyBean(new CustomerFieldBlob(), field)).toList());
                // record logs
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<Customer, CustomerField> eventListener = new CustomFieldImportEventListener<>(fields, Customer.class, currentOrg, currentUser,
                    customerFieldMapper, afterDo, 2000);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getDataList().size()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("customer import error: ", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 检查导入的文件
     *
     * @param file       文件
     * @param currentOrg 当前组织
     *
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CUSTOMER.getKey(), currentOrg);
            CustomFieldCheckEventListener<CustomerField> eventListener = new CustomFieldCheckEventListener<>(fields, "customer", currentOrg, customerFieldMapper);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("customer import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public void batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = customerFieldService.getAndCheckField(request.getFieldId(), organizationId);

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            CustomerBatchTransferRequest batchTransferRequest = new CustomerBatchTransferRequest();
            batchTransferRequest.setIds(request.getIds());
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            batchTransfer(batchTransferRequest, userId, organizationId);
            return;
        }

        List<Customer> originCustomers = customerMapper.selectByIds(request.getIds());

        customerFieldService.batchUpdate(request, field, originCustomers, Customer.class, LogModule.CUSTOMER_INDEX, extCustomerMapper::batchUpdate, userId, organizationId);
    }

    /**
     *
     * @param request 合并请求参数
     * @param currentUser 当前用户
     * @param currentOrgId 当前组织ID
     */
    public void merge(CustomerMergeRequest request, String currentUser, String currentOrgId) {
        /*
         * 规则:
         * 1. 合并客户联系人, 客户关联商机.
         * 3. 合并客户跟进记录/计划.
         * 4. 删除被合并的客户, 并添加对应的负责人为合并客户的协作人.
         */
        request.getMergeIds().remove(request.getToMergeId());
        if (CollectionUtils.isEmpty(request.getMergeIds())) {
            // 没有可合并的客户数据
            throw new GenericException(Translator.get("no.customer.merge.data"));
        }
        extCustomerContactMapper.batchMerge(request, currentUser, currentOrgId);
        extOpportunityMapper.batchMerge(request, currentUser, currentOrgId);
        extFollowUpRecordMapper.batchMerge(request, currentUser, currentOrgId);
        extFollowUpPlanMapper.batchMerge(request, currentUser, currentOrgId);
        List<Customer> mergeCustomers = customerMapper.selectByIds(request.getMergeIds());
        List<String> ownerIds = mergeCustomers.stream().map(Customer::getOwner).toList();
        for (String ownerId : ownerIds) {
            CustomerCollaborationAddRequest collaborationAddRequest = new CustomerCollaborationAddRequest();
            collaborationAddRequest.setCustomerId(request.getToMergeId());
            collaborationAddRequest.setCollaborationType("COLLABORATION");
            collaborationAddRequest.setUserId(ownerId);
            customerCollaborationService.add(collaborationAddRequest, currentUser, currentOrgId);
        }
        customerMapper.deleteByIds(request.getMergeIds());
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        List<ChartResult> chartResults = extCustomerMapper.chart(chartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return moduleFormCacheService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }
}