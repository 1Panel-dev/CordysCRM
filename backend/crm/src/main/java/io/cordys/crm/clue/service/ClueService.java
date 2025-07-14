package io.cordys.crm.clue.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.*;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.permission.PermissionUtils;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.constants.ClueStatus;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.request.*;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import io.cordys.crm.customer.dto.request.ReTransitionCustomerRequest;
import io.cordys.crm.customer.service.CustomerCollaborationService;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.customer.service.PoolCustomerService;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.dto.response.BatchAffectResponse;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.crm.system.service.ProductService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private CustomerService customerService;
    @Resource
    private ClueFieldService clueFieldService;
    @Resource
    private CluePoolService cluePoolService;
    @Resource
    private BaseMapper<CluePoolRecycleRule> recycleRuleMapper;
    @Resource
    private ClueOwnerHistoryService clueOwnerHistoryService;
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

    public PagerWithOption<List<ClueListResponse>> list(CluePageRequest request, String userId, String orgId,
                                                        DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission);
        List<ClueListResponse> buildList = buildListData(list, orgId);

        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    @NotNull
    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<ClueListResponse> list, List<ClueListResponse> buildList) {
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

    public List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
        if(CollectionUtils.isEmpty(list)){
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
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.CLUE.getKey(), orgId);
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

        if (clueGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(clueGetResponse.getOwner(), orgId);
            if (userDeptDTO != null) {
                clueGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

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
        if (!StringUtils.equals(originClue.getOwner(), request.getOwner())) {
            poolClueService.validateCapacity(1, request.getOwner(), orgId);
        }
        dataScopeService.checkDataPermission(userId, orgId, originClue.getOwner(), PermissionConstants.CLUE_MANAGEMENT_UPDATE);

        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!StringUtils.equals(request.getOwner(), originClue.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                clueOwnerHistoryService.add(originClue, userId);
                sendTransferNotice(List.of(originClue), request.getOwner(), userId, orgId);
            }
        }

        // 获取模块字段
        List<BaseModuleFieldValue> originCustomerFields = clueFieldService.getModuleFieldValuesByResourceId(request.getId());

        // 更新模块字段
        updateModuleField(clue, request.getModuleFields(), orgId, userId);

        clueMapper.update(clue);
        clue = clueMapper.selectByPrimaryKey(request.getId());
        baseService.handleUpdateLog(originClue, clue, originCustomerFields, request.getModuleFields(), originClue.getId(), originClue.getName());
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    private void sendTransferNotice(List<Clue> originClues, String toUser, String userId, String orgId) {
        originClues.forEach(clue -> {
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                    NotificationConstants.Event.TRANSFER_CLUE, clue.getName(), userId,
                    orgId, List.of(toUser), true);
        });
    }

    private String getClueNames(List<Clue> clues) {
        return String.join(";",
                clues.stream()
                        .map(Clue::getName)
                        .toList()
        );
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
     * @param request 请求参数
     * @param userId 用户ID
     * @param orgId 组织ID
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
        long processCount = clues.stream().filter(clue -> !StringUtils.equals(clue.getOwner(), request.getOwner())).count();
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

        // 消息通知
        clues.forEach(clue -> {
            commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                    NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                    orgId, List.of(clue.getOwner()), true);
        });
    }

    private List<String> getOwners(List<Clue> clues) {
        return clues.stream()
                .map(Clue::getOwner)
                .distinct()
                .toList();
    }

    /**
     * 批量移入线索池
     * @param ids id集合
     * @param orgId 组织ID
     * @param currentUser 当前用户
     */
    public BatchAffectResponse batchToPool(List<String> ids, String currentUser, String orgId) {
        LambdaQueryWrapper<Clue> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Clue::getId, ids);
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
            clueOwnerHistoryService.add(clue, currentUser);
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
        return BatchAffectResponse.builder().success(success).fail(ids.size() - success).build();
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String organizationId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, organizationId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.CLUE_MANAGEMENT_READ, rolePermissions);
    }


    public String getClueName(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        return Optional.ofNullable(clue).map(Clue::getName).orElse(null);
    }


    public String getClueNameByIds(List<String> ids) {
        List<Clue> clueList = clueMapper.selectByIds(ids);
        if (CollectionUtils.isNotEmpty(clueList)) {
            List<String> names = clueList.stream().map(Clue::getName).toList();
            return String.join(",", JSON.parseArray(JSON.toJSONString(names)));
        }
        return StringUtils.EMPTY;
    }

    public void transitionOldCustomer(ReTransitionCustomerRequest request, String currentUser, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(request.getCustomerId());
        if (customer.getInSharedPool()) {
            // 如果客户已经在公海中，则领取改客户
            PoolCustomerPickRequest pickRequest = new PoolCustomerPickRequest();
            pickRequest.setCustomerId(request.getCustomerId());
            pickRequest.setPoolId(customer.getPoolId());
            poolCustomerService.pick(pickRequest, currentUser, orgId);
        } else {
            // 如果客户不在公海中，则加入到协作人
            CustomerCollaborationAddRequest collaborationAddRequest = new CustomerCollaborationAddRequest();
            collaborationAddRequest.setCustomerId(request.getCustomerId());
            collaborationAddRequest.setCollaborationType("COLLABORATION");
            collaborationAddRequest.setUserId(currentUser);
            customerCollaborationService.add(collaborationAddRequest, currentUser);
        }
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        clue.setTransitionId(request.getCustomerId());
        clue.setTransitionType("CUSTOMER");
        clueMapper.update(clue);
    }
}