package io.cordys.crm.opportunity.service;

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
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.permission.PermissionCache;
import io.cordys.common.permission.PermissionUtils;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.opportunity.constants.StageType;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.request.*;
import io.cordys.crm.opportunity.dto.response.OpportunityDetailResponse;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.notice.CommonNoticeSendService;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityService {

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private OpportunityFieldService opportunityFieldService;
    @Resource
    private LogService logService;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private BaseMapper<Product> productMapper;
    @Autowired
    private OpportunityRuleService opportunityRuleService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private CommonNoticeSendService commonNoticeSendService;
    @Resource
    private PermissionCache permissionCache;

    public PagerWithOption<List<OpportunityListResponse>> list(OpportunityPageRequest request, String userId, String orgId,
                                                               DeptDataPermissionDTO deptDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId, userId, deptDataPermission);
        List<OpportunityListResponse> buildList = buildListData(list, orgId);

        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, OpportunityListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getOwner, OpportunityListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                OpportunityListResponse::getContactId, OpportunityListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);


        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    private List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list, String orgId) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<BaseModuleFieldValue>> opportunityFiledMap = opportunityFieldService.getResourceFieldMap(opportunityIds, true);

        List<String> ownerIds = list.stream()
                .map(OpportunityListResponse::getOwner)
                .distinct()
                .toList();

        List<String> followerIds = list.stream()
                .map(OpportunityListResponse::getFollower)
                .distinct()
                .toList();
        List<String> createUserIds = list.stream()
                .map(OpportunityListResponse::getCreateUser)
                .distinct()
                .toList();
        List<String> updateUserIds = list.stream()
                .map(OpportunityListResponse::getUpdateUser)
                .distinct()
                .toList();
        List<String> userIds = Stream.of(ownerIds, followerIds, createUserIds, updateUserIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream()
                .map(OpportunityListResponse::getContactId)
                .distinct()
                .toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);

        Map<String, OpportunityRule> ownersDefaultRuleMap = opportunityRuleService.getOwnersDefaultRuleMap(ownerIds, orgId);
        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            opportunityListResponse.setReservedDays(BooleanUtils.isFalse(opportunityListResponse.getStatus()) ? null : opportunityRuleService.calcReservedDay(ownersDefaultRuleMap.get(opportunityListResponse.getOwner()), opportunityListResponse));
            opportunityListResponse.setModuleFields(opportunityFields);

            opportunityListResponse.setFollowerName(userNameMap.get(opportunityListResponse.getFollower()));
            opportunityListResponse.setCreateUserName(userNameMap.get(opportunityListResponse.getCreateUser()));
            opportunityListResponse.setUpdateUserName(userNameMap.get(opportunityListResponse.getUpdateUser()));
            opportunityListResponse.setOwnerName(userNameMap.get(opportunityListResponse.getOwner()));
            opportunityListResponse.setContactName(contactMap.get(opportunityListResponse.getContactId()));

            UserDeptDTO userDeptDTO = userDeptMap.get(opportunityListResponse.getOwner());
            if (userDeptDTO != null) {
                opportunityListResponse.setDepartmentId(userDeptDTO.getDeptId());
                opportunityListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }

        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    /**
     * 新建商机
     *
     * @param request
     * @param operatorId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.ADD, resourceName = "{#request.name}")
    public Opportunity add(OpportunityAddRequest request, String operatorId, String orgId) {
        checkOpportunity(request, orgId, null);
        Opportunity opportunity = new Opportunity();
        String id = IDGenerator.nextStr();
        opportunity.setId(id);
        opportunity.setName(request.getName());
        opportunity.setCustomerId(request.getCustomerId());
        opportunity.setAmount(request.getAmount());
        opportunity.setPossible(request.getPossible());
        opportunity.setProducts(request.getProducts());
        opportunity.setOrganizationId(orgId);
        opportunity.setStage(StageType.CREATE.name());
        opportunity.setContactId(request.getContactId());
        opportunity.setOwner(request.getOwner());
        opportunity.setCreateTime(System.currentTimeMillis());
        opportunity.setCreateUser(operatorId);
        opportunity.setUpdateTime(System.currentTimeMillis());
        opportunity.setUpdateUser(operatorId);
        opportunity.setStatus(true);
        if (StringUtils.isBlank(request.getOwner())) {
            opportunity.setOwner(operatorId);
        }
        opportunityMapper.insert(opportunity);

        //自定义字段
        opportunityFieldService.saveModuleField(id, orgId, operatorId, request.getModuleFields());
        baseService.handleAddLog(opportunity, request.getModuleFields());

        return opportunity;
    }


    /**
     * 校验商机
     *
     * @param request
     * @param orgId
     * @param id
     */
    private void checkOpportunity(OpportunityAddRequest request, String orgId, String id) {
        List<String> products = extOpportunityMapper.selectByProducts(request, orgId, id);
        if (CollectionUtils.isNotEmpty(products)) {
            List<String> ids = JSON.parseArray(products.getFirst(), String.class);
            String projectId = request.getProducts().stream()
                    .filter(ids::contains)
                    .toList().getFirst();
            Product product = productMapper.selectByPrimaryKey(projectId);

            throw new GenericException(String.format(Translator.get("opportunity_exist"), product.getName()));
        }
    }


    /**
     * 更新商机
     *
     * @param request
     * @param userId
     * @param orgId
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.UPDATE, resourceId = "{#request.id}")
    public Opportunity update(OpportunityUpdateRequest request, String userId, String orgId) {
        Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(oldOpportunity).ifPresentOrElse(item -> {
            Opportunity newOpportunity = BeanUtils.copyBean(new Opportunity(), item);
            checkOpportunity(request, orgId, request.getId());
            //更新商机
            updateOpportunity(newOpportunity, request, userId);
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = opportunityFieldService.getModuleFieldValuesByResourceId(request.getId());
            //更新模块字段
            updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);
            baseService.handleUpdateLog(oldOpportunity, newOpportunity, originCustomerFields, request.getModuleFields(), oldOpportunity.getId(), oldOpportunity.getName());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        return oldOpportunity;
    }


    private void updateOpportunity(Opportunity item, OpportunityUpdateRequest request, String userId) {
        item.setName(request.getName());
        item.setCustomerId(request.getCustomerId());
        item.setAmount(request.getAmount());
        item.setPossible(request.getPossible());
        item.setProducts(request.getProducts());
        item.setContactId(request.getContactId());
        item.setOwner(request.getOwner());
        item.setUpdateTime(System.currentTimeMillis());
        item.setUpdateUser(userId);
        opportunityMapper.update(item);
    }


    private void updateModuleField(String id, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        opportunityFieldService.deleteByResourceId(id);
        // 再保存
        opportunityFieldService.saveModuleField(id, orgId, userId, moduleFields);
    }


    /**
     * 删除商机
     *
     * @param id
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String userId, String orgId) {
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(id);
        Optional.ofNullable(opportunity).ifPresentOrElse(item -> {
            opportunityMapper.deleteByPrimaryKey(opportunity.getId());
            opportunityFieldService.deleteByResourceId(opportunity.getId());
        }, () -> {
            throw new GenericException("opportunity_not_found");
        });
        // 添加日志上下文
        OperationLogContext.setResourceName(opportunity.getName());

        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_DELETED, opportunity.getName(), userId,
                orgId, List.of(opportunity.getOwner()), true);
    }


    /**
     * 商机转移
     *
     * @param request
     */
    public void transfer(OpportunityTransferRequest request, String userId, String orgId) {
        extOpportunityMapper.batchTransfer(request, userId, System.currentTimeMillis());
        List<Opportunity> opportunityList = opportunityMapper.selectByIds(request.getIds());
        // 记录日志
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.forEach(customer -> {
            Customer originCustomer = new Customer();
            originCustomer.setOwner(customer.getOwner());
            Customer modifieCustomer = new Customer();
            modifieCustomer.setOwner(request.getOwner());
            LogDTO logDTO = new LogDTO(orgId, customer.getId(), userId, LogType.UPDATE, LogModule.OPPORTUNITY, customer.getName());
            logDTO.setOriginalValue(originCustomer);
            logDTO.setModifiedValue(modifieCustomer);
            logs.add(logDTO);
        });

        logService.batchAdd(logs);
        sendTransferNotice(opportunityList, request.getOwner(), userId, orgId);
    }

    private void sendTransferNotice(List<Opportunity> opportunityList, String toUser, String userId, String orgId) {
        String opportunityNames = getOpportunityNames(opportunityList);

        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                NotificationConstants.Event.BUSINESS_TRANSFER, opportunityNames, userId,
                orgId, List.of(toUser), true);
    }

    /**
     * 批量删除商机
     *
     * @param ids
     * @param userId
     */
    public void batchDelete(List<String> ids, String userId, String orgId) {
        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Opportunity::getId, ids);
        List<Opportunity> opportunityList = opportunityMapper.selectListByLambda(wrapper);
        opportunityMapper.deleteByIds(ids);
        opportunityFieldService.deleteByResourceIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        opportunityList.forEach(opportunity -> {
            LogDTO logDTO = new LogDTO(opportunity.getOrganizationId(), opportunity.getId(), userId, LogType.DELETE, LogModule.OPPORTUNITY, opportunity.getName());
            logDTO.setOriginalValue(opportunity);
            logs.add(logDTO);
        });
        logService.batchAdd(logs);

        // 消息通知
        opportunityList.stream()
                .collect(Collectors.groupingBy(Opportunity::getOwner))
                .forEach((owner, opportunity) ->
                        commonNoticeSendService.sendNotice(NotificationConstants.Module.OPPORTUNITY,
                                NotificationConstants.Event.BUSINESS_DELETED, getOpportunityNames(opportunity), userId,
                                orgId, List.of(owner), true)
                );
    }

    private String getOpportunityNames(List<Opportunity> opportunity) {
        return String.join(";",
                opportunity.stream()
                        .map(Opportunity::getName)
                        .toList()
        );
    }


    /**
     * 商机详情
     *
     * @param id
     * @param orgId
     * @return
     */
    public OpportunityDetailResponse get(String id, String orgId) {
        OpportunityDetailResponse response = extOpportunityMapper.getDetail(id);
        List<BaseModuleFieldValue> fieldValueList = opportunityFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        List<String> userIds = Stream.of(List.of(response.getCreateUser(), response.getUpdateUser(), response.getOwner()))
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);
        Map<String, String> contactMap = baseService.getContactMap(List.of(response.getContactId()));

        response.setCreateUserName(userNameMap.get(response.getCreateUser()));
        response.setUpdateUserName(userNameMap.get(response.getUpdateUser()));
        response.setOwnerName(userNameMap.get(response.getOwner()));
        response.setContactName(contactMap.get(response.getContactId()));


        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, fieldValueList);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getOwner, OpportunityDetailResponse::getOwnerName);
        optionMap.put(BusinessModuleField.CUSTOMER_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getContactId, OpportunityDetailResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);

        List<OptionDTO> customerOption = moduleFormService.getBusinessFieldOption(response,
                OpportunityDetailResponse::getCustomerId, OpportunityDetailResponse::getCustomerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey(), customerOption);

        List<OptionDTO> productOption = extProductMapper.getOptions(orgId);
        optionMap.put(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey(), productOption);

        response.setOptionMap(optionMap);

        return response;
    }


    /**
     * 标记商机阶段
     *
     * @param request
     */
    @OperationLog(module = LogModule.OPPORTUNITY, type = LogType.UPDATE, resourceId = "{#request.id}")
    public void updateStage(OpportunityStageRequest request) {
        Opportunity oldOpportunity = opportunityMapper.selectByPrimaryKey(request.getId());
        Opportunity newOpportunity = new Opportunity();
        if (StringUtils.equalsAnyIgnoreCase(request.getStage(), StageType.SUCCESS.name(), StageType.FAIL.name())) {
            newOpportunity.setStatus(false);
        }
        newOpportunity.setId(request.getId());
        newOpportunity.setLastStage(oldOpportunity.getStage());
        newOpportunity.setStage(request.getStage());
        opportunityMapper.update(newOpportunity);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceName(oldOpportunity.getName())
                        .originalValue(oldOpportunity)
                        .modifiedValue(opportunityMapper.selectByPrimaryKey(request.getId()))
                        .build()
        );
    }

    public ResourceTabEnableDTO getTabEnableConfig(String userId, String orgId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(userId, orgId);
        return PermissionUtils.getTabEnableConfig(userId, PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, rolePermissions);
    }
}
