package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseFollowUpService {

    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private RelatedResourcePermissionService relatedResourcePermissionService;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private PoolClueService poolClueService;

    public void checkPermission(String orgId, String userId, String permission, String owner) {
        if (!PermissionUtils.hasPermission(permission)
                || !dataScopeService.hasDataPermission(userId, orgId, owner, permission)) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }

    public boolean hasPermission(String orgId, String userId, String permission, String owner) {
        if (PermissionUtils.hasPermission(permission)
                && dataScopeService.hasDataPermission(userId, orgId, owner, permission)) {
            return true;
        }
        return false;
    }

    public boolean hasCustomerCollaborationPermission(String customerId, String userId, boolean isRead) {
        // 查询协作人信息
        List<CustomerCollaboration> collaborations = customerCollaborationService.selectByCustomerId(customerId);

        List<CustomerCollaboration> userList = collaborations.stream()
                .filter(collaboration -> Strings.CS.equals(collaboration.getUserId(), userId))
                .toList();

        if (CollectionUtils.isNotEmpty(userList)) {
            if (isRead) {
                // 如果是查看权限，只要是协作人就有权限
                return true;
            } else {
                // 如果是编辑权限，需要判断协作类型是否为协作
                for (CustomerCollaboration customerCollaboration : userList) {
                    if (Strings.CS.equalsAny(customerCollaboration.getCollaborationType(),
                            CustomerCollaborationType.COLLABORATION.name(),
                            CustomerCollaborationType.JOINT_FOLLOW.name())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 详情可由跟进自身或任一关联业务资源的数据权限读取；两者均不满足时拒绝访问。
     */
    public void checkRelatedReadPermission(FollowUpRecord record, String orgId, String userId,
            String followPermission, String followFormType) {
        if (record == null) {
            throw new GenericException(CrmHttpResultCode.NOT_FOUND);
        }
        relatedResourcePermissionService.checkIsolatedData(followFormType, record.getId(), userId, orgId);
        resourcePermissionService.checkPermission(followPermission);
        if (hasResourceReadPermission(followPermission, record.getId(), followFormType, userId, orgId)
                || hasClueReadPermission(record.getClueId(), userId, orgId)
                || hasCustomerReadPermission(record.getCustomerId(), userId, orgId)
                || hasResourceReadPermission(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
                        record.getOpportunityId(), FormKeyConstants.OPPORTUNITY, userId, orgId)) {
            return;
        }
        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }

    private boolean hasResourceReadPermission(String permission, String resourceId, String formType, String userId,
            String orgId) {
        if (StringUtils.isBlank(resourceId)) {
            return false;
        }
        try {
            resourcePermissionService.checkResourcePermission(permission, resourceId, formType, userId, orgId);
            return true;
        } catch (GenericException exception) {
            return false;
        }
    }

    private boolean hasClueReadPermission(String clueId, String userId, String orgId) {
        if (StringUtils.isBlank(clueId)) {
            return false;
        }
        Clue clue = clueMapper.selectByPrimaryKey(clueId);
        if (clue == null || !Strings.CS.equals(clue.getOrganizationId(), orgId)) {
            return false;
        }
        if (!BooleanUtils.isTrue(clue.getInSharedPool())) {
            return hasResourceReadPermission(PermissionConstants.CLUE_MANAGEMENT_READ, clueId,
                    FormKeyConstants.CLUE, userId, orgId);
        }
        try {
            poolClueService.checkPoolMember(clue.getPoolId(), userId, orgId);
            return true;
        } catch (GenericException exception) {
            return false;
        }
    }

    private boolean hasCustomerReadPermission(String customerId, String userId, String orgId) {
        if (StringUtils.isBlank(customerId)) {
            return false;
        }
        Customer customer = customerMapper.selectByPrimaryKey(customerId);
        if (customer == null || !Strings.CS.equals(customer.getOrganizationId(), orgId)) {
            return false;
        }
        if (!BooleanUtils.isTrue(customer.getInSharedPool())) {
            return hasResourceReadPermission(PermissionConstants.CUSTOMER_MANAGEMENT_READ, customerId,
                    FormKeyConstants.CUSTOMER, userId, orgId) || hasCustomerCollaborationPermission(customerId, userId, true);
        }
        return poolCustomerService.hasPoolMember(customer.getPoolId(), userId, orgId) || hasCustomerCollaborationPermission(customerId, userId, true);
    }
}
