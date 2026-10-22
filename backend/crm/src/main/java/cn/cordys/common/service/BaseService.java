package cn.cordys.common.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.crm.approval.service.ApprovalResourceQueryService;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 保留通用服务调用入口，具体职责委托给用户查询、资源日志及审批查询服务。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseService {
    @Resource
    private UserQueryService userQueryService;
    @Resource
    private ResourceLogService resourceLogService;
    @Resource
    private ApprovalResourceQueryService approvalResourceQueryService;
    @Resource
    private ExtCustomerContactMapper extCustomerContactMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtClueMapper extClueMapper;

    public <T> T setCreateAndUpdateUserName(T object) {
        return userQueryService.setCreateAndUpdateUserName(object);
    }

    public <T> List<T> setCreateAndUpdateUserName(List<T> list) {
        return userQueryService.setCreateAndUpdateUserName(list);
    }

    public <T> T setCreateUpdateOwnerUserName(T object) {
        return userQueryService.setCreateUpdateOwnerUserName(object);
    }

    public <T> T setCreateAndUpdateOwnerUserName(T object) {
        return userQueryService.setCreateAndUpdateOwnerUserName(object);
    }

    public <T> List<T> setCreateUpdateOwnerUserName(List<T> list) {
        return userQueryService.setCreateUpdateOwnerUserName(list);
    }

    public Map<String, String> getUserNameMap(List<String> userIds) {
        return userQueryService.getUserNameMap(userIds);
    }

    public String getUserName(String userId) {
        return userQueryService.getUserName(userId);
    }

    public Map<String, String> getUserNameMap(Set<String> userIds) {
        return userQueryService.getUserNameMap(userIds);
    }

    public Map<String, UserDeptDTO> getUserDeptMapByUserIds(Set<String> ownerIds, String orgId) {
        return userQueryService.getUserDeptMapByUserIds(ownerIds, orgId);
    }

    public UserDeptDTO getUserDeptMapByUserId(String ownerId, String orgId) {
        return userQueryService.getUserDeptMapByUserId(ownerId, orgId);
    }

    public String getDeptIdByUserId(String userId, String orgId) {
        return userQueryService.getDeptIdByUserId(userId, orgId);
    }

    public Map<String, UserDeptDTO> getUserDeptMapByUserIds(List<String> ownerIds, String orgId) {
        return userQueryService.getUserDeptMapByUserIds(ownerIds, orgId);
    }

    /**
     * 获取联系人ID和名称的映射
     */
    public Map<String, String> getContactMap(List<String> contactIds) {
        if (CollectionUtils.isEmpty(contactIds)) {
            return Collections.emptyMap();
        }
        return extCustomerContactMapper.selectContactOptionByIds(contactIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    public Map<String, UserResponse> getUserDepAndPhoneByUserIds(List<String> ownerIds, String orgId) {
        return userQueryService.getUserDepAndPhoneByUserIds(ownerIds, orgId);
    }

    public <T> void handleAddLog(T resource, List<BaseModuleFieldValue> moduleFields) {
        resourceLogService.handleAddLog(resource, moduleFields);
    }

    public <T> void handleAddLogWithResourceName(T resource, List<BaseModuleFieldValue> moduleFields) {
        resourceLogService.handleAddLogWithResourceName(resource, moduleFields);
    }

    public <T> void handleAddLogWithSubTable(
            T resource,
            List<BaseModuleFieldValue> moduleFields,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        resourceLogService.handleAddLogWithSubTable(resource, moduleFields, subTableKeyName, moduleFormConfigDTO);
    }

    public <T> void handleUpdateLog(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name) {
        resourceLogService.handleUpdateLog(originResource, modifiedResource, originResourceFields,
                modifiedResourceFields, id, name);
    }

    public <T> void handleUpdateLogWithSubTable(
            T originResource,
            T modifiedResource,
            List<BaseModuleFieldValue> originResourceFields,
            List<BaseModuleFieldValue> modifiedResourceFields,
            String id,
            String name,
            String subTableKeyName,
            ModuleFormConfigDTO moduleFormConfigDTO) {
        resourceLogService.handleUpdateLogWithSubTable(originResource, modifiedResource, originResourceFields,
                modifiedResourceFields, id, name, subTableKeyName, moduleFormConfigDTO);
    }

    public Map<String, String> getSubTableIdKeyMap(ModuleFormConfigDTO moduleFormConfigDTO) {
        return resourceLogService.getSubTableIdKeyMap(moduleFormConfigDTO);
    }

    /**
     * 客户id与名称映射
     */
    public Map<String, String> getCustomerMap(List<String> customerIds) {
        if (CollectionUtils.isEmpty(customerIds)) {
            return Collections.emptyMap();
        }
        return extCustomerMapper.getCustomerOptionsByIds(customerIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    /**
     * 商机id与名称映射
     */
    public Map<String, String> getOpportunityMap(List<String> opportunityIds) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            return Collections.emptyMap();
        }
        return extOpportunityMapper.getOpportunityOptionsByIds(opportunityIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    /**
     * 线索id与名称映射
     */
    public Map<String, String> getClueMap(List<String> clueIds) {
        if (CollectionUtils.isEmpty(clueIds)) {
            return Collections.emptyMap();
        }
        return extClueMapper.selectOptionByIds(clueIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    /**
     * 联系人id和电话映射
     */
    public Map<String, String> getContactPhone(List<String> contactIds) {
        if (CollectionUtils.isEmpty(contactIds)) {
            return Collections.emptyMap();
        }
        return extCustomerContactMapper.selectContactPhoneOptionByIds(contactIds)
                .stream()
                .collect(Collectors.toMap(OptionDTO::getIdAsString, OptionDTO::getName));
    }

    public String getAndCheckOptionName(String option) {
        return userQueryService.getAndCheckOptionName(option);
    }

    public Map<String, Boolean> getApprovingResourceFirstNodeApproved(List<String> resourceIds, String currentOrgId) {
        return approvalResourceQueryService.getApprovingResourceFirstNodeApproved(resourceIds, currentOrgId);
    }

    public Map<String, String> getApprovingResourceSubmitterIds(List<String> resourceIds) {
        return approvalResourceQueryService.getApprovingResourceSubmitterIds(resourceIds);
    }

    public String getApprovingResourceSubmitterId(String resourceId) {
        return approvalResourceQueryService.getApprovingResourceSubmitterId(resourceId);
    }
}
