package io.cordys.crm.follow.service;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.service.DataScopeService;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.customer.constants.CustomerCollaborationType;
import io.cordys.crm.customer.domain.CustomerCollaboration;
import io.cordys.crm.customer.service.CustomerCollaborationService;
import io.cordys.crm.follow.dto.CustomerDataDTO;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    public CustomerDataDTO getCustomerPermission(String userId, String sourceId) {
        CustomerDataDTO customerDataDTO = new CustomerDataDTO();
        DeptDataPermissionDTO deptDataPermission = dataScopeService.getDeptDataPermission(SessionUtils.getUserId(), OrganizationContext.getOrganizationId());

        //全部数据
        if (deptDataPermission.getAll() || StringUtils.equalsAnyIgnoreCase(userId, InternalUser.ADMIN.getValue())) {
            customerDataDTO.setAll(true);
            return customerDataDTO;
        }


        // 查询协作人信息
        List<CustomerCollaboration> collaborations = customerCollaborationService.selectByCustomerId(sourceId);

        List<CustomerCollaboration> userList = collaborations.stream()
                .filter(collaboration -> StringUtils.equals(collaboration.getUserId(), userId))
                .toList();

        if (CollectionUtils.isNotEmpty(userList)) {
            CustomerCollaboration first = userList.getFirst();
            if (StringUtils.equals(first.getCollaborationType(), CustomerCollaborationType.READ_ONLY.name())) {
                customerDataDTO.setOwner(true);
            }
        } else {
            //不是协作人
            customerDataDTO.setOwner(true);
        }


        // 获取协作类型的协作的联系人
        Set<String> collaborationUserIds = collaborations.stream()
                .filter(collaboration -> StringUtils.equals(collaboration.getCollaborationType(), CustomerCollaborationType.COLLABORATION.name()))
                .map(CustomerCollaboration::getUserId)
                .collect(Collectors.toSet());


        // 部门数据权限
        if (CollectionUtils.isNotEmpty(deptDataPermission.getDeptIds())) {
            List<OrganizationUser> users = extOrganizationUserMapper.selectUserByUserIds(new ArrayList<>(collaborationUserIds));
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
}
