package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.DataScopeService;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerRelation;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 关联记录ID不能作为客户ID使用，必须从数据库解析所属客户再校验。
 */
@Service
public class CustomerAssociationPermissionService {
    @Resource
    private BaseMapper<CustomerCollaboration> collaborationMapper;
    @Resource
    private BaseMapper<CustomerRelation> relationMapper;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private DataScopeService dataScopeService;

    public void checkCollaborations(List<String> ids) {
        for (String id : ids) {
            CustomerCollaboration collaboration = collaborationMapper.selectByPrimaryKey(id);
            if (collaboration == null || StringUtils.isBlank(collaboration.getCustomerId())) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            resourcePermissionService.checkResourcePermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE,
                    collaboration.getCustomerId(), FormKeyConstants.CUSTOMER,
                    SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
        }
    }

    public void checkRelation(String id) {
        resourcePermissionService.checkPermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
        CustomerRelation relation = relationMapper.selectByPrimaryKey(id);
        if (relation == null) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        String orgId = OrganizationContext.getOrganizationId();
        Customer source = customerMapper.selectByPrimaryKey(relation.getSourceCustomerId());
        Customer target = customerMapper.selectByPrimaryKey(relation.getTargetCustomerId());
        if (StringUtils.isBlank(orgId) || source == null || target == null
                || !orgId.equals(source.getOrganizationId()) || !orgId.equals(target.getOrganizationId())) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        // 关系可从任一端客户维护，但两端必须属于当前组织。
        if (!canUpdate(source, orgId) && !canUpdate(target, orgId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    private boolean canUpdate(Customer customer, String orgId) {
        return StringUtils.isNotBlank(customer.getOwner()) && dataScopeService.hasDataPermission(
                SessionUtils.getUserId(), orgId, customer.getOwner(), PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
    }
}
