package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.PermissionUtils;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.dto.CustomerDataDTO;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class BaseFollowUpService {

    @Resource
    private CustomerCollaborationService customerCollaborationService;
    @Resource
    private DataScopeService dataScopeService;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private PoolClueService poolClueService;

    public CustomerDataDTO getCustomerPermission(String userId, String sourceId, String permission) {
        CustomerDataDTO customerDataDTO = new CustomerDataDTO();
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), permission);

        // 全部数据
        if (deptDataPermission.getAll() || Strings.CI.equalsAny(userId, InternalUser.ADMIN.getValue())) {
            customerDataDTO.setAll(true);
            return customerDataDTO;
        }


        // 查询协作人信息
        List<CustomerCollaboration> collaborations = customerCollaborationService.selectByCustomerId(sourceId);

        List<CustomerCollaboration> userList = collaborations.stream()
                .filter(collaboration -> Strings.CS.equals(collaboration.getUserId(), userId))
                .toList();

        if (CollectionUtils.isNotEmpty(userList)) {
            CustomerCollaboration first = userList.getFirst();
            if (Strings.CS.equals(first.getCollaborationType(), CustomerCollaborationType.READ_ONLY.name())) {
                customerDataDTO.setOwner(true);
            }
        } else {
            // 不是协作人
            customerDataDTO.setOwner(true);
        }


        // 获取协作类型的协作的联系人
        Set<String> collaborationUserIds = collaborations.stream()
                .filter(collaboration -> Strings.CS.equals(collaboration.getCollaborationType(), CustomerCollaborationType.COLLABORATION.name()))
                .map(CustomerCollaboration::getUserId)
                .collect(Collectors.toSet());


        // 部门数据权限
        if (CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())) {
            List<OrganizationUser> users = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(collaborationUserIds)) {
                users = extOrganizationUserMapper.selectUserByUserIds(new ArrayList<>(collaborationUserIds));
            }
            List<OrganizationUser> currentUser = extOrganizationUserMapper.selectUserByUserIds(List.of(userId));
            if (customerDataDTO.isOwner()) {
                List<OrganizationUser> depUsers = users.stream()
                        .filter(user -> !deptDataPermission.getDeptIds().contains(user.getDepartmentId()))
                        .toList();
                if (CollectionUtils.isNotEmpty(depUsers)) {
                    customerDataDTO.setUserIds(depUsers.stream().map(OrganizationUser::getUserId).toList());
                }
            } else {
                List<OrganizationUser> depUsers = users.stream()
                        .filter(user -> !deptDataPermission.getDeptIds().contains(user.getDepartmentId()))
                        .toList();
                List<String> ids = depUsers.stream().map(OrganizationUser::getUserId).toList();
                List<String> userIds = new ArrayList<>();
                if (!deptDataPermission.getDeptIds().contains(currentUser.getFirst().getDepartmentId())) {
                    userIds.add(currentUser.getFirst().getUserId());
                }
                userIds.addAll(ids);
                customerDataDTO.setUserIds(userIds);
            }

        }

        if (deptDataPermission.getSelf()) {
            if (customerDataDTO.isOwner()) {
                customerDataDTO.setUserIds(new ArrayList<>(collaborationUserIds));
            } else {
                customerDataDTO.setSelf(true);
            }
        }

        return customerDataDTO;
    }

    public CustomerDataDTO getOpportunityPermission(String userId, String permission) {
        CustomerDataDTO customerDataDTO = new CustomerDataDTO();
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(),
                OrganizationContext.getOrganizationId(), permission);

        // 全部数据
        if (deptDataPermission.getAll() || Strings.CI.equalsAny(userId, InternalUser.ADMIN.getValue())) {
            customerDataDTO.setAll(true);
            return customerDataDTO;
        }

        if (deptDataPermission.getSelf()) {
            customerDataDTO.setSelf(true);
        }
        return customerDataDTO;
    }


    public void checkPermission(String orgId, String userId, String permission, String owner) {
        if (!PermissionUtils.hasPermission(permission) || !dataScopeService.hasDataPermission(userId, orgId, owner, permission)) {
            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }

    public boolean hasPermission(String orgId, String userId, String permission, String owner) {
        if (PermissionUtils.hasPermission(permission) && dataScopeService.hasDataPermission(userId, orgId, owner, permission)) {
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
                    if (Strings.CS.equals(customerCollaboration.getCollaborationType(), CustomerCollaborationType.COLLABORATION.name())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 拦截跟进记录的操作权限（角色权限位 + 所属客户/商机/线索的数据权限）
     *
     * @param record    记录
     * @param orgId 组织ID
     */
    public void checkRecordPermission(FollowUpRecord record, String orgId, String userId, boolean isRead) {
        if (record == null) {
            throw new GenericException("plan_not_found");
        }
        String permission;
        if (Strings.CS.equals(record.getType(), ModuleKey.CLUE.name())) {
            permission = isRead ? PermissionConstants.CLUE_MANAGEMENT_READ : PermissionConstants.CLUE_MANAGEMENT_UPDATE;
            Clue clue = clueMapper.selectByPrimaryKey(record.getClueId());
            if (clue != null) {
                if (BooleanUtils.isTrue(clue.getInSharedPool())) {
                    // 校验线索池
                    poolClueService.checkPoolMember(clue.getPoolId(), userId, orgId);
                }
                checkPermission(orgId, userId, permission, clue.getOwner());
            } else {
                throw new GenericException(Translator.get("no.operation.permission"));
            }
        } else {
            permission = isRead ? PermissionConstants.CUSTOMER_MANAGEMENT_READ : PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE;
            Customer customer = customerMapper.selectByPrimaryKey(record.getCustomerId());
            if (customer != null) {
                if (BooleanUtils.isTrue(customer.getInSharedPool())) {
                    // 校验公海
                    poolCustomerService.checkPoolMember(customer.getPoolId(), userId, orgId);
                    return;
                }
                if (hasPermission(orgId, userId, permission, customer.getOwner())
                        || hasCustomerCollaborationPermission(record.getCustomerId(), userId, isRead)) {
                    // 有客户权限，或者是客户协作人，直接返回
                    return;
                }
            }

            if (StringUtils.isNotEmpty(record.getOpportunityId())) {
                // 如果有商机，则判断商机权限
                permission = isRead ? PermissionConstants.OPPORTUNITY_MANAGEMENT_READ : PermissionConstants.OPPORTUNITY_MANAGEMENT_UPDATE;
                Opportunity opportunity = opportunityMapper.selectByPrimaryKey(record.getOpportunityId());
                if (opportunity != null) {
                    checkPermission(orgId, userId, permission, opportunity.getOwner());
                    return;
                }
            }

            throw new GenericException(Translator.get("no.operation.permission"));
        }
    }
}
