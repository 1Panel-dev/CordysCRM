package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.domain.BaseResourceSubField;
import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseChartService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.uid.utils.EnumUtils;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalResourceUpdateType;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.dto.ResourceApprovalFieldUpdateParam;
import cn.cordys.crm.approval.dto.ResourceApprovalPostUpdateParam;
import cn.cordys.crm.approval.dto.ResourceSnapshotApprovalParam;
import cn.cordys.crm.approval.handler.ApprovalResourceHandler;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.customer.constants.CustomerResultCode;
import cn.cordys.crm.customer.domain.*;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerPoolMapper;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.ImportType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.ImportRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
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
import cn.cordys.crm.system.service.StatisticFieldService.StatisticHostScope;
import cn.cordys.excel.utils.EasyExcelExporter;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import cn.idev.excel.FastExcelFactory;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
@Slf4j
public class CustomerService implements ApprovalResourceHandler {

    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private StatisticFieldService statisticFieldService;
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
    private BaseMapper<CustomerPool> customerPoolMapper;
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
    private BaseChartService baseChartService;
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
    @Resource
    private BaseMapper<CustomerCollaboration> customerCollaborationMapper;
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ApprovalFlowService approvalFlowService;

    public PagerWithOption<List<CustomerListResponse>> list(CustomerPageRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomerListResponse> list = extCustomerMapper.list(request, orgId, userId, deptDataPermission);
        List<CustomerListResponse> buildList = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public PagerWithOption<List<CustomerListResponse>> transitionList(CustomerPageRequest request, String userId, String orgId) {
        /*
         * 数据范围: 当前用户所在公海&私海客户(协作客户&&数据权限客户)
         */
        List<String> scopeIds = userExtendService.getUserScopeIds(userId, orgId);
        List<CustomerPool> pools = extCustomerPoolMapper.getPoolByScopeIds(scopeIds, orgId);
        request.setTransitionPoolIds(pools.stream().map(CustomerPool::getId).toList());
        request.setTransition(true);
        request.setTransitionDataPermission(dataScopeService.getDeptDataPermission(userId, orgId, null, PermissionConstants.CUSTOMER_MANAGEMENT_READ));
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

    public ModuleFormConfigDTO getFormConfig(String orgId) {
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
                .toList();

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
            var recycleRuleWrapper = new LambdaQueryWrapper<CustomerPoolRecycleRule>();
            recycleRuleWrapper.in(CustomerPoolRecycleRule::getPoolId, poolIds);
            List<CustomerPoolRecycleRule> recycleRules = customerPoolRecycleRuleMapper.selectListByLambda(recycleRuleWrapper);
            recycleRuleMap = recycleRules.stream().collect(Collectors.toMap(CustomerPoolRecycleRule::getPoolId, rule -> rule));
        }

        // 公海原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CUSTOMER_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例, 统一留空
        List<String> approvingResourceIds = list.stream()
                .filter(item -> Strings.CI.equals(item.getApprovalStatus(), ApprovalStatus.APPROVING.name()))
                .map(CustomerListResponse::getId).toList();
        Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(approvingResourceIds, orgId);
        Map<String, String> submitterIdMap = baseService.getApprovingResourceSubmitterIds(approvingResourceIds);

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

            customerListResponse.setFirstApproved(firstNodeApprovedMap.get(customerListResponse.getId()));
            customerListResponse.setSubmitterId(submitterIdMap.get(customerListResponse.getId()));
        });

        return list;
    }

    public CustomerGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        CustomerGetResponse getResponse = get(id);
        if (getResponse == null) {
            throw new GenericException(Translator.get("customer.not.exist"));
        }
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

    /**
     * @param id 客户ID
     * @return 客户详情
     */
    public CustomerGetResponse get(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        if (customer == null) {
            return null;
        }
        CustomerGetResponse customerGetResponse = BeanUtils.copyBean(new CustomerGetResponse(), customer);
        customerGetResponse = baseService.setCreateUpdateOwnerUserName(customerGetResponse);
        // 获取模块字段
        List<BaseModuleFieldValue> customerFields = customerFieldService.getModuleFieldValuesByResourceId(id);
        ModuleFormConfigDTO customerFormConfig = getFormConfig(customer.getOrganizationId());

        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, customerFields);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(customerGetResponse,
                CustomerGetResponse::getOwner, CustomerGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        customerGetResponse.setOptionMap(optionMap);
        customerGetResponse.setModuleFields(customerFields);

        // 公海原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CUSTOMER_POOL_RS.name(), customer.getOrganizationId());
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        if (customerGetResponse.getOwner() != null) {
            // 获取负责人默认公海信息
            Map<String, CustomerPool> ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(List.of(customerGetResponse.getOwner()), customer.getOrganizationId());
            List<String> poolIds = ownersDefaultPoolMap.values().stream().map(CustomerPool::getId).distinct().toList();
            Map<String, CustomerPoolRecycleRule> recycleRuleMap;
            if (CollectionUtils.isEmpty(poolIds)) {
                recycleRuleMap = Map.of();
            } else {
                var recycleRuleWrapper = new LambdaQueryWrapper<CustomerPoolRecycleRule>();
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


            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(customerGetResponse.getOwner(), customer.getOrganizationId());
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

        // 审批中才需要提审人与首节点是否已通过, 其余状态不查审批实例
        if (Strings.CI.equals(customerGetResponse.getApprovalStatus(), ApprovalStatus.APPROVING.name())) {
            Map<String, Boolean> firstNodeApprovedMap = baseService.getApprovingResourceFirstNodeApproved(List.of(id), customer.getOrganizationId());
            customerGetResponse.setFirstApproved(firstNodeApprovedMap.get(id));
            customerGetResponse.setSubmitterId(baseService.getApprovingResourceSubmitterId(id));
        }

        return customerGetResponse;
    }

    /**
     * 获取客户详情 (⚠️反射调用; 勿修改入参, 返回, 方法名!)
     *
     * @param id 客户ID
     * @return 客户详情
     */
    public CustomerGetResponse getSimple(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        if (customer == null) {
            return null;
        }
        CustomerGetResponse response = BeanUtils.copyBean(new CustomerGetResponse(), customer);
        List<BaseModuleFieldValue> fvs = customerFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fvs);
        return response;
    }

    /**
     * 批量获取客户详情 (用于数据源批量查询优化)
     *
     * @param ids 客户ID集合
     * @return 客户详情列表
     */
    public List<CustomerGetResponse> batchGetSimpleByIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        // 批量查询资源基本信息
        List<Customer> customers = customerMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(customers)) {
            return List.of();
        }
        // 批量查询自定义字段值
        Map<String, List<BaseModuleFieldValue>> fieldValueMap = customerFieldService.getResourceFieldMap(ids, true);

        // 组装结果
        return customers.stream().map(customer -> {
            CustomerGetResponse response = BeanUtils.copyBean(new CustomerGetResponse(), customer);
            response.setModuleFields(fieldValueMap.get(customer.getId()));
            return response;
        }).toList();
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.ADD)
    @HitApproval(formKey = FormKey.CUSTOMER, executeType = ExecuteTimingEnum.CREATE, operatorId = "{#userId}")
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
        customer.setFrozen(false);
        customer.setApprovalStatus(ApprovalStatus.NONE.name());
        customer.setApproved(false);

        //保存自定义字段
        customerFieldService.saveModuleField(customer, orgId, userId, request.getModuleFields(), false);

        customerMapper.insert(customer);

        // 统计字段: 本条记录刚建好, 先按各统计字段的空值口径把值行落一次
        statisticFieldService.refreshDataStatisticFields(FormKey.CUSTOMER.getKey(), customer.getId(), orgId);
        // 统计字段: 新数据可能关联到了别的表单记录, 被关联记录的统计值要跟着重算
        statisticFieldService.refreshByRelatedDataChange(FormKey.CUSTOMER.getKey(), customer.getId(), orgId);
        baseService.handleAddLogWithResourceName(customer, request.getModuleFields());
        // 通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_ADD, customer.getName(), userId,
                orgId, List.of(customer.getOwner()), true);
        return customer;
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    @HitApproval(formKey = FormKey.CUSTOMER, executeType = ExecuteTimingEnum.UPDATE, resourceId = "{#request.id}", updateType = "{#request.updateType}", operatorId = "{#userId}", comment = "{#request.comment}")
    public Customer update(CustomerUpdateRequest request, String userId, String orgId) {
        Customer originCustomer = customerMapper.selectByPrimaryKey(request.getId());
        // 审批回退是把客户还给原负责人, 不是一次新的分配, 不该受负责人容量限制;
        // 若在这里抛异常, 会被 revertToSnapshot 的 catch 吞掉, 业务数据会原地不动
        if (!Strings.CS.equals(originCustomer.getOwner(), request.getOwner())
                && !isApprovalRevert(request.getUpdateType())) {
            poolCustomerService.validateCapacity(1, request.getOwner(), orgId);
        }

        Customer customer = BeanUtils.copyBean(new Customer(), request);
        customer.setUpdateTime(System.currentTimeMillis());
        customer.setUpdateUser(userId);
        // 保留审批状态, 编辑不改变审批状态
        customer.setApprovalStatus(originCustomer.getApprovalStatus());
        customer.setApproved(originCustomer.getApproved());

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!Strings.CS.equals(request.getOwner(), originCustomer.getOwner())) {
                //客户负责人变更，联系人同步更新
                syncContactOwnerOnOwnerChange(request, originCustomer, orgId);


                // 如果责任人有修改，则添加责任人历史
                // 审批回退的负责人变更是回退动作本身, 不再记一条, 否则历史里会残留一次并未真正发生的负责人变更
                if (!isApprovalRevert(request.getUpdateType())) {
                    customerOwnerHistoryService.add(originCustomer, userId, false);
                }
                sendTransferNotice(List.of(originCustomer), request.getOwner(), userId, orgId);
                // 重置领取时间
                customer.setCollectionTime(System.currentTimeMillis());
            }
        }

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = customerFieldService.getModuleFieldValuesByResourceId(request.getId());

        // 统计字段: 关联字段在下面会被覆盖, 改之前先把它当前指向的宿主捕下来 ——
        // 改成别的关联对象时, 变更前那条宿主的统计值会偏大, 而改完就再也查不出它了
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.CUSTOMER.getKey(), List.of(request.getId()), orgId);

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            customerFieldService.updateModuleFieldByAgent(customer, originCustomerFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(customer, request.getModuleFields(), orgId, userId);
        }

        customerMapper.update(customer);
        // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
        statisticFieldService.refreshAfterRelatedChange(statisticScope, List.of(request.getId()));

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

    /**
     * 删除客户（带审批校验）
     *
     * @param id     客户ID
     * @param userId 用户ID
     * @param orgId  组织ID
     */
    @HitApproval(formKey = FormKey.CUSTOMER, executeType = ExecuteTimingEnum.DELETE, resourceId = "{#id}", operatorId = "{#userId}")
    public void deleteWithApprovalCheck(String id, String userId, String orgId) {
        delete(id, userId, orgId);
    }

    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Customer originCustomer = customerMapper.selectByPrimaryKey(id);
        checkResourceRef(List.of(id));
        deleteCustomerResource(List.of(id));

        // 设置操作对象
        OperationLogContext.setResourceName(originCustomer.getName());

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                NotificationConstants.Event.CUSTOMER_DELETED, originCustomer.getName(), userId,
                orgId, List.of(originCustomer.getOwner()), true);
    }

    public BatchAffectReasonResponse batchTransfer(CustomerBatchTransferRequest request, String userId, String orgId) {
        List<Customer> originCustomers = customerMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originCustomers)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("customer.not.exist")).build();
        }
        long processCount = originCustomers.stream().filter(customer -> !Strings.CS.equals(customer.getOwner(), request.getOwner())).count();
        poolCustomerService.validateCapacity((int) processCount, request.getOwner(), orgId);

        // 转移 SQL 只写负责人真正发生变化的客户, 审批同样只对这些客户触发, 避免给原本就是这个负责人的客户凭空建一条审批
        List<String> changedIds = originCustomers.stream()
                .filter(customer -> !Strings.CS.equals(customer.getOwner(), request.getOwner()))
                .map(Customer::getId)
                .toList();
        // 快照须在转移前落库, 否则审批驳回/撤回时回退到的是转移后的负责人
        CommonBeanFactory.getBean(ApprovalResourceService.class).batchTransferTriggerApproval(
                changedIds, BusinessModuleField.CUSTOMER_OWNER, FormKey.CUSTOMER, orgId, userId, request.getOwner());

        // 客户负责人变更, 联系人负责人要跟着走, 与单个编辑(CustomerService#update)保持一致;
        // 在这里取值, 此时客户行还没被转移 SQL 改写, 拿到的仍是原负责人
        originCustomers.stream()
                .filter(customer -> !Strings.CS.equals(customer.getOwner(), request.getOwner()))
                .forEach(customer -> customerContactService.updateContactOwner(
                        customer.getId(), request.getOwner(), customer.getOwner(), orgId));

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

        // success: 真的换了负责人; skip: 负责人本来就是目标负责人; fail: 入参里有查不到的客户
        return BatchAffectReasonResponse.builder()
                .success((int) processCount)
                .fail(CollectionUtils.size(request.getIds()) - originCustomers.size())
                .skip(originCustomers.size() - (int) processCount)
                .errorMessages(Translator.get("batch.transfer.reason"))
                .build();
    }

    /**
     * 是否为审批驳回/撤回触发的回退更新
     * <p>
     * 回退复用的是编辑接口, 但语义是"把数据还原回去"而非一次新的编辑:
     * 不该占用负责人容量, 也不该再产生一条负责人变更记录。
     *
     * @param updateType 更新类型
     * @return true 表示这是回退
     */
    private boolean isApprovalRevert(String updateType) {
        return ApprovalResourceUpdateType.APPROVAL.getValue().equals(updateType);
    }

    /**
     * 客户负责人变更时同步联系人负责人
     * <p>
     * 正常编辑/转移是把原负责人名下的联系人整批改派给新负责人, 按负责人反查即可;
     * 审批回退方向相反, 必须按编辑前快照记录的 id 逐个还原 —— 若同样按负责人反查, 会把本来
     * 就挂在新负责人名下的联系人一并拖回原负责人。升级前落库的旧快照没记 id, 只能退回近似还原。
     *
     * @param request        编辑请求, 回退时其运行时类型是编辑前快照
     * @param originCustomer 变更前的客户
     * @param orgId          组织ID
     */
    private void syncContactOwnerOnOwnerChange(CustomerUpdateRequest request, Customer originCustomer, String orgId) {
        if (isApprovalRevert(request.getUpdateType()) && request instanceof CustomerApprovalSnapshotRequest snapshot
                && snapshot.getContactIds() != null) {
            // 快照已经记下这次改派了谁(空集合表示一个都没有), 只按 id 还原; 空集合不生成 in () 这种非法 SQL
            if (CollectionUtils.isNotEmpty(snapshot.getContactIds())) {
                customerContactService.updateContactOwnerByIds(request.getId(), snapshot.getContactIds(), request.getOwner(), orgId);
            }
            return;
        }
        customerContactService.updateContactOwner(request.getId(), request.getOwner(), originCustomer.getOwner(), orgId);
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
        // 状态权限校验: 过滤出当前用户有权删除的客户
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CUSTOMER.getValue(),
                customers,
                PermissionConstants.CUSTOMER_MANAGEMENT_DELETE,
                orgId,
                Customer::getId,
                Customer::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return;
        }

        List<Customer> permittedCustomers = customers.stream()
                .filter(customer -> permittedIds.contains(customer.getId()))
                .toList();
        List<String> toDoIds = permittedCustomers.stream().map(Customer::getId).toList();
        if (CollectionUtils.isEmpty(toDoIds)) {
            return;
        }

        // 命中删除审批流的客户不直接删除, 走审批
        Map<String, String> nameMap = permittedCustomers.stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        List<String> approvalIds = approvalResourceService.batchDeleteTriggerApproval(
                toDoIds, FormKey.CUSTOMER, orgId, userId, nameMap);
        List<String> deleteIds = toDoIds.stream().filter(id -> !approvalIds.contains(id)).toList();
        if (CollectionUtils.isEmpty(deleteIds)) {
            return;
        }

        checkResourceRef(deleteIds);

        deleteCustomerResource(deleteIds);

        List<Customer> deletedCustomers = permittedCustomers.stream()
                .filter(customer -> deleteIds.contains(customer.getId()))
                .toList();
        List<LogDTO> logs = deletedCustomers.stream()
                .map(customer ->
                        new LogDTO(orgId, customer.getId(), userId, LogType.DELETE, LogModule.CUSTOMER_INDEX, customer.getName())
                )
                .toList();
        logService.batchAdd(logs);

        // 消息通知
        deletedCustomers.forEach(customer ->
                commonNoticeSendService.sendNotice(NotificationConstants.Module.CUSTOMER,
                        NotificationConstants.Event.CUSTOMER_DELETED, customer.getName(), userId,
                        orgId, List.of(customer.getOwner()), true)
        );
    }

    public void deleteCustomerResource(List<String> ids) {
        // 统计字段: 删除会一并带走关联字段的值, 宿主关系只能删前先捕; 重算要等下面全部删完才准。
        // 挂在这里而不是两个公开入口上: 客户删除、客户批量删除、公海里的两种删除最后都走这一处,
        // 而且捕获时客户还在、重算时已被本方法删掉, 级联进来的重算会被统计字段服务侧的存在性判断挡掉。
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHosts(
                FormKey.CUSTOMER.getKey(), ids, OrganizationContext.getOrganizationId());
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
        statisticFieldService.refreshAfterRelatedDelete(statisticScope);
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
        customers = customers.stream()
                .filter(customer -> !BooleanUtils.isTrue(customer.getInSharedPool()))
                .toList();
        if (CollectionUtils.isEmpty(customers)) {
            return BatchAffectResponse.builder().success(0).fail(request.getIds().size()).build();
        }
        CustomerPool targetPool = null;
        Map<String, CustomerPool> ownersDefaultPoolMap = new HashMap<>(4);
        if (StringUtils.isNotBlank(request.getPoolId())) {
            targetPool = getTargetCustomerPool(request.getPoolId(), orgId);
        } else {
            List<String> ownerIds = getOwners(customers);
            ownersDefaultPoolMap = customerPoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        }

        int success = 0;
        var logs = new ArrayList<LogDTO>();
        for (Customer customer : customers) {
            CustomerPool customerPool = targetPool != null ? targetPool : ownersDefaultPoolMap.get(customer.getOwner());
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
        batchRequest.setPoolId(request.getPoolId());
        batchRequest.setIds(List.of(request.getId()));
        return batchToPool(batchRequest, currentUser, orgId);
    }

    private CustomerPool getTargetCustomerPool(String poolId, String orgId) {
        CustomerPool customerPool = customerPoolMapper.selectByPrimaryKey(poolId);
        if (customerPool == null || !Strings.CS.equals(customerPool.getOrganizationId(), orgId) || !BooleanUtils.isTrue(customerPool.getEnable())) {
            throw new GenericException(Translator.get("customer_pool_not_exist"));
        }
        return customerPool;
    }

    public List<OptionDTO> getCustomerOptions(String keyword, String organizationId) {
        return extCustomerMapper.getCustomerOptions(keyword, organizationId);
    }

    public String getCustomerName(String id) {
        Customer customer = customerMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(customer).map(Customer::getName).orElse(null);
    }

    public List<Customer> getCustomerListByNames(List<String> names) {
        var lambdaQueryWrapper = new LambdaQueryWrapper<Customer>();
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
            return String.join(",", JSON.parseArray(JSON.toJSONString(names), String.class));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        new EasyExcelExporter()
                .exportMultiSheetTplWithSharedHandler(response, moduleFormService.getCustomImportHeadsNoRef(FormKey.CUSTOMER.getKey(), currentOrg),
                        Translator.get("customer.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT),
                        new CustomTemplateWriteHandler(moduleFormService.getAllCustomImportFields(FormKey.CUSTOMER.getKey(), currentOrg)),
                        new CustomHeadColWidthStyleStrategy());
    }

    /**
     * 导入检查
     *
     * @param file       导入文件
     * @param currentOrg 当前组织
     * @return 导入检查信息
     */
    public ImportResponse importPreCheck(MultipartFile file, String importType, String currentOrg) {
        if (file == null) {
            throw new GenericException(Translator.get("file_cannot_be_null"));
        }
        return checkImportExcel(file, importType, currentOrg);
    }

    /**
     * 客户导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     * @return 导入返回信息
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ImportResponse realImport(MultipartFile file, ImportRequest request, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CUSTOMER.getKey(), currentOrg);
            CustomImportAfterDoConsumer<Customer, BaseResourceSubField> afterDo = (customers, customerFields, customerFieldBlobs) -> {
                var logs = new ArrayList<LogDTO>();
                ImportType importType = EnumUtils.valueOf(ImportType.class, request.getImportType());
                switch (importType) {
                    case ADD -> {
                        customers.forEach(customer -> {
                            customer.setCollectionTime(customer.getCreateTime());
                            customer.setInSharedPool(false);
                            customer.setFrozen(false);
                            logs.add(new LogDTO(currentOrg, customer.getId(), currentUser, LogType.ADD, LogModule.CUSTOMER_INDEX, customer.getName()));
                        });
                        customerMapper.batchInsert(customers);
                        customerFieldMapper.batchInsert(customerFields.stream().map(field -> BeanUtils.copyBean(new CustomerField(), field)).toList());
                        customerFieldBlobMapper.batchInsert(customerFieldBlobs.stream().map(field -> BeanUtils.copyBean(new CustomerFieldBlob(), field)).toList());
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
                        Map<String, Customer> originCustomerMaps = originCustomerList.stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> originFieldValueMap = customerFieldService.getResourceFieldMap(ids, true);

                        List<CustomerField> insertField = new ArrayList<>();
                        List<CustomerFieldBlob> insertFieldBlob = new ArrayList<>();
                        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
                        ExtCustomerMapper customerBatchMapper = sqlSession.getMapper(ExtCustomerMapper.class);
                        CommonMapper commonMapper = sqlSession.getMapper(CommonMapper.class);

                        if (CollectionUtils.isNotEmpty(customers)) {
                            customers.forEach(customer -> {
                                customer.setInSharedPool(false);
                                customerBatchMapper.updateCustomer(customer);
                            });
                        }

                        if (CollectionUtils.isNotEmpty(customerFields)) {
                            List<CustomerField> fieldList = customerFieldMapper.selectByIds(customerFields.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, CustomerField> fieldMap = fieldList.stream().collect(Collectors.toMap(CustomerField::getId, Function.identity()));
                            customerFields.forEach(customerField -> {
                                if (fieldMap.containsKey(customerField.getId())) {
                                    commonMapper.updateCustomerField("customer_field", customerField);
                                } else {
                                    insertField.add(BeanUtils.copyBean(new CustomerField(), customerField));
                                }
                            });
                        }

                        if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
                            List<CustomerFieldBlob> blobList = customerFieldBlobMapper.selectByIds(customerFieldBlobs.stream().map(BaseResourceSubField::getId).toList());
                            Map<String, CustomerFieldBlob> blobMap = blobList.stream().collect(Collectors.toMap(CustomerFieldBlob::getId, Function.identity()));
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

                        SqlSession currentSession =
                                SqlSessionUtils.getSqlSession(sqlSessionFactory);
                        currentSession.clearCache();

                        Map<String, Customer> modifiedCustomerMaps = customerMapper.selectByIds(ids).stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
                        Map<String, List<BaseModuleFieldValue>> modifiedFieldValueMap = customerFieldService.getResourceFieldMap(ids, true);

                        ids.forEach(id -> {
                            Customer originDate = originCustomerMaps.get(id);
                            Customer modifiedDate = modifiedCustomerMaps.get(id);
                            baseService.handleUpdateLog(originDate, modifiedDate, originFieldValueMap.get(id), modifiedFieldValueMap.get(id), id, modifiedDate.getName());
                            LogContextInfo contextInfo = OperationLogContext.getContext();
                            if (contextInfo != null) {
                                LogDTO logDTO = new LogDTO(currentOrg, id, currentUser, LogType.UPDATE, LogModule.CUSTOMER_INDEX, modifiedDate.getName());
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
            CustomFieldImportEventListener<Customer> eventListener = new CustomFieldImportEventListener<>(fields, Customer.class, currentOrg, currentUser,
                    "customer_field", "customer_field_blob", afterDo, 2000, null, null, request.getImportType());
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccessCount()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer import error: {}", e.getMessage());
            throw new GenericException("导入异常，请检查文件数据！" + e);
        }
    }

    /**
     * 检查导入的文件
     *
     * @param file       文件
     * @param currentOrg 当前组织
     * @return 检查信息
     */
    private ImportResponse checkImportExcel(MultipartFile file, String importType, String currentOrg) {
        try {
            List<BaseField> fields = moduleFormService.getAllCustomImportFields(FormKey.CUSTOMER.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "customer", "customer_field", currentOrg, importType);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            log.error("customer import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public BatchAffectReasonResponse batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = customerFieldService.getAndCheckField(request.getFieldId(), organizationId);
        List<Customer> originCustomers = customerMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(originCustomers)) {
            return BatchAffectReasonResponse.builder().success(0).fail(0).skip(0)
                    .errorMessages(Translator.get("customer.not.exist")).build();
        }
        // 状态权限校验: 过滤出当前用户有权编辑的客户。放在各分支之前, 是因为下面还有一条走批量转移
        // 的路径, 它同样是一次编辑 —— 否则处在无权编辑状态(如审批中)的客户会从这条路径绕过去
        List<String> permittedIds = approvalFlowService.filterResourcesWithPermission(
                ApprovalFormTypeEnum.CUSTOMER.getValue(),
                originCustomers,
                PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                organizationId,
                Customer::getId,
                Customer::getApprovalStatus
        );
        if (CollectionUtils.isEmpty(permittedIds)) {
            return BatchAffectReasonResponse.builder().success(0).fail(originCustomers.size()).skip(0)
                    .errorMessages(Translator.get("no.operation.permission")).build();
        }

        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CUSTOMER_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            CustomerBatchTransferRequest batchTransferRequest = new CustomerBatchTransferRequest();
            batchTransferRequest.setIds(permittedIds);
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            return batchTransfer(batchTransferRequest, userId, organizationId);
        }

        ApprovalResourceService approvalResourceService = CommonBeanFactory.getBean(ApprovalResourceService.class);
        approvalResourceService.batchEditTriggerApproval(permittedIds, request.getFieldId(), FormKey.CUSTOMER, organizationId, userId, field.getName(), request.getFieldValue());
        List<Customer> permittedCustomers = originCustomers.stream()
                .filter(customer -> permittedIds.contains(customer.getId()))
                .toList();

        ResourceBatchEditRequest filteredRequest = new ResourceBatchEditRequest();
        filteredRequest.setIds(permittedIds);
        filteredRequest.setFieldId(request.getFieldId());
        filteredRequest.setFieldValue(request.getFieldValue());

        // 统计字段: 批量编辑只改一个字段, 改的若是关联字段, 下面这批客户的关联关系会整批换人 ——
        // 换之前它们指向的宿主得先捕下来, 否则那些宿主的统计值会一直偏大; 改的不是关联字段时
        // 这一步在服务内部直接短路, 只多一次反查。用 permittedIds: 没权限的那些根本没被写
        StatisticHostScope statisticScope = statisticFieldService.captureRelatedHostsForFieldChange(
                FormKey.CUSTOMER.getKey(), request.getFieldId(), permittedIds, organizationId);
        customerFieldService.batchUpdate(filteredRequest, field, permittedCustomers, Customer.class, LogModule.CUSTOMER_INDEX, extCustomerMapper::batchUpdate, userId, organizationId);
        // 统计字段: 改前改后关联到的宿主记录都要重算(关联没动时这两批是同一批, 去重后只算一次)
        statisticFieldService.refreshAfterRelatedChange(statisticScope, permittedIds);

        return BatchAffectReasonResponse.builder()
                .success(permittedIds.size())
                .fail(originCustomers.size() - permittedIds.size())
                .skip(0)
                .errorMessages(Translator.get("batch.update.reason"))
                .build();
    }

    /**
     * @param request      合并请求参数
     * @param currentUser  当前用户
     * @param currentOrgId 当前组织ID
     */
    @OperationLog(module = LogModule.CUSTOMER_INDEX, type = LogType.MERGE, resourceId = "{#request.toMergeId}")
    public void merge(CustomerMergeRequest request, String currentUser, String currentOrgId) {
        /*
         * 规则:
         * 1. 合并客户联系人, 客户关联商机.
         * 3. 合并客户跟进记录/计划.
         * 4. 删除被合并的客户, 并添加对应的负责人为合并客户的协作人, 被合并客户的协作人也要.
         */
        request.getMergeIds().remove(request.getToMergeId());
        Customer oldCustomer = customerMapper.selectByPrimaryKey(request.getToMergeId());
        if (CollectionUtils.isEmpty(request.getMergeIds()) || oldCustomer == null) {
            // 没有可合并的客户数据
            throw new GenericException(Translator.get("no.customer.merge.data"));
        }

        // 批量合并产生的修改日志
        List<LogDTO> mergeLogs = getMergeRelateLogs(request, currentUser, currentOrgId);
        // 联系人需要排除重复项合并
        Map<String, Boolean> uniqueMap = customerContactService.getUniqueMap(currentOrgId);
        List<String> names = new ArrayList<>();
        List<String> phones = new ArrayList<>();
        var contactWrapper = new LambdaQueryWrapper<CustomerContact>();
        contactWrapper.eq(CustomerContact::getCustomerId, request.getToMergeId());
        List<CustomerContact> toMergeContacts = customerContactMapper.selectListByLambda(contactWrapper);
        if (uniqueMap.get(BusinessModuleField.CUSTOMER_CONTACT_NAME.getKey())) {
            names = toMergeContacts.stream().map(CustomerContact::getName).toList();
        }
        if (uniqueMap.get(BusinessModuleField.CUSTOMER_CONTACT_PHONE.getKey())) {
            phones = toMergeContacts.stream().map(CustomerContact::getPhone).toList();
        }
        extCustomerContactMapper.batchMerge(request, currentUser, currentOrgId, names, phones);
        extOpportunityMapper.batchMerge(request, currentUser, currentOrgId);
        extFollowUpRecordMapper.batchMerge(request, currentUser, currentOrgId);
        extFollowUpPlanMapper.batchMerge(request, currentUser, currentOrgId);

        // 合并协作人&&删除被合并客户&&记录删除日志
        mergeCollaboration(request, currentUser, currentOrgId);
        List<Customer> mergeCustomers = customerMapper.selectByIds(request.getMergeIds());
        customerMapper.deleteByIds(request.getMergeIds());
        for (Customer mergeCustomer : mergeCustomers) {
            // 被合并客户的删除日志
            mergeLogs.add(new LogDTO(currentOrgId, mergeCustomer.getId(), currentUser, LogType.DELETE, LogModule.CUSTOMER_INDEX, mergeCustomer.getName()));
        }

        // 变更合并客户的负责人
        if (!Strings.CS.equals(oldCustomer.getOwner(), request.getOwnerId())) {
            LogDTO logDTO = new LogDTO(currentOrgId, request.getToMergeId(), currentUser, LogType.UPDATE, LogModule.CUSTOMER_INDEX, oldCustomer.getName());
            logDTO.setOriginalValue(oldCustomer);
            // 客户负责人变更，联系人同步更新
            customerContactService.updateContactOwner(request.getToMergeId(), request.getOwnerId(), oldCustomer.getOwner(), currentOrgId);
            // 如果责任人有修改，则添加责任人历史
            customerOwnerHistoryService.add(oldCustomer, currentUser, false);
            sendTransferNotice(List.of(oldCustomer), request.getOwnerId(), currentUser, currentOrgId);
            Customer customer = BeanUtils.copyBean(new Customer(), oldCustomer);
            // 重置领取时间
            customer.setCollectionTime(System.currentTimeMillis());
            customer.setOwner(request.getOwnerId());
            customerMapper.updateById(customer);
            // 合并客户的修改日志
            logDTO.setModifiedValue(customer);
            mergeLogs.add(logDTO);
        }

        // 批量插入合并日志&&其他操作日志
        OperationLogContext.setContext(LogContextInfo.builder().resourceId(request.getToMergeId()).resourceName(oldCustomer.getName())
                .originalValue(Map.of("merge", mergeCustomers.stream().map(Customer::getName).toList()))
                .modifiedValue(Map.of("merge", List.of(oldCustomer.getName())))
                .build());
        if (!CollectionUtils.isEmpty(mergeLogs)) {
            logService.batchAdd(mergeLogs);
        }
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        formConfig.getFields().addAll(BaseChartService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        CustomerChartAnalysisDbRequest customerChartAnalysisDbRequest = BeanUtils.copyBean(new CustomerChartAnalysisDbRequest(), chartAnalysisDbRequest);
        List<ChartResult> chartResults = extCustomerMapper.chart(customerChartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return baseChartService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }

    /**
     * 合并客户协作人
     *
     * @param request      请求参数
     * @param currentUser  当前用户
     * @param currentOrgId 当前组织ID
     */
    private void mergeCollaboration(CustomerMergeRequest request, String currentUser, String currentOrgId) {
        // 被合并客户的协作人
        var mergeCollaborationWrapper = new LambdaQueryWrapper<CustomerCollaboration>();
        mergeCollaborationWrapper.in(CustomerCollaboration::getCustomerId, request.getMergeIds());
        List<CustomerCollaboration> mergeCollaborations = customerCollaborationMapper.selectListByLambda(mergeCollaborationWrapper);
        List<String> toCollaborationUserIds = mergeCollaborations.stream().map(CustomerCollaboration::getUserId).distinct().toList();
        Map<String, String> toCollaborationMap = mergeCollaborations.stream().collect(Collectors.toMap(CustomerCollaboration::getUserId, CustomerCollaboration::getCollaborationType));
        // 被合并的客户负责人
        List<Customer> mergeCustomers = customerMapper.selectByIds(request.getMergeIds());
        List<String> toCollaborationOwnerIds = mergeCustomers.stream().map(Customer::getOwner).toList();
        // 合并去重
        List<String> mergeIds = Stream.of(toCollaborationUserIds, toCollaborationOwnerIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        // 合并客户已存在的协作人
        var collaborationWrapper = new LambdaQueryWrapper<CustomerCollaboration>();
        collaborationWrapper.eq(CustomerCollaboration::getCustomerId, request.getToMergeId());
        List<CustomerCollaboration> customerCollaborations = customerCollaborationMapper.selectListByLambda(collaborationWrapper);
        List<String> collaborationUserIds = customerCollaborations.stream().map(CustomerCollaboration::getUserId).toList();
        for (String mergeId : mergeIds) {
            if (collaborationUserIds.contains(mergeId) || Strings.CS.equals(mergeId, request.getOwnerId())) {
                // 已经是协作人或者是合并客户的负责人，跳过
                continue;
            }
            CustomerCollaborationAddRequest collaborationAddRequest = new CustomerCollaborationAddRequest();
            collaborationAddRequest.setCustomerId(request.getToMergeId());
            // 如果被合并客户的协作人中有该用户，则继承该协作类型，否则使用默认类型
            collaborationAddRequest.setCollaborationType(toCollaborationMap.getOrDefault(mergeId, "COLLABORATION"));
            collaborationAddRequest.setUserId(mergeId);
            customerCollaborationService.add(collaborationAddRequest, currentUser, currentOrgId);
        }

        // 删除被合并客户的协作人关系
        var delCollaborationWrapper = new LambdaQueryWrapper<CustomerCollaboration>();
        delCollaborationWrapper.in(CustomerCollaboration::getCustomerId, request.getMergeIds());
        customerCollaborationMapper.deleteByLambda(delCollaborationWrapper);
    }

    /**
     * 获取合并相关的日志
     *
     * @param mergeRequest 合并请求参数
     * @param currentUser  当前用户
     * @param currentOrgId 当前组织ID
     * @return 日志列表
     */
    private List<LogDTO> getMergeRelateLogs(CustomerMergeRequest mergeRequest, String currentUser, String currentOrgId) {
        var logs = new ArrayList<LogDTO>();

        List<Customer> mergeCustomers = customerMapper.selectByIds(mergeRequest.getMergeIds());
        Map<String, String> customerMap = mergeCustomers.stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
        // 联系人日志
        List<CustomerContact> mergeContacts = extCustomerContactMapper.getMergeContactList(mergeRequest, currentOrgId);
        if (CollectionUtils.isNotEmpty(mergeContacts)) {
            for (CustomerContact contact : mergeContacts) {
                LogDTO logDTO = new LogDTO(currentOrgId, contact.getId(), currentUser, LogType.UPDATE, LogModule.CUSTOMER_CONTACT, contact.getName());
                contact.setCustomerId(customerMap.get(contact.getCustomerId()));
                logDTO.setOriginalValue(contact);
                CustomerContact newContact = BeanUtils.copyBean(new CustomerContact(), contact);
                newContact.setCustomerId(mergeRequest.getToMergeId());
                logDTO.setModifiedValue(newContact);
                logs.add(logDTO);
            }
        }

        // 商机日志
        List<Opportunity> mergeOpportunities = extOpportunityMapper.getMergeOpportunityList(mergeRequest, currentOrgId);
        if (CollectionUtils.isNotEmpty(mergeOpportunities)) {
            for (Opportunity opportunity : mergeOpportunities) {
                LogDTO logDTO = new LogDTO(currentOrgId, opportunity.getId(), currentUser, LogType.UPDATE, LogModule.OPPORTUNITY_INDEX, opportunity.getName());
                opportunity.setCustomerId(customerMap.get(opportunity.getCustomerId()));
                logDTO.setOriginalValue(opportunity);
                Opportunity newOpportunity = BeanUtils.copyBean(new Opportunity(), opportunity);
                newOpportunity.setCustomerId(mergeRequest.getToMergeId());
                logDTO.setModifiedValue(newOpportunity);
                logs.add(logDTO);
            }
        }

        // 跟进记录/计划日志
        List<FollowUpRecord> mergeRecords = extFollowUpRecordMapper.getMergeRecordList(mergeRequest, currentOrgId);
        List<String> recordCustomerIds = mergeRecords.stream().map(FollowUpRecord::getCustomerId).toList();
        List<FollowUpPlan> mergePlans = extFollowUpPlanMapper.getMergePlanList(mergeRequest, currentOrgId);
        List<String> planCustomerIds = mergePlans.stream().map(FollowUpPlan::getCustomerId).toList();
        for (String mergeId : mergeRequest.getMergeIds()) {
            if (recordCustomerIds.contains(mergeId)) {
                LogDTO logDTO = new LogDTO(currentOrgId, mergeId, currentUser, LogType.UPDATE, LogModule.FOLLOW_UP_RECORD, customerMap.get(mergeId));
                FollowUpRecord oldRecord = new FollowUpRecord();
                oldRecord.setCustomerId(customerMap.get(mergeId));
                logDTO.setOriginalValue(oldRecord);
                FollowUpRecord newRecord = new FollowUpRecord();
                newRecord.setCustomerId(mergeRequest.getToMergeId());
                logDTO.setModifiedValue(newRecord);
                logs.add(logDTO);
            }
            if (planCustomerIds.contains(mergeId)) {
                LogDTO logDTO = new LogDTO(currentOrgId, mergeId, currentUser, LogType.UPDATE, LogModule.FOLLOW_UP_PLAN, customerMap.get(mergeId));
                FollowUpPlan oldPlan = new FollowUpPlan();
                oldPlan.setCustomerId(customerMap.get(mergeId));
                logDTO.setOriginalValue(oldPlan);
                FollowUpPlan newPlan = new FollowUpPlan();
                newPlan.setCustomerId(mergeRequest.getToMergeId());
                logDTO.setModifiedValue(newPlan);
                logs.add(logDTO);
            }
        }

        return logs;
    }


    public boolean checkOwner(String customerId, String userId) {
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        return Strings.CI.equals(customer.getOwner(), userId);
    }

    @Override
    public FormKey getFormKey() {
        return FormKey.CUSTOMER;
    }

    /**
     * 更新业务快照审批状态
     * <p>
     * 客户没有业务快照表，编辑回退统一走框架的 {@code approval_resource_snapshot}，此处无需处理。
     *
     * @param param 参数
     */
    @Override
    public void updateSnapshotApprovalStatus(ResourceSnapshotApprovalParam param) {
        // 客户无业务快照
    }

    @Override
    public String getPreUpdateSnapshotData(String resourceId, String userId, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(resourceId);
        if (customer == null) {
            return null;
        }
        List<BaseModuleFieldValue> customerFields = customerFieldService.getModuleFieldValuesByResourceId(resourceId);
        CustomerApprovalSnapshotRequest snapshotReq = BeanUtils.copyBean(new CustomerApprovalSnapshotRequest(), customer);
        snapshotReq.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());
        // 负责人变更会把这批联系人整批改派, 回退要按 id 精确还原, 先记下当前挂在负责人名下的联系人
        snapshotReq.setContactIds(customerContactService.listOwnedContactIds(
                customer.getId(), customer.getOwner(), customer.getOrganizationId()));
        ModuleFormConfigDTO customerFormConfig = getFormConfig(customer.getOrganizationId());
        // 获取模块字段
        moduleFormService.processBusinessFieldValues(snapshotReq, customerFields, customerFormConfig);
        return JSON.toJSONString(snapshotReq);
    }

    @Override
    public void revertToSnapshot(String resourceId, String userId, String orgId, String snapshotData) {
        try {
            CustomerApprovalSnapshotRequest request = JSON.parseObject(snapshotData, CustomerApprovalSnapshotRequest.class);
            if (request == null) {
                return;
            }
            CommonBeanFactory.getBean(CustomerService.class).update(request, userId, orgId);
            // 编辑回退的负责人变更分支会把领取时间重置为回退时间, 转移前的领取时间只能按快照单独还原
            revertTransfer(request);
            // 转移时 batchAdd 按转移前的负责人+领取时间写了一条变更记录, 回退要把它删掉,
            // 否则历史里会残留一次并未真正生效的负责人变更
            customerOwnerHistoryService.deleteTransferHistory(request.getId(), request.getOwner(), request.getCollectionTime());
        } catch (Exception e) {
            log.error("审批回退还原业务数据失败, resourceId:{}", resourceId, e);
        }
    }

    /**
     * 回退转移重置的领取时间
     * <p>
     * 快照没有领取时间(升级前落库的旧快照, 或转移前本就没有领取时间)时不回退,
     * 避免把转移写入的领取时间清空。
     *
     * @param request 编辑前快照
     */
    private void revertTransfer(CustomerApprovalSnapshotRequest request) {
        if (request.getCollectionTime() == null) {
            return;
        }
        Customer current = customerMapper.selectByPrimaryKey(request.getId());
        if (current == null || Objects.equals(current.getCollectionTime(), request.getCollectionTime())) {
            return;
        }
        extCustomerMapper.revertTransferByApproval(request.getId(), request.getCollectionTime());
    }

    /**
     * ⚠️反射调用: 由审批执行后置操作统一调用, 勿修改
     *
     * @param postFieldParam 参数
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void updateApprovalPostField(ResourceApprovalPostUpdateParam postFieldParam) {
        ModuleFormConfigDTO formConfig = getFormConfig(OrganizationContext.getOrganizationId());
        Map<String, BaseField> fieldConfigMap = formConfig.getFields().stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity(), (a, b) -> a));
        Customer customer = customerMapper.selectByPrimaryKey(postFieldParam.getResourceId());
        if (customer == null) {
            return;
        }
        // 保存原始数据用于日志记录
        Customer originCustomer = BeanUtils.copyBean(new Customer(), customer);
        List<BaseModuleFieldValue> originFields = customerFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId());
        List<CustomerField> customerFields = new ArrayList<>();
        List<CustomerFieldBlob> customerFieldBlobs = new ArrayList<>();

        for (ResourceApprovalFieldUpdateParam fieldUpdateParam : postFieldParam.getFields()) {
            if (!fieldConfigMap.containsKey(fieldUpdateParam.getFieldId()) || fieldUpdateParam.getFieldValue() == null) {
                continue;
            }
            BaseField fieldConfig = fieldConfigMap.get(fieldUpdateParam.getFieldId());
            AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
            if (fieldConfig.hasBusinessKey()) {
                // 业务主表字段
                customerFieldService.setResourceFieldValue(customer, fieldConfig.getBusinessKey(), fieldUpdateParam.getFieldValue());
            } else {
                // 自定义字段
                if (fieldConfig.isBlob()) {
                    customerFieldService.getResourceFieldBlobMapper().deleteByLambda(new LambdaQueryWrapper<CustomerFieldBlob>()
                            .eq(CustomerFieldBlob::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(CustomerFieldBlob::getResourceId, postFieldParam.getResourceId()));
                    CustomerFieldBlob field = new CustomerFieldBlob();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    customerFieldBlobs.add(field);
                } else {
                    customerFieldService.getResourceFieldMapper().deleteByLambda(new LambdaQueryWrapper<CustomerField>()
                            .eq(CustomerField::getFieldId, fieldUpdateParam.getFieldId())
                            .eq(CustomerField::getResourceId, postFieldParam.getResourceId()));
                    CustomerField field = new CustomerField();
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(postFieldParam.getResourceId());
                    field.setFieldId(fieldUpdateParam.getFieldId());
                    field.setFieldValue(customFieldResolver.convertToString(fieldConfig, fieldUpdateParam.getFieldValue()));
                    customerFields.add(field);
                }
            }
        }
        customerMapper.updateById(customer);
        if (CollectionUtils.isNotEmpty(customerFields)) {
            customerFieldService.getResourceFieldMapper().batchInsert(customerFields);
        }
        if (CollectionUtils.isNotEmpty(customerFieldBlobs)) {
            customerFieldService.getResourceFieldBlobMapper().batchInsert(customerFieldBlobs);
        }
        // 记录审批后置字段更新日志
        baseService.handleUpdateLog(originCustomer, customer, originFields,
                customerFieldService.getModuleFieldValuesByResourceId(postFieldParam.getResourceId()),
                postFieldParam.getResourceId(), customer.getName());
        // 从 OperationLogContext 中获取日志信息并手动记录
        LogContextInfo contextInfo = OperationLogContext.getContext();
        if (contextInfo != null) {
            String orgId = OrganizationContext.getOrganizationId();
            LogDTO logDTO = new LogDTO(orgId, postFieldParam.getResourceId(), postFieldParam.getOperator(), LogType.UPDATE, LogModule.CUSTOMER_INDEX, customer.getName());
            logDTO.setOriginalValue(contextInfo.getOriginalValue());
            logDTO.setModifiedValue(contextInfo.getModifiedValue());
            logService.add(logDTO);
            OperationLogContext.clear();
        }
    }
}
