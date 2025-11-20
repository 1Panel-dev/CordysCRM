package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.LinkScenarioKey;
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
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.common.utils.ConditionFilterUtils;
import cn.cordys.crm.clue.constants.ClueStatus;
import cn.cordys.crm.clue.domain.*;
import cn.cordys.crm.clue.dto.ClueFollowDTO;
import cn.cordys.crm.clue.dto.TransformCsAssociateDTO;
import cn.cordys.crm.clue.dto.request.*;
import cn.cordys.crm.clue.dto.response.ClueGetResponse;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.domain.*;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.mapper.ExtProductMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.constants.SheetKey;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormLinkFill;
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
import cn.cordys.crm.system.mapper.ExtUserMapper;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueService {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private DictService dictService;
    @Resource
    private CustomerService customerService;
    @Resource
    private OpportunityService opportunityService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private PoolClueService poolClueService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private ProductService productService;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private CustomerContactService customerContactService;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<ClueField> clueFieldMapper;
    @Resource
    private BaseMapper<ClueFieldBlob> clueFieldBlobMapper;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpRecordField> followUpRecordFieldMapper;
    @Resource
    private BaseMapper<FollowUpRecordFieldBlob> followUpRecordFieldBlobMapper;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private BaseMapper<FollowUpPlanField> followUpPlanFieldMapper;
    @Resource
    private BaseMapper<FollowUpPlanFieldBlob> followUpPlanFieldBlobMapper;

    public PagerWithOption<List<ClueListResponse>> list(CluePageRequest request, String userId, String orgId,
                                                        DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission);
        List<ClueListResponse> buildList = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ClueListResponse> list, List<ClueListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
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

    public ModuleFormConfigDTO getFormConfig(String orgId) {
        return moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
    }

    public List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
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
                    clueListResponse.getCollectionTime(), clueListResponse.getCreateTime()));

            UserDeptDTO userDeptDTO = userDeptMap.get(clueListResponse.getOwner());
            if (userDeptDTO != null) {
                clueListResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
            clueListResponse.setFollowerName(userNameMap.get(clueListResponse.getFollower()));
            clueListResponse.setCreateUserName(userNameMap.get(clueListResponse.getCreateUser()));
            clueListResponse.setUpdateUserName(userNameMap.get(clueListResponse.getUpdateUser()));
            clueListResponse.setOwnerName(userNameMap.get(clueListResponse.getOwner()));
            if (StringUtils.isNotBlank(clueListResponse.getReasonId())) {
                clueListResponse.setReasonName(dictMap.get(clueListResponse.getReasonId()));
            }
        });

        return list;
    }

    public ClueGetResponse getWithDataPermissionCheck(String id, String userId, String orgId) {
        ClueGetResponse getResponse = get(id, orgId);
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner(), PermissionConstants.CLUE_MANAGEMENT_READ);
        return getResponse;
    }

    public ClueGetResponse get(String id, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);
        clueGetResponse = baseService.setCreateUpdateOwnerUserName(clueGetResponse);

        // 获取模块字段
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(id);
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = getFormConfig(orgId);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, clueFields);
        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(clueGetResponse,
                ClueGetResponse::getOwner, ClueGetResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CLUE_OWNER.getBusinessKey(), ownerFieldOption);

        // 意向产品选项
        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);


        clueGetResponse.setOptionMap(optionMap);
        clueGetResponse.setModuleFields(clueFields);


        // 线索池原因
        DictConfigDTO dictConf = dictService.getDictConf(DictModule.CLUE_POOL_RS.name(), orgId);
        List<Dict> dictList = dictConf.getDictList();
        Map<String, String> dictMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

        if (clueGetResponse.getOwner() != null) {
            // 获取负责人线索池信息
            Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(List.of(clueGetResponse.getOwner()), orgId);
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
            // 设置回收公海
            CluePool reservePool = ownersDefaultPoolMap.get(clueGetResponse.getOwner());
            clueGetResponse.setRecyclePoolName(reservePool != null ? reservePool.getName() : null);
            // 计算剩余归属天数
            clueGetResponse.setReservedDays(cluePoolService.calcReservedDay(reservePool,
                    reservePool != null ? recycleRuleMap.get(reservePool.getId()) : null,
                    clueGetResponse.getCollectionTime(), clueGetResponse.getCreateTime()));

            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(clueGetResponse.getOwner(), orgId);
            if (userDeptDTO != null) {
                clueGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        if (clueGetResponse.getFollower() != null) {
            Map<String, String> userNameMap = baseService.getUserNameMap(List.of(clueGetResponse.getFollower()));
            clueGetResponse.setFollowerName(userNameMap.get(clueGetResponse.getFollower()));
        }

        if (StringUtils.isNotBlank(clueGetResponse.getReasonId())) {
            clueGetResponse.setReasonName(dictMap.get(clueGetResponse.getReasonId()));
        }

        // 附件信息
        clueGetResponse.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, clueFields));

        return clueGetResponse;
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.ADD, resourceName = "{#request.name}")
    public Clue add(ClueAddRequest request, String userId, String orgId) {
        productService.checkProductList(request.getProducts());
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        if (StringUtils.isBlank(request.getOwner())) {
            clue.setOwner(userId);
        }
        poolClueService.validateCapacity(1, clue.getOwner(), orgId);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(clue.getCreateTime());
        clue.setUpdateUser(userId);
        clue.setCreateUser(userId);
        clue.setOrganizationId(orgId);
        clue.setId(IDGenerator.nextStr());
        clue.setStage(ClueStatus.NEW.name());
        clue.setInSharedPool(false);

        //保存自定义字段
        clueFieldService.saveModuleField(clue, orgId, userId, request.getModuleFields(), false);

        clueMapper.insert(clue);
        baseService.handleAddLog(clue, request.getModuleFields());
        return clue;
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Clue update(ClueUpdateRequest request, String userId, String orgId) {
        productService.checkProductList(request.getProducts());
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        if (!Strings.CS.equals(originClue.getOwner(), request.getOwner())) {
            poolClueService.validateCapacity(1, request.getOwner(), orgId);
        }
        dataScopeService.checkDataPermission(userId, orgId, originClue.getOwner(), PermissionConstants.CLUE_MANAGEMENT_UPDATE);

        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!Strings.CS.equals(request.getOwner(), originClue.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                clueOwnerHistoryService.add(originClue, userId, false);
                sendTransferNotice(List.of(originClue), request.getOwner(), userId, orgId);
            }
        }

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = clueFieldService.getModuleFieldValuesByResourceId(request.getId());

        if (BooleanUtils.isTrue(request.getAgentInvoke())) {
            clueFieldService.updateModuleFieldByAgent(clue, originCustomerFields, request.getModuleFields(), orgId, userId);
        } else {
            // 更新模块字段
            updateModuleField(clue, request.getModuleFields(), orgId, userId);
        }

        clueMapper.update(clue);
        clue = clueMapper.selectByPrimaryKey(request.getId());
        baseService.handleUpdateLog(originClue, clue, originCustomerFields, request.getModuleFields(), originClue.getId(), originClue.getName());
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    private void sendTransferNotice(List<Clue> originClues, String toUser, String userId, String orgId) {
        originClues.forEach(clue -> commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.TRANSFER_CLUE, clue.getName(), userId,
                orgId, List.of(toUser), true));
    }

    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStatus(ClueStatusUpdateRequest request, String userId, String orgId) {
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        dataScopeService.checkDataPermission(userId, orgId, originClue.getOwner(), PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        // 记录修改前的状态
        clue.setLastStage(originClue.getStage());
        clueMapper.update(clue);
        // 日志
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(originClue.getName())
                        .originalValue(originClue)
                        .modifiedValue(clueMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }

    private void updateModuleField(Clue clue, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        clueFieldService.deleteByResourceId(clue.getId());
        // 再保存
        clueFieldService.saveModuleField(clue, orgId, userId, moduleFields, true);
    }

    /**
     * 转移客户
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    public void transitionCustomer(ClueTransitionCustomerRequest request, String userId, String orgId) {
        Customer customer = customerService.add(request, userId, orgId);
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_ADD);
        clue.setTransitionId(customer.getId());
        clue.setTransitionType(FormKey.CUSTOMER.name());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        clueMapper.update(clue);

        // 同步添加联系人
        LambdaQueryWrapper<CustomerContact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerContact::getPhone, clue.getPhone());
        List<CustomerContact> contacts = customerContactMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(contacts)) {
            CustomerContact contact = new CustomerContact();
            contact.setId(IDGenerator.nextStr());
            contact.setCustomerId(customer.getId());
            contact.setOwner(customer.getOwner());
            contact.setName(clue.getContact());
            contact.setPhone(clue.getPhone());
            contact.setEnable(true);
            contact.setOrganizationId(orgId);
            contact.setCreateUser(userId);
            contact.setCreateTime(System.currentTimeMillis());
            contact.setUpdateUser(userId);
            contact.setUpdateTime(System.currentTimeMillis());
            customerContactMapper.insert(contact);
        }

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_CONVERT_CUSTOMER, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);
    }


    @OperationLog(module = LogModule.CLUE_INDEX, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner(), PermissionConstants.CLUE_MANAGEMENT_DELETE);
        // 删除客户
        clueMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        clueFieldService.deleteByResourceId(id);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(List.of(id));
        // 删除跟进记录
        followUpRecordService.deleteByClueIds(List.of(id));
        // 删除跟进计划
        followUpPlanService.deleteByClueIds(List.of(id));

        // 设置操作对象
        OperationLogContext.setResourceName(clue.getName());

        // 消息通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);

    }

    public void batchTransfer(ClueBatchTransferRequest request, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(request.getIds());
        List<String> ownerIds = getOwners(clues);
        long processCount = clues.stream().filter(clue -> !Strings.CS.equals(clue.getOwner(), request.getOwner())).count();
        poolClueService.validateCapacity((int) processCount, request.getOwner(), orgId);
        dataScopeService.checkDataPermission(userId, orgId, ownerIds, PermissionConstants.CLUE_MANAGEMENT_UPDATE);

        // 添加责任人历史
        clueOwnerHistoryService.batchAdd(request, userId);
        extClueMapper.batchTransfer(request);

        // 记录日志
        List<LogDTO> logs = clues.stream()
                .map(clue -> {
                    Customer originCustomer = new Customer();
                    originCustomer.setOwner(clue.getOwner());
                    Customer modifieCustomer = new Customer();
                    modifieCustomer.setOwner(request.getOwner());
                    LogDTO logDTO = new LogDTO(orgId, clue.getId(), userId, LogType.UPDATE, LogModule.CLUE_INDEX, clue.getName());
                    logDTO.setOriginalValue(originCustomer);
                    logDTO.setModifiedValue(modifieCustomer);
                    return logDTO;
                }).toList();

        logService.batchAdd(logs);

        sendTransferNotice(clues, request.getOwner(), userId, orgId);
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(ids);
        List<String> owners = getOwners(clues);
        dataScopeService.checkDataPermission(userId, orgId, owners, PermissionConstants.CLUE_MANAGEMENT_DELETE);

        // 删除客户
        clueMapper.deleteByIds(ids);
        // 删除客户模块字段
        clueFieldService.deleteByResourceIds(ids);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(ids);
        // 删除跟进记录
        followUpRecordService.deleteByClueIds(ids);
        // 删除跟进计划
        followUpPlanService.deleteByClueIds(ids);

        // 消息通知
        clues.forEach(clue -> commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true));
    }

    private List<String> getOwners(List<Clue> clues) {
        return clues.stream()
                .map(Clue::getOwner)
                .distinct()
                .toList();
    }

    /**
     * 批量移入线索池
     *
     * @param request     请求参数
     * @param orgId       组织ID
     * @param currentUser 当前用户
     */
    public BatchAffectResponse batchToPool(BatchPoolReasonRequest request, String currentUser, String orgId) {
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Clue::getId, request.getIds());
        List<Clue> clues = clueMapper.selectListByLambda(wrapper);
        List<String> ownerIds = getOwners(clues);
        dataScopeService.checkDataPermission(currentUser, orgId, ownerIds, PermissionConstants.CLUE_MANAGEMENT_RECYCLE);

        Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        int success = 0;
        List<LogDTO> logs = new ArrayList<>();
        for (Clue clue : clues) {
            CluePool cluePool = ownersDefaultPoolMap.get(clue.getOwner());
            if (cluePool == null) {
                // 未找到默认公海，不移入
                continue;
            }
            // 日志
            LogDTO logDTO = new LogDTO(orgId, clue.getId(), currentUser, LogType.MOVE_TO_CUSTOMER_POOL, LogModule.CLUE_INDEX, clue.getName());
            String detail = Translator.getWithArgs("clue.to.pool", clue.getName(), cluePool.getName());
            logDTO.setDetail(detail);
            logs.add(logDTO);
            // 消息通知
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                    NotificationConstants.Event.CLUE_MOVED_POOL, clue.getName(), currentUser,
                    orgId, List.of(clue.getOwner()), true);
            // 插入责任人历史
            clue.setReasonId(request.getReasonId());
            clueOwnerHistoryService.add(clue, currentUser, true);
            clue.setPoolId(cluePool.getId());
            clue.setInSharedPool(true);
            clue.setOwner(null);
            clue.setCollectionTime(null);
            clue.setUpdateUser(currentUser);
            clue.setUpdateTime(System.currentTimeMillis());
            extClueMapper.moveToPool(clue);
            success++;
        }
        logService.batchAdd(logs);
        return BatchAffectResponse.builder().success(success).fail(request.getIds().size() - success).build();
    }

    /**
     * 移入线索池
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

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String organizationId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, organizationId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CLUE_MANAGEMENT_READ, rolePermissions);
    }


    public String getClueName(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(clue).map(Clue::getName).orElse(null);
    }

    public List<Clue> getClueListByNames(List<String> names) {
        LambdaQueryWrapper<Clue> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(Clue::getName, names);
        return clueMapper.selectListByLambda(lambdaQueryWrapper);
    }

    @SuppressWarnings("unchecked")
    public String getClueNameByIds(List<String> ids) {
        List<Clue> clueList = clueMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(clueList)) {
            List<String> names = clueList.stream().map(Clue::getName).toList();
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 批量关联线索和客户
     *
     * @param request     关联参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public void batchTransition(BatchReTransitionCustomerRequest request, String currentUser, String orgId) {
        if (CollectionUtils.isEmpty(request.getClueIds())) {
            throw new GenericException(Translator.get("clue_ids_not_empty"));
        }
        Customer customer = customerMapper.selectByPrimaryKey(request.getCustomerId());
        // 操作人尝试领取公海客户, 领取失败则不继续
        if (customer.getInSharedPool()) {
            PoolCustomerPickRequest pickRequest = new PoolCustomerPickRequest();
            pickRequest.setCustomerId(customer.getId());
            pickRequest.setPoolId(customer.getPoolId());
            poolCustomerService.pick(pickRequest, currentUser, orgId);
        }
        request.getClueIds().forEach(clueId -> transitionCs(clueId, customer, currentUser, orgId));
    }

    /**
     * 线索关联已有客户
     *
     * @param clueId      线索ID
     * @param currentUser 当前用户ID
     * @param orgId       组织ID
     */
    public void transitionCs(String clueId, Customer transitionCs, String currentUser, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        // 负责人不存在, 跳过关联
        List<String> owners = extUserMapper.selectUserNameByIds(List.of(clue.getOwner()));
        if (CollectionUtils.isEmpty(owners)) {
            return;
        }

        // 关联
        TransformCsAssociateDTO transformCsAssociateDTO = transformCsAssociate(clue, transitionCs, currentUser, orgId);
        clue.setTransitionId(transitionCs.getId());
        clue.setTransitionType("CUSTOMER");
        clueMapper.update(clue);

        // 转移线索的计划&记录
        batchCopyCluePlanAndRecord(clue.getId(), transitionCs.getId(), null, transformCsAssociateDTO.getContactId());
        // 刷新客户的最新跟进时间
        if (clue.getFollowTime() != null && (transitionCs.getFollowTime() == null || transitionCs.getFollowTime() < clue.getFollowTime())) {
            Customer updateCustomer = new Customer();
            updateCustomer.setId(transitionCs.getId());
            updateCustomer.setFollower(clue.getFollower());
            updateCustomer.setFollowTime(clue.getFollowTime());
            customerMapper.updateById(updateCustomer);
        }

        // 只通知线索负责人
        Map<String, Object> paramMap = new HashMap<>(8);
        paramMap.put("useTemplate", "true");
        paramMap.put("template", Translator.get("message.clue_relate_customer"));
        paramMap.put("customerName", transitionCs.getName());
        paramMap.put("name", clue.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_CUSTOMER,
                paramMap, currentUser, orgId, List.of(clue.getOwner()), true);
    }

    /**
     * 线索转换
     *
     * @param request     请求参数
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public String transform(ClueTransformRequest request, String currentUser, String orgId) {
        checkTransformPermission(request.getOppCreated());
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        if (clue == null) {
            throw new GenericException(Translator.get("clue_not_exist"));
        }
        List<String> owners = extUserMapper.selectUserNameByIds(List.of(clue.getOwner()));
        if (CollectionUtils.isEmpty(owners)) {
            throw new GenericException(Translator.get("clue_owner_not_exist"));
        }

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getName, clue.getName());
        List<Customer> customers = customerMapper.selectListByLambda(wrapper);
        boolean uniqueCheck = moduleFormService.hasFieldUniqueCheck(FormKey.CUSTOMER.getKey(), orgId, BusinessModuleField.CUSTOMER_NAME.getKey());
        Customer transformCustomer;
        if (uniqueCheck && CollectionUtils.isNotEmpty(customers)) {
            // 表单存在唯一性校验, 且同名客户存在, 根据规则选取
            transformCustomer = selectorCs(customers, clue.getOwner());
        } else {
            // 根据表单联动来创建客户
            transformCustomer = generateCustomerByLinkForm(clue, currentUser, orgId);
        }
        if (clue.getFollowTime() != null && (transformCustomer.getFollowTime() == null || transformCustomer.getFollowTime() < clue.getFollowTime())) {
            // 刷新同名客户最新跟进时间
            Customer updateCustomer = new Customer();
            updateCustomer.setId(transformCustomer.getId());
            updateCustomer.setFollower(clue.getFollower());
            updateCustomer.setFollowTime(clue.getFollowTime());
            customerMapper.updateById(updateCustomer);
        }

        TransformCsAssociateDTO transformCsAssociateDTO = transformCsAssociate(clue, transformCustomer, currentUser, orgId);
        clue.setTransitionId(transformCustomer.getId());
        clue.setTransitionType("CUSTOMER");
        clueMapper.update(clue);

        // 客户转换通知
        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put("useTemplate", "true");
        paramMap.put("template", Translator.get("message.clue_convert_customer_text"));
        paramMap.put("name", clue.getName());
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_CUSTOMER,
                paramMap, currentUser, orgId, List.of(clue.getOwner()), true);

        // 是否转换商机
        if (request.getOppCreated()) {
            transformCustomer.setName(request.getOppName());
            Opportunity transformOpportunity = generateOpportunityByLinkForm(clue, transformCsAssociateDTO.getContactId(), transformCustomer, currentUser, orgId);
            // 转移线索的计划&记录
            batchCopyCluePlanAndRecord(clue.getId(), transformCustomer.getId(), transformOpportunity.getId(), transformCsAssociateDTO.getContactId());
            paramMap.put("template", Translator.get("message.clue_convert_business_text"));
            paramMap.put("name", clue.getName());
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE, NotificationConstants.Event.CLUE_CONVERT_BUSINESS,
                    paramMap, currentUser, orgId, List.of(clue.getOwner()), true);
            return transformOpportunity.getId();
        } else {
            // 转移线索的计划&记录
            batchCopyCluePlanAndRecord(clue.getId(), transformCustomer.getId(), null, transformCsAssociateDTO.getContactId());
            return transformCustomer.getId();
        }
    }

    /**
     * 批量复制线索跟进计划和记录
     * @param clueId 线索ID
     * @param customerId 客户ID
     * @param opportunityId 商机ID
     */
    public void batchCopyCluePlanAndRecord(String clueId, String customerId, String opportunityId, String contactId) {
        // 记录
        LambdaQueryWrapper<FollowUpRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recordLambdaQueryWrapper.eq(FollowUpRecord::getClueId, clueId).eq(FollowUpRecord::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpRecord> followUpRecords = followUpRecordMapper.selectListByLambda(recordLambdaQueryWrapper);
        if (CollectionUtils.isNotEmpty(followUpRecords)) {
            List<String> ids = followUpRecords.stream().map(FollowUpRecord::getId).toList();
            LambdaQueryWrapper<FollowUpRecordField> fieldLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldLambdaQueryWrapper.in(FollowUpRecordField::getResourceId, ids);
            List<FollowUpRecordField> followUpRecordFields = followUpRecordFieldMapper.selectListByLambda(fieldLambdaQueryWrapper);
            LambdaQueryWrapper<FollowUpRecordFieldBlob> fieldBlobLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldBlobLambdaQueryWrapper.in(FollowUpRecordFieldBlob::getResourceId, ids);
            List<FollowUpRecordFieldBlob> followUpRecordFieldBlobs = followUpRecordFieldBlobMapper.selectListByLambda(fieldBlobLambdaQueryWrapper);
            followUpRecords.forEach(record -> {
                String recordId = IDGenerator.nextStr();
                followUpRecordFields.stream().filter(recordField ->Strings.CS.equals(recordField.getResourceId(), record.getId())).forEach(field -> {
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(recordId);
                });
                followUpRecordFieldBlobs.stream().filter(recordFieldBlob -> Strings.CS.equals(recordFieldBlob.getResourceId(), record.getId())).forEach(fieldBlob -> {
                    fieldBlob.setId(IDGenerator.nextStr());
                    fieldBlob.setResourceId(recordId);
                });
                record.setId(recordId);
                record.setClueId(null);
                record.setType(FollowUpPlanType.CUSTOMER.name());
                record.setCustomerId(customerId);
                record.setOpportunityId(opportunityId);
                record.setContactId(contactId);
            });
            followUpRecordMapper.batchInsert(followUpRecords);
            followUpRecordFieldMapper.batchInsert(followUpRecordFields);
            followUpRecordFieldBlobMapper.batchInsert(followUpRecordFieldBlobs);
        }
        // 计划
        LambdaQueryWrapper<FollowUpPlan> planLambdaQueryWrapper = new LambdaQueryWrapper<>();
        planLambdaQueryWrapper.eq(FollowUpPlan::getClueId, clueId).eq(FollowUpPlan::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpPlan> followUpPlans = followUpPlanMapper.selectListByLambda(planLambdaQueryWrapper);
        if (CollectionUtils.isNotEmpty(followUpPlans)) {
            List<String> ids = followUpPlans.stream().map(FollowUpPlan::getId).toList();
            LambdaQueryWrapper<FollowUpPlanField> fieldLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldLambdaQueryWrapper.in(FollowUpPlanField::getResourceId, ids);
            List<FollowUpPlanField> followUpPlanFields = followUpPlanFieldMapper.selectListByLambda(fieldLambdaQueryWrapper);
            LambdaQueryWrapper<FollowUpPlanFieldBlob> fieldBlobLambdaQueryWrapper = new LambdaQueryWrapper<>();
            fieldBlobLambdaQueryWrapper.in(FollowUpPlanFieldBlob::getResourceId, ids);
            List<FollowUpPlanFieldBlob> followUpPlanFieldBlobs = followUpPlanFieldBlobMapper.selectListByLambda(fieldBlobLambdaQueryWrapper);
            followUpPlans.forEach(plan -> {
                String planId = IDGenerator.nextStr();
                followUpPlanFields.stream().filter(planField ->Strings.CS.equals(planField.getResourceId(), plan.getId())).forEach(field -> {
                    field.setId(IDGenerator.nextStr());
                    field.setResourceId(planId);
                });
                followUpPlanFieldBlobs.stream().filter(planFieldBlob -> Strings.CS.equals(planFieldBlob.getResourceId(), plan.getId())).forEach(fieldBlob -> {
                    fieldBlob.setId(IDGenerator.nextStr());
                    fieldBlob.setResourceId(planId);
                });
                plan.setId(planId);
                plan.setClueId(null);
                plan.setType(FollowUpPlanType.CUSTOMER.name());
                plan.setCustomerId(customerId);
                plan.setOpportunityId(opportunityId);
                plan.setContactId(contactId);
            });
            followUpPlanMapper.batchInsert(followUpPlans);
            followUpPlanFieldMapper.batchInsert(followUpPlanFields);
            followUpPlanFieldBlobMapper.batchInsert(followUpPlanFieldBlobs);
        }
    }

    /**
     * 检查转换权限
     *
     * @param checkOpportunityPermission 是否检查商机权限
     */
    public void checkTransformPermission(boolean checkOpportunityPermission) {
        if (!PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_ADD)) {
            throw new GenericException(Translator.get("transform.miss.customer.permission"));
        }
        if (checkOpportunityPermission && !PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_ADD)) {
            throw new GenericException(Translator.get("transform.miss.opportunity.permission"));
        }
    }

    /**
     * 同名客户选择器
     *
     * @param customers 客户列表
     *
     * @return 客户
     */
    public Customer selectorCs(List<Customer> customers, String clueOwner) {
        if (customers.size() == 1) {
            return customers.getFirst();
        }
        // 优先选择不在公海的且负责人一致的客户
        Optional<Customer> find = customers.stream().filter(customer -> !customer.getInSharedPool() && Strings.CS.equals(customer.getOwner(), clueOwner))
                .findFirst();
        return find.orElseGet(customers::getFirst);
    }

    /**
     * 通过表单联动来创建客户
     *
     * @param clue        线索
     * @param currentUser 当前用户
     * @param orgId       组织ID
     *
     * @return 客户
     */
    public Customer generateCustomerByLinkForm(Clue clue, String currentUser, String orgId) {
        CustomerAddRequest addRequest = new CustomerAddRequest();
        ModuleFormConfigDTO customerFormConfig = moduleFormService.getBusinessFormConfig(FormKey.CUSTOMER.getKey(), orgId);
        FormLinkFill<Customer> customerLinkFillDTO;
        try {
            customerLinkFillDTO = moduleFormService.fillFormLinkValue(new Customer(), get(clue.getId(), orgId),
                    customerFormConfig, orgId, FormKey.CLUE.getKey(), LinkScenarioKey.CLUE_TO_CUSTOMER.name());
        } catch (Exception e) {
            LogUtils.error("Attempt to fill linked form values error: {}", e.getMessage());
            throw new GenericException(Translator.get("transform.customer.error"));
        }
        // 部分内置字段未配置联动, 取线索值即可
        addRequest.setName(customerLinkFillDTO.getEntity() == null || StringUtils.isEmpty(customerLinkFillDTO.getEntity().getName()) ?
                clue.getName() : customerLinkFillDTO.getEntity().getName());
        addRequest.setOwner(customerLinkFillDTO.getEntity() == null || StringUtils.isEmpty(customerLinkFillDTO.getEntity().getOwner()) ?
                clue.getOwner() : customerLinkFillDTO.getEntity().getOwner());
        addRequest.setModuleFields(customerLinkFillDTO.getFields());
        addRequest.setFollower(clue.getFollower());
        addRequest.setFollowTime(clue.getFollowTime());
        return customerService.add(addRequest, currentUser, orgId);
    }

    /**
     * 通过表单联动来创建商机
     *
     * @param clue        线索
     * @param contactId   联系人ID
     * @param currentUser 当前用户
     * @param orgId       组织ID
     *
     * @return 商机
     */
    public Opportunity generateOpportunityByLinkForm(Clue clue, String contactId, Customer transformCustomer, String currentUser, String orgId) {
        ModuleFormConfigDTO opportunityFormConfig = moduleFormService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        FormLinkFill<Opportunity> opportunityLinkFillDTO;
        try {
            opportunityLinkFillDTO = moduleFormService.fillFormLinkValue(new Opportunity(), get(clue.getId(), orgId),
                    opportunityFormConfig, orgId, FormKey.CLUE.getKey(), LinkScenarioKey.CLUE_TO_OPPORTUNITY.name());
        } catch (Exception e) {
            LogUtils.error("Attempt to fill linked form values error: {}", e.getMessage());
            throw new GenericException(Translator.get("transform.opportunity.error"));
        }
        OpportunityAddRequest addRequest = new OpportunityAddRequest();
        if (opportunityLinkFillDTO.getEntity() != null) {
            BeanUtils.copyBean(addRequest, opportunityLinkFillDTO.getEntity());
        }
        // 部分内置字段需手动设置值
        addRequest.setName(transformCustomer.getName());
        addRequest.setCustomerId(transformCustomer.getId());
        if (CollectionUtils.isEmpty(addRequest.getProducts())) {
            addRequest.setProducts(new ArrayList<>());
        }
        if (StringUtils.isEmpty(addRequest.getOwner())) {
            addRequest.setOwner(clue.getOwner());
        }
        if (StringUtils.isEmpty(addRequest.getContactId())) {
            addRequest.setContactId(contactId);
        }
        addRequest.setModuleFields(opportunityLinkFillDTO.getFields());
        addRequest.setFollower(clue.getFollower());
        addRequest.setFollowTime(clue.getFollowTime());
        return opportunityService.add(addRequest, currentUser, orgId);
    }

    /**
     * 转换客户处理
     *
     * @param clue        线索
     * @param transformCs 转换客户
     * @param currentUser 当前用户
     * @param orgId       组织ID
     */
    public TransformCsAssociateDTO transformCsAssociate(Clue clue, Customer transformCs, String currentUser, String orgId) {
        TransformCsAssociateDTO transformDTO = new TransformCsAssociateDTO();
        // 如果当前线索负责人不是关联客户的负责人，且不是客户协作人, 则添加协作关系
        if (!Strings.CS.equals(transformCs.getOwner(), clue.getOwner()) && !customerCollaborationService.hasCollaboration(clue.getOwner(), transformCs.getId())) {
            CustomerCollaborationAddRequest collaborationAddRequest = new CustomerCollaborationAddRequest();
            collaborationAddRequest.setCustomerId(transformCs.getId());
            collaborationAddRequest.setCollaborationType("COLLABORATION");
            collaborationAddRequest.setUserId(clue.getOwner());
            customerCollaborationService.add(collaborationAddRequest, currentUser, orgId);
        }
        // 线索联系人 => 客户联系人
        if (StringUtils.isNotEmpty(clue.getContact())) {
            boolean unique = customerContactService.checkCustomerContactUnique(clue.getContact(), clue.getPhone(), transformCs.getId(), orgId);
            if (unique) {
                CustomerContactAddRequest contactAddRequest = new CustomerContactAddRequest();
                contactAddRequest.setCustomerId(transformCs.getId());
                contactAddRequest.setName(clue.getContact());
                contactAddRequest.setPhone(clue.getPhone());
                contactAddRequest.setOwner(clue.getOwner());
                CustomerContact contact = customerContactService.add(contactAddRequest, currentUser, orgId);
                transformDTO.setContactId(contact.getId());
            }
        }

        return transformDTO;
    }

    /**
     * 下载导入的模板
     *
     * @param response 响应
     */
    public void downloadImportTpl(HttpServletResponse response, String currentOrg) {
        // 线索表单字段
        List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CLUE.getKey(), currentOrg);

        new EasyExcelExporter(Objects.class)
                .exportMultiSheetTplWithSharedHandler(response, fields.stream().map(field -> Collections.singletonList(field.getName())).toList(),
                        Translator.get("clue.import_tpl.name"), Translator.get(SheetKey.DATA), Translator.get(SheetKey.COMMENT), new CustomTemplateWriteHandler(fields), new CustomHeadColWidthStyleStrategy());
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
     * 线索导入
     *
     * @param file        导入文件
     * @param currentOrg  当前组织
     * @param currentUser 当前用户
     *
     * @return 导入返回信息
     */
    public ImportResponse realImport(MultipartFile file, String currentOrg, String currentUser) {
        try {
            List<BaseField> fields = moduleFormService.getAllFields(FormKey.CLUE.getKey(), currentOrg);
            CustomImportAfterDoConsumer<Clue, BaseResourceField> afterDo = (clues, clueFields, clueFieldBlobs) -> {
                List<LogDTO> logs = new ArrayList<>();
                clues.forEach(clue -> {
                    clue.setCollectionTime(clue.getCreateTime());
                    clue.setStage(ClueStatus.NEW.name());
                    clue.setInSharedPool(false);
                    logs.add(new LogDTO(currentOrg, clue.getId(), currentUser, LogType.ADD, LogModule.CLUE_INDEX, clue.getName()));
                });
                clueMapper.batchInsert(clues);
                clueFieldMapper.batchInsert(clueFields.stream().map(field -> BeanUtils.copyBean(new ClueField(), field)).toList());
                clueFieldBlobMapper.batchInsert(clueFieldBlobs.stream().map(field -> BeanUtils.copyBean(new ClueFieldBlob(), field)).toList());
                // record logs
                logService.batchAdd(logs);
            };
            CustomFieldImportEventListener<Clue> eventListener = new CustomFieldImportEventListener<>(fields, Clue.class, currentOrg, currentUser,
                    "clue_field", afterDo, 2000);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getDataList().size()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("clue import error: ", e.getMessage());
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
            List<BaseField> fields = moduleFormService.getCustomImportHeads(FormKey.CLUE.getKey(), currentOrg);
            CustomFieldCheckEventListener eventListener = new CustomFieldCheckEventListener(fields, "clue", "clue_field", currentOrg);
            FastExcelFactory.read(file.getInputStream(), eventListener).headRowNumber(1).ignoreEmptyRow(true).sheet().doRead();
            return ImportResponse.builder().errorMessages(eventListener.getErrList())
                    .successCount(eventListener.getSuccess()).failCount(eventListener.getErrList().size()).build();
        } catch (Exception e) {
            LogUtils.error("clue import pre-check error: {}", e.getMessage());
            throw new GenericException(e.getMessage());
        }
    }

    public void batchUpdate(ResourceBatchEditRequest request, String userId, String organizationId) {
        BaseField field = clueFieldService.getAndCheckField(request.getFieldId(), organizationId);
        if (Strings.CS.equals(field.getBusinessKey(), BusinessModuleField.CLUE_OWNER.getBusinessKey())) {
            // 修改负责人，走批量转移接口
            ClueBatchTransferRequest batchTransferRequest = new ClueBatchTransferRequest();
            batchTransferRequest.setIds(request.getIds());
            batchTransferRequest.setOwner(request.getFieldValue().toString());
            batchTransfer(batchTransferRequest, userId, organizationId);
            return;
        }

        List<Clue> originClues = clueMapper.selectByIds(request.getIds());

        clueFieldService.batchUpdate(request, field, originClues, Clue.class, LogModule.CLUE_INDEX, extClueMapper::batchUpdate, userId, organizationId);
    }

    public List<ChartResult> chart(ChartAnalysisRequest request, String userId, String orgId, DeptDataPermissionDTO deptDataPermission) {
        ModuleFormConfigDTO formConfig = getFormConfig(orgId);
        formConfig.getFields().addAll(BaseResourceFieldService.getChartBaseFields());
        ChartAnalysisDbRequest chartAnalysisDbRequest = ConditionFilterUtils.parseChartAnalysisRequest(request, formConfig);
        ClueChartAnalysisDbRequest clueChartAnalysisDbRequest = BeanUtils.copyBean(new ClueChartAnalysisDbRequest(), chartAnalysisDbRequest);
        List<ChartResult> chartResults = extClueMapper.chart(clueChartAnalysisDbRequest, userId, orgId, deptDataPermission);
        return clueFieldService.translateAxisName(formConfig, chartAnalysisDbRequest, chartResults);
    }

    /**
     * 处理已转移线索的跟进计划和记录
     */
    public void processTransferredCluePlanAndRecord() {
        List<FollowUpRecord> records = new ArrayList<>();
        List<FollowUpPlan> plans = new ArrayList<>();
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clue::getTransitionType, "CUSTOMER");
        List<Clue> clues = clueMapper.selectListByLambda(wrapper);
        List<Clue> transferredClues = clues.stream().filter(clue -> StringUtils.isNotBlank(clue.getTransitionId())).toList();
        Map<String, String> clueTransferMap = transferredClues.stream().collect(Collectors.toMap(Clue::getId, Clue::getTransitionId));
        List<String> clueIds = transferredClues.stream().map(Clue::getId).toList();
        // 记录
        LambdaQueryWrapper<FollowUpRecord> recordLambdaQueryWrapper = new LambdaQueryWrapper<>();
        recordLambdaQueryWrapper.in(FollowUpRecord::getClueId, clueIds).eq(FollowUpRecord::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpRecord> clueRecords = followUpRecordMapper.selectListByLambda(recordLambdaQueryWrapper);
        Map<String, ClueFollowDTO> followDTOMap = new HashMap<>();
        clueRecords.forEach(clueRecord -> {
            String customerId = clueTransferMap.get(clueRecord.getClueId());
            clueRecord.setId(IDGenerator.nextStr());
            clueRecord.setCustomerId(customerId);
            clueRecord.setClueId(null);
            clueRecord.setType(FollowUpPlanType.CUSTOMER.name());
            if (StringUtils.isNotBlank(clueRecord.getCustomerId())) {
                records.add(clueRecord);
                ClueFollowDTO clueFollowDTO = followDTOMap.get(customerId);
                if (clueFollowDTO == null) {
                    followDTOMap.put(customerId, ClueFollowDTO.builder().follower(clueRecord.getOwner())
                            .followerTime(clueRecord.getFollowTime()).build());
                } else {
                    if (clueRecord.getFollowTime() != null && (clueFollowDTO.getFollowerTime() == null || clueRecord.getFollowTime() > clueFollowDTO.getFollowerTime())) {
                        clueFollowDTO.setFollower(clueRecord.getOwner());
                        clueFollowDTO.setFollowerTime(clueRecord.getFollowTime());
                        followDTOMap.put(customerId, clueFollowDTO);
                    }
                }
            }
        });
        // 计划
        LambdaQueryWrapper<FollowUpPlan> planLambdaQueryWrapper = new LambdaQueryWrapper<>();
        planLambdaQueryWrapper.in(FollowUpPlan::getClueId, clueIds).eq(FollowUpPlan::getType, FollowUpPlanType.CLUE.name());
        List<FollowUpPlan> cluePlans = followUpPlanMapper.selectListByLambda(planLambdaQueryWrapper);
        cluePlans.forEach(cluePlan -> {
            cluePlan.setId(IDGenerator.nextStr());
            cluePlan.setCustomerId(clueTransferMap.get(cluePlan.getClueId()));
            cluePlan.setClueId(null);
            cluePlan.setType(FollowUpPlanType.CUSTOMER.name());
            if (StringUtils.isNotBlank(cluePlan.getCustomerId())) {
                plans.add(cluePlan);
            }
        });
        // 批量插入
        if (CollectionUtils.isNotEmpty(records)) {
            followUpRecordMapper.batchInsert(records);
        }
        if (CollectionUtils.isNotEmpty(plans)) {
            followUpPlanMapper.batchInsert(plans);
        }
        // 更新客户最新跟进人和时间
        List<String> customerIds = followDTOMap.keySet().stream().toList();
        List<Customer> customers = customerMapper.selectByIds(customerIds);
        customers.forEach(customer -> {
            if (customer.getFollowTime() == null || customer.getFollowTime() < followDTOMap.get(customer.getId()).getFollowerTime()) {
                customer.setFollower(followDTOMap.get(customer.getId()).getFollower());
                customer.setFollowTime(followDTOMap.get(customer.getId()).getFollowerTime());
            }
            customerMapper.updateById(customer);
        });
    }
}