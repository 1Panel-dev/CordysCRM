package io.cordys.crm.follow.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.follow.domain.FollowUpPlan;
import io.cordys.crm.follow.dto.CustomerDataDTO;
import io.cordys.crm.follow.dto.request.FollowUpPlanAddRequest;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.request.FollowUpRecordUpdateRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanDetailResponse;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.service.ModuleFormCacheService;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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

    /**
     * 新建跟进计划
     *
     * @param request
     * @param userId
     * @param orgId
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
        if (StringUtils.isBlank(request.getOwner())) {
            followUpPlan.setOwner(userId);
        }
        followUpPlanMapper.insert(followUpPlan);
        //保存自定义字段
        followUpPlanFieldService.saveModuleField(followUpPlan.getId(), orgId, userId, request.getModuleFields());
        return followUpPlan;
    }


    /**
     * 更新跟进计划
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.FOLLOW_UP_PLAN, type = LogType.UPDATE, resourceId = "{#request.id}")
    public FollowUpPlan update(FollowUpRecordUpdateRequest request, String userId, String orgId) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(request.getId());
        Optional.ofNullable(followUpPlan).ifPresentOrElse(plan -> {
            //更新跟进计划
            updatePlan(plan, request, userId);
            // 获取模块字段
            List<BaseModuleFieldValue> originCustomerFields = followUpPlanFieldService.getModuleFieldValuesByResourceId(request.getId());
            //更新模块字段
            updateModuleField(request.getId(), request.getModuleFields(), orgId, userId);
            baseService.handleUpdateLog(followUpPlan, plan, originCustomerFields, request.getModuleFields(), followUpPlan.getId(), Translator.get("update_follow_up_plan"));
        }, () -> {
            throw new GenericException("plan_not_found");
        });
        return followUpPlan;
    }

    private void updateModuleField(String id, List<BaseModuleFieldValue> moduleFields, String orgId, String userId) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        followUpPlanFieldService.deleteByResourceId(id);
        // 再保存
        followUpPlanFieldService.saveModuleField(id, orgId, userId, moduleFields);
    }

    private void updatePlan(FollowUpPlan plan, FollowUpRecordUpdateRequest request, String userId) {
        plan.setCustomerId(request.getCustomerId());
        plan.setOpportunityId(request.getOpportunityId());
        plan.setType(request.getType());
        plan.setClueId(request.getClueId());
        plan.setOwner(request.getOwner());
        plan.setContactId(request.getContactId());
        plan.setContent(request.getContent());
        plan.setEstimatedTime(request.getEstimatedTime());
        plan.setUpdateTime(System.currentTimeMillis());
        plan.setUpdateUser(userId);
        followUpPlanMapper.update(plan);
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
     * @return
     */
    public PagerWithOption<List<FollowUpPlanListResponse>> list(FollowUpPlanPageRequest request, String userId, String orgId, String resourceType, String type, CustomerDataDTO customerData) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<FollowUpPlanListResponse> list = extFollowUpPlanMapper.selectList(request, userId, orgId, resourceType, type, customerData, null);
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
        Map<String, List<BaseModuleFieldValue>> resourceFieldMap = followUpPlanFieldService.getResourceFieldMap(ids);

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

        Map<String, UserResponse> userDeptMap = baseService.getUserDepAndPhoneByUserIds(ownerIds, orgId);

        List<String> customerIds = list.stream().map(FollowUpPlanListResponse::getCustomerId).toList();
        Map<String, String> customerMap = baseService.getCustomerMap(customerIds);

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

            UserResponse userResponse = userDeptMap.get(planResponse.getOwner());
            if (userResponse != null) {
                planResponse.setDepartmentId(userResponse.getDepartmentId());
                planResponse.setDepartmentName(userResponse.getDepartmentName());
                planResponse.setPhone(userResponse.getPhone());
            }
        });
        return list;
    }


    /**
     * 跟进计划详情
     *
     * @param id
     * @return
     */
    public FollowUpPlanDetailResponse get(String id, String orgId) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(id);
        FollowUpPlanDetailResponse response = BeanUtils.copyBean(new FollowUpPlanDetailResponse(), followUpPlan);
        List<BaseModuleFieldValue> fieldValueList = followUpPlanFieldService.getModuleFieldValuesByResourceId(id);
        response.setModuleFields(fieldValueList);
        List<FollowUpPlanListResponse> buildList = buildListData(List.of(response), orgId);

        ModuleFormConfigDTO customerFormConfig = moduleFormCacheService.getBusinessFormConfig(FormKey.FOLLOW_PLAN.getKey(), orgId);
        Map<String, List<OptionDTO>> optionMap = moduleFormService.getOptionMap(customerFormConfig, fieldValueList);

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

        return response;
    }


    /**
     * 取消跟进计划
     *
     * @param id
     */
    public void cancelPlan(String id) {
        FollowUpPlan followUpPlan = followUpPlanMapper.selectByPrimaryKey(id);
        if (followUpPlan == null) {
            throw new GenericException("plan_not_found");
        }
        FollowUpPlan plan = new FollowUpPlan();
        plan.setId(followUpPlan.getId());
        plan.setStatus("CANCELLED");
        followUpPlanMapper.updateById(plan);
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
        followUpPlanMapper.deleteByPrimaryKey(id);

        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("delete_follow_up_plan"));
    }
}
