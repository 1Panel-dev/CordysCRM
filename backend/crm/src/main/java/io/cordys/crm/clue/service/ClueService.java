package io.cordys.crm.clue.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.service.DataScopeService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.clue.constants.ClueResultCode;
import io.cordys.crm.clue.constants.ClueStatus;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRecycleRule;
import io.cordys.crm.clue.dto.request.*;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.service.CustomerService;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.service.OpportunityService;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.dto.response.BatchAffectResponse;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
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
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private DataScopeService dataScopeService;

    public PagerWithOption<List<ClueListResponse>> list(CluePageRequest request, String userId, String orgId,
                                                        DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission);
        List<ClueListResponse> buildList = buildListData(list, orgId);

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

        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    private List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
        if(CollectionUtils.isEmpty(list)){
            return list;
        }
        List<String> clueIds = list.stream().map(ClueListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = clueFieldService.getResourceFieldMap(clueIds);

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
        dataScopeService.checkDataPermission(userId, orgId, getResponse.getOwner());
        return getResponse;
    }

    public ClueGetResponse get(String id, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);

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

        clueGetResponse.setOptionMap(optionMap);
        clueGetResponse.setModuleFields(clueFields);

        if (clueGetResponse.getOwner() != null) {
            UserDeptDTO userDeptDTO = baseService.getUserDeptMapByUserId(clueGetResponse.getOwner(), orgId);
            if (userDeptDTO != null) {
                clueGetResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueGetResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        }

        return baseService.setCreateUpdateOwnerUserName(clueGetResponse);
    }

    public Clue add(ClueAddRequest request, String userId, String orgId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(clue.getCreateTime());
        clue.setUpdateUser(userId);
        clue.setCreateUser(userId);
        clue.setOrganizationId(orgId);
        clue.setId(IDGenerator.nextStr());
        clue.setStage(ClueStatus.NEW.name());
        clue.setInSharedPool(false);

        // 校验名称重复
        checkAddExist(clue);

        clueMapper.insert(clue);

        //保存自定义字段
        clueFieldService.saveModuleField(clue.getId(), orgId, userId, request.getModuleFields());
        return clue;
    }

    public Clue update(ClueUpdateRequest request, String userId, String orgId) {
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        dataScopeService.checkDataPermission(userId, orgId, originClue.getOwner());

        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);

        // 校验名称重复
        checkUpdateExist(clue);

        if (StringUtils.isNotBlank(request.getOwner())) {
            if (!StringUtils.equals(request.getOwner(), originClue.getOwner())) {
                // 如果责任人有修改，则添加责任人历史
                clueOwnerHistoryService.add(originClue, userId);
                sendTransferNotice(List.of(originClue), request.getOwner(), userId, orgId);
            }
        }

        clueMapper.update(clue);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    private void sendTransferNotice(List<Clue> originClues, String toUser, String userId, String orgId) {
        String customerNames = getClueNames(originClues);

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.TRANSFER_CLUE, customerNames, userId,
                orgId, List.of(toUser), true);
    }

    private String getClueNames(List<Clue> clues) {
        return String.join(";",
                clues.stream()
                        .map(Clue::getName)
                        .toList()
        );
    }

    public void updateStatus(ClueStatusUpdateRequest request, String userId, String orgId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        Clue originClue = clueMapper.selectByPrimaryKey(request.getId());
        // 记录修改前的状态
        clue.setLastStage(originClue.getStage());
        clueMapper.update(clue);
    }

    private void updateModuleField(String clueId, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        clueFieldService.deleteByResourceId(clueId);
        // 再保存
        clueFieldService.saveModuleField(clueId, orgId, userId, moduleFields);
    }

    private void checkAddExist(Clue clue) {
        if (extClueMapper.checkAddExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
    }

    private void checkUpdateExist(Clue clue) {
        if (extClueMapper.checkUpdateExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
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
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner());
        clue.setTransitionId(customer.getId());
        clue.setTransitionType(FormKey.CUSTOMER.name());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        clueMapper.update(clue);

        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_CONVERT_CUSTOMER, customer.getName(), userId,
                orgId, List.of(customer.getOwner()), true);
    }

    /**
     * 转移商机
     * @param request 请求参数
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    public void transitionOpportunity(ClueTransitionOpportunityRequest request, String userId, String orgId) {
        Opportunity opportunity = opportunityService.add(request, userId, orgId);
        Clue clue = clueMapper.selectByPrimaryKey(request.getClueId());
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner());

        clue.setTransitionId(opportunity.getId());
        clue.setTransitionType(FormKey.OPPORTUNITY.name());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);
        clueMapper.update(clue);
    }

    public void delete(String id, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        dataScopeService.checkDataPermission(userId, orgId, clue.getOwner());
        // 删除客户
        clueMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        clueFieldService.deleteByResourceId(id);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(List.of(id));

        // 消息通知
        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                NotificationConstants.Event.CLUE_DELETED, clue.getName(), userId,
                orgId, List.of(clue.getOwner()), true);

    }

    public void batchTransfer(ClueBatchTransferRequest request, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(request.getIds());
        List<String> ownerIds = getOwners(clues);
        dataScopeService.checkDataPermission(userId, orgId, ownerIds);

        // 添加责任人历史
        clueOwnerHistoryService.batchAdd(request, userId);
        extClueMapper.batchTransfer(request);

        sendTransferNotice(clues, request.getOwner(), userId, orgId);
    }

    public void batchDelete(List<String> ids, String userId, String orgId) {
        List<Clue> clues = clueMapper.selectByIds(ids);
        List<String> owners = getOwners(clues);
        dataScopeService.checkDataPermission(userId, orgId, owners);

        // 删除客户
        clueMapper.deleteByIds(ids);
        // 删除客户模块字段
        clueFieldService.deleteByResourceIds(ids);
        // 删除责任人历史
        clueOwnerHistoryService.deleteByClueIds(ids);

        // 消息通知
        clues.stream()
                .collect(Collectors.groupingBy(Clue::getOwner))
                .forEach((owner, customerList) ->
                        commonNoticeSendService.sendNotice(NotificationConstants.Module.CLUE,
                                NotificationConstants.Event.CLUE_DELETED, getClueNames(customerList), userId,
                                orgId, List.of(owner), true)
                );
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
        dataScopeService.checkDataPermission(currentUser, orgId, ownerIds);

        Map<String, CluePool> ownersDefaultPoolMap = cluePoolService.getOwnersDefaultPoolMap(ownerIds, orgId);
        int success = 0;
        for (Clue clue : clues) {
            CluePool cluePool = ownersDefaultPoolMap.get(clue.getOwner());
            if (cluePool == null) {
                // 未找到默认公海，不移入
                continue;
            }
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
        return BatchAffectResponse.builder().success(success).fail(ids.size() - success).build();
    }
}