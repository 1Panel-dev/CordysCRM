package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.*;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.permission.PermissionCache;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.constants.FollowUpPlanStatusType;
import cn.cordys.crm.follow.constants.FollowUpPlanType;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.follow.dto.request.*;
import cn.cordys.crm.follow.dto.response.FollowUpPlanDetailResponse;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.dto.response.FollowUpRecordDetailResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpPlanService extends BaseFollowUpService {

    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private FollowUpPlanFieldService followUpPlanFieldService;
    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private DataScopeService dataScopeService;

    /**
     * 新建跟进计划
     *
     * @param request
     * @param userId
     * @param orgId
     *
     * @return
     */
    public FollowUpPlan add(FollowUpPlanAddRequest request, String userId, String orgId) {
        FollowUpPlan followUpPlan = BeanUtils.copyBean(new FollowUpPlan(), request);
        followUpPlan.setCreateTime(System.currentTimeMillis());
        followUpPlan.setUpdateTime(System.currentTimeMillis());
        followUpPlan.setUpdateUser(userId);
        followUpPlan.setCreateUser(userId);
        followUpPlan.setId(IDGenerator.nextStr());
        followUpPlan.setOrganizationId(orgId);
        followUpPlan.setStatus(FollowUpPlanStatusType.PREPARED.name());
        followUpPlan.setConverted(false);
        if (StringUtils.isBlank(request.getOwner())) {
            followUpPlan.setOwner(userId);
        }
        //保存自定义字段
        followUpPlanFieldService.saveModuleField(followUpPlan, orgId, userId, request.getModuleFields(), false);
        followUpPlanMapper.insert(followUpPlan);
        return followUpPlan;
    }


    /**
     * 更新跟进计划
     *
     * @param request
     * @param userId
     * @param orgId
     *
     * @return
     */
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE, resourceId = "{#request.id}")
    public FollowUpPlan update(FollowUpPlanUpdateRequest request, String userId, String orgId) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(followUpPlan).ifPresentOrElse(plan -> {
            //更新跟进计划
            FollowUpPlan newPlan = BeanUtils.copyBean(new FollowUpPlan(), plan);
            FollowUpPlan updateFollowUpPlan = newPlan(newPlan, request, userId);
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = followUpPlanFieldService.getModuleFieldValuesByResourceId(request.getId());
            //更新模块字段
            updateModuleField(updateFollowUpPlan, request.getModuleFields(), orgId, userId);
            followUpPlanMapper.update(updateFollowUpPlan);
            baseService.handleUpdateLog(followUpPlan, updateFollowUpPlan, originCustomerFields, request.getModuleFields(), followUpPlan.getId(), Translator.get("update_follow_up_plan"));
        }, () -> {
            throw new GenericException("plan_not_found");
        });
        return followUpPlan;
    }

    private void updateModuleField(FollowUpPlan followUpPlan, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        followUpPlanFieldService.deleteByResourceId(followUpPlan.getId());
        // 再保存
        followUpPlanFieldService.saveModuleField(followUpPlan, orgId, userId, moduleFields, true);
    }

    private FollowUpPlan newPlan(FollowUpPlan plan, FollowUpPlanUpdateRequest request, String userId) {
        plan.setCustomerId(request.getCustomerId());
        plan.setOpportunityId(request.getOpportunityId());
        plan.setType(request.getType());
        plan.setClueId(request.getClueId());
        plan.setOwner(request.getOwner());
        plan.setContactId(request.getContactId());
        plan.setContent(request.getContent());
        plan.setMethod(request.getMethod());
        plan.setEstimatedTime(request.getEstimatedTime());
        plan.setUpdateTime(System.currentTimeMillis());
        plan.setUpdateUser(userId);
        plan.setConverted(request.getConverted());
        return plan;
    }


    /**
     * 跟进计划列表查询
     *
     * @param request
     * @param userId
     * @param orgId
     * @param resourceType
     * @param type
     * @param customerData
     *
     * @return
     */
    public PagerWithOption<List<FollowUpPlanListResponse>> list(FollowUpPlanPageRequest request, String userId, String orgId, String resourceType, String type, CustomerDataDTO customerData) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpPlanListResponse> list = extFollowUpPlanMapper.selectList(request, userId, orgId, resourceType, type, customerData, null);
        List<FollowUpPlanListResponse> buildList = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }

    /**
     * 跟进计划汇总查询
     * @param request 请求参数
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param clueDataPermission 线索数据权限
     * @param customerDataPermission 客户数据权限
     * @return 跟进计划汇总列表
     */
    public PagerWithOption<List<FollowUpPlanListResponse>> totalList(PlanHomePageRequest request, String userId, String orgId,
                                                                     DeptDataPermissionDTO clueDataPermission, DeptDataPermissionDTO customerDataPermission) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpPlanListResponse> list = extFollowUpPlanMapper.selectTotalList(request, userId, orgId, clueDataPermission, customerDataPermission);
        List<FollowUpPlanListResponse> buildList = buildListData(list, orgId);
        Map<String, List<OptionDTO>> optionMap = buildOptionMap(orgId, list, buildList);
        return PageUtils.setPageInfoWithOption(page, buildList, optionMap);
    }


    public Map<String, List<OptionDTO>> buildOptionMap(String orgId, List<FollowUpPlanListResponse> list, List<FollowUpPlanListResponse> buildList) {
        // 处理自定义字段选项数据
        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_PLAN.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(list, FollowUpPlanListResponse::getModuleFields);
        // 获取选项值对应的 option
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getOwner, FollowUpPlanListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getContactId, FollowUpPlanListResponse::getContactName);
        optionMap.put(BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey(), contactFieldOption);
        return optionMap;
    }

    public List<FollowUpPlanListResponse> buildListData(List<FollowUpPlanListResponse> list, String orgId) {
        List<String> ids = list.stream().map(FollowUpPlanListResponse::getId).toList();
        Map<String, List<BaseModuleFieldValue>> resourceFieldMap = followUpPlanFieldService.getResourceFieldMap(ids, true);

        List<String> createUserIds = list.stream().map(FollowUpPlanListResponse::getCreateUser).toList();
        List<String> updateUserIds = list.stream().map(FollowUpPlanListResponse::getUpdateUser).toList();
        List<String> ownerIds = list.stream().map(FollowUpPlanListResponse::getOwner).toList();
        List<String> userIds = Stream.of(createUserIds, updateUserIds, ownerIds)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<String> contactIds = list.stream().map(FollowUpPlanListResponse::getContactId).toList();
        Map<String, String> contactMap = baseService.getContactMap(contactIds);
        Map<String, String> contactPhoneMap = baseService.getContactPhone(contactIds);

        Map<String, UserResponse> userDeptMap = baseService.getUserDepAndPhoneByUserIds(ownerIds, orgId);

        List<String> customerIds = list.stream().map(FollowUpPlanListResponse::getCustomerId).toList();
        Map<String, String> customerMap = baseService.getCustomerMap(customerIds);
        Map<String, String> customerPoolIdMap = baseService.getCustomerPoolId(customerIds);

        List<String> opportunityIds = list.stream().map(FollowUpPlanListResponse::getOpportunityId).toList();
        Map<String, String> opportunityMap = baseService.getOpportunityMap(opportunityIds);

        List<String> clueIds = list.stream().map(FollowUpPlanListResponse::getClueId).toList();
        Map<String, String> clueMap = baseService.getClueMap(clueIds);

        list.forEach(planResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> planCustomerFields = resourceFieldMap.get(planResponse.getId());
            planResponse.setModuleFields(planCustomerFields);
            planResponse.setCreateUserName(userNameMap.get(planResponse.getCreateUser()));
            planResponse.setUpdateUserName(userNameMap.get(planResponse.getUpdateUser()));
            planResponse.setOwnerName(userNameMap.get(planResponse.getOwner()));
            planResponse.setContactName(contactMap.get(planResponse.getContactId()));
            planResponse.setCustomerName(customerMap.get(planResponse.getCustomerId()));
            planResponse.setOpportunityName(opportunityMap.get(planResponse.getOpportunityId()));
            planResponse.setClueName(clueMap.get(planResponse.getClueId()));
            planResponse.setPhone(contactPhoneMap.get(planResponse.getContactId()));
            planResponse.setPoolId(customerPoolIdMap.get(planResponse.getCustomerId()));
            planResponse.setResourceType(planResponse.getType());

            UserResponse userResponse = userDeptMap.get(planResponse.getOwner());
            if (userResponse != null) {
                planResponse.setDepartmentId(userResponse.getDepartmentId());
                planResponse.setDepartmentName(userResponse.getDepartmentName());
            }
        });
        return list;
    }


    /**
     * 跟进计划详情
     *
     * @param id
     *
     * @return
     */
    public FollowUpPlanDetailResponse get(String id, String orgId) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(id);
        FollowUpPlanDetailResponse response = BeanUtils.copyBean(new FollowUpPlanDetailResponse(), followUpPlan);
        List<FollowUpPlanListResponse> buildList = buildListData(List.of(response), orgId);

        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_PLAN.getKey(), orgId);
        // 获取所有模块字段的值
        List<BaseModuleFieldValue> moduleFieldValues = moduleFormService.getBaseModuleFieldValues(List.of(response), FollowUpPlanDetailResponse::getModuleFields);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, moduleFieldValues);

        // 补充负责人选项
        List<OptionDTO> ownerFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getOwner, FollowUpPlanListResponse::getOwnerName);
        optionMap.put(BusinessModuleField.FOLLOW_PLAN_OWNER.getBusinessKey(), ownerFieldOption);

        // 联系人
        List<OptionDTO> contactFieldOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getContactId, FollowUpPlanListResponse::getContactName);
        optionMap.put(BusinessModuleField.FOLLOW_PLAN_CONTACT.getBusinessKey(), contactFieldOption);

        //客户
        List<OptionDTO> customerOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getCustomerId, FollowUpPlanListResponse::getCustomerName);
        optionMap.put(BusinessModuleField.FOLLOW_PLAN_CUSTOMER.getBusinessKey(), customerOption);

        //商机
        List<OptionDTO> opportunityOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getOpportunityId, FollowUpPlanListResponse::getOpportunityName);
        optionMap.put(BusinessModuleField.FOLLOW_PLAN_OPPORTUNITY.getBusinessKey(), opportunityOption);

        //线索
        List<OptionDTO> clueOption = moduleFormService.getBusinessFieldOption(buildList,
                FollowUpPlanListResponse::getClueId, FollowUpPlanListResponse::getClueName);
        optionMap.put(BusinessModuleField.FOLLOW_PLAN_CLUE.getBusinessKey(), clueOption);


        response.setOptionMap(optionMap);

        // 附件信息
        response.setAttachmentMap(moduleFormService.getAttachmentMap(customerFormConfig, moduleFieldValues));
        return response;
    }


    /**
     * 取消跟进计划
     *
     * @param id
     */
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.CANCEL, resourceId = "{#id}")
    public void cancelPlan(String id, String operator) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(id);
        if (followUpPlan == null) {
            throw new GenericException("plan_not_found");
        }
        //检查跟进计划是否可以取消,如果是已完成，并且已转记录的跟进计划，则不允许取消
        if (followUpPlan.getStatus().equals(FollowUpPlanStatusType.COMPLETED.name()) && followUpPlan.getConverted()) {
            return;
        }
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(followUpPlan.getId());
        plan.setStatus(FollowUpPlanStatusType.CANCELLED.name());
        plan.setUpdateUser(operator);
        plan.setUpdateTime(System.currentTimeMillis());
        followUpPlanMapper.updateById(plan);
        OperationLogContext.setResourceName(Translator.get("cancel_follow_up_plan"));
    }


    /**
     * 删除跟进计划
     *
     * @param id
     */
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(id);
        if (followUpPlan == null) {
            throw new GenericException("plan_not_found");
        }
        deleteByIds(List.of(id));

        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("delete_follow_up_plan"));
    }


    /**
     * 更新状态
     *
     * @param request
     * @param userId
     */
    public void updateStatus(FollowUpPlanStatusRequest request, String userId) {
        //检查跟进计划是否可以更新状态,如果是已完成，并且已转记录的跟进计划，则不允许更新状态
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(request.getId());
        if (followUpPlan == null) {
            throw new GenericException("plan_not_found");
        }
        if (followUpPlan.getStatus().equals(FollowUpPlanStatusType.COMPLETED.name()) && followUpPlan.getConverted()) {
            return;
        }
        followUpPlan = new FollowUpPlan();
        followUpPlan.setStatus(request.getStatus());
        followUpPlan.setId(request.getId());
        followUpPlan.setUpdateUser(userId);
        followUpPlan.setUpdateTime(System.currentTimeMillis());
        followUpPlanMapper.update(followUpPlan);
    }

    public void deleteByCustomerIds(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return;
        }
        List<String> ids = getByCustomerIds(customerIds)
                .stream()
                .map(FollowUpPlan::getId)
                .toList();
        deleteByIds(ids);
    }

    private void deleteByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        followUpPlanFieldService.deleteByResourceIds(ids);
        followUpPlanMapper.deleteByIds(ids);
    }

    private List<FollowUpPlan> getByCustomerIds(List<String> customerIds) {
        LambdaQueryWrapper<FollowUpPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FollowUpPlan::getCustomerId, customerIds);
        return followUpPlanMapper.selectListByLambda(queryWrapper);
    }

    private List<FollowUpPlan> getByClueIds(List<String> clueIds) {
        LambdaQueryWrapper<FollowUpPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(FollowUpPlan::getClueId, clueIds);
        queryWrapper.eq(FollowUpPlan::getType, FollowUpPlanType.CLUE.name());
        return followUpPlanMapper.selectListByLambda(queryWrapper);
    }

    public void deleteByClueIds(List<String> clueIds) {
        if (CollectionUtils.isEmpty(clueIds)) {
            return;
        }
        List<String> ids = getByClueIds(clueIds)
                .stream()
                .map(FollowUpPlan::getId)
                .toList();
        deleteByIds(ids);
    }

    /**
     * 获取跟进计划对应数据权限配置
     * @param currentUser 当前用户ID
     * @param organizationId 组织ID
     * @return 数据权限配置
     */
    public ResourceTabEnableDTO getTabEnableConfig(String currentUser, String organizationId) {
        List<RolePermissionDTO> rolePermissions = permissionCache.getRolePermissions(currentUser, organizationId);
        // 由于统一的记录/计划页面会展示多个业务模块的数据，因此需要合并多个数据权限, 展示范围更大的数据权限配置
        ResourceTabEnableDTO clueTabConfig = PermissionUtils.getTabEnableConfig(currentUser, PermissionConstants.CLUE_MANAGEMENT_READ, rolePermissions);
        ResourceTabEnableDTO customerTabConfig = PermissionUtils.getTabEnableConfig(currentUser, PermissionConstants.CUSTOMER_MANAGEMENT_READ, rolePermissions);
        return clueTabConfig.or(customerTabConfig);
    }

    /**
     * 拦截跟进记录的操作权限
     * @param id 记录ID
     * @param orgId 组织ID
     */
    public void checkPlanPermission(String id, String orgId) {
        FollowUpPlanDetailResponse planDetail = get(id, orgId);
        boolean hasPermission;
        if (Strings.CS.equals(planDetail.getType(), ModuleKey.CLUE.name())) {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.CLUE_MANAGEMENT_UPDATE);
        } else if (StringUtils.isNotEmpty(planDetail.getOpportunityId())) {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE);
        } else {
            hasPermission = PermissionUtils.hasPermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
        }
        if (!hasPermission) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }
}
