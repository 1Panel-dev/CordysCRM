package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.approval.mapper.ExtApprovalTaskMapper;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.service.PoolClueService;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.service.PoolCustomerService;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.service.CustomFormDataService;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.system.domain.Attachment;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 附件沿用所属业务数据的查看权限，审批附件额外允许历史经办人访问。
 */
@Service
public class AttachmentPermissionService {

    @Resource
    private BaseMapper<ApprovalComment> approvalCommentMapper;
    @Resource
    private ExtApprovalTaskMapper extApprovalTaskMapper;
    @Resource
    private ExtApprovalCommentMapper extApprovalCommentMapper;
    @Resource
    private List<ResourceAccessContextProvider> contextProviders;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;
    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;
    @Resource
    private BaseMapper<ProductPrice> productPriceMapper;
    @Resource
    private BaseMapper<CustomFormData> customFormDataMapper;
    @Resource
    private PoolCustomerService poolCustomerService;
    @Resource
    private PoolClueService poolClueService;
    @Resource
    private FollowUpRecordService followUpRecordService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private CustomFormDataService customFormDataService;

    public void checkPermission(Attachment attachment) {
        checkPermission(attachment, false);
    }

    /**
     * 预览、下载评论附件时，允许同一业务资源的有效被@用户读取。
     */
    public void checkReadPermission(Attachment attachment) {
        checkPermission(attachment, true);
    }

    private void checkPermission(Attachment attachment, boolean readOnly) {
        String userId = SessionUtils.getUserId();
        String orgId = OrganizationContext.getOrganizationId();
        if (attachment == null || StringUtils.isAnyBlank(userId, orgId, attachment.getResourceId())
                || !orgId.equals(attachment.getOrganizationId())) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        String resourceId = attachment.getResourceId();
        ApprovalComment comment = approvalCommentMapper.selectByPrimaryKey(resourceId);
        if (comment != null) {
            if (!orgId.equals(comment.getOrganizationId()) || StringUtils.isBlank(comment.getResourceId())) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            resourceId = comment.getResourceId();
            if (extApprovalTaskMapper.existsResourceParticipant(resourceId, userId, orgId)
                    || (readOnly && extApprovalCommentMapper.existsMention(resourceId, userId, orgId))) {
                return;
            }
        } else if (extApprovalTaskMapper.existsAttachmentParticipant(attachment.getId(), resourceId, userId, orgId)) {
            return;
        }
        checkBusinessPermission(resourceId, userId, orgId);
    }

    private void checkBusinessPermission(String resourceId, String userId, String orgId) {
        for (ResourceAccessContextProvider provider : contextProviders) {
            if (provider.getAccessContext(resourceId, orgId) == null) {
                continue;
            }
            FormKey formKey = FormKey.ofKey(provider.getFormType());
            if (formKey == null) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            switch (formKey) {
            case CUSTOMER -> checkCustomer(resourceId, userId, orgId);
            case CLUE -> checkClue(resourceId, userId, orgId);
            case FOLLOW_RECORD -> {
                FollowUpRecord record = followUpRecordMapper.selectByPrimaryKey(resourceId);
                if (record == null || !orgId.equals(record.getOrganizationId())) {
                    throw new GenericException(CrmHttpResultCode.FORBIDDEN);
                }
                followUpRecordService.checkRecordPermission(resourceId, orgId, userId, true);
            }
            case FOLLOW_PLAN -> {
                FollowUpPlan plan = followUpPlanMapper.selectByPrimaryKey(resourceId);
                if (plan == null || !orgId.equals(plan.getOrganizationId())) {
                    throw new GenericException(CrmHttpResultCode.FORBIDDEN);
                }
                followUpPlanService.checkPlanPermission(resourceId, orgId, userId, true);
            }
            default -> resourcePermissionService.checkResourcePermissionWithoutApprovalTask(
                    getReadPermission(formKey), resourceId, provider.getFormType(), userId, orgId);
            }
            return;
        }

        ProductPrice price = productPriceMapper.selectByPrimaryKey(resourceId);
        if (price != null) {
            if (!orgId.equals(price.getOrganizationId())) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            resourcePermissionService.checkPermission(PermissionConstants.PRICE_READ);
            return;
        }
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(resourceId);
        if (data != null && orgId.equals(data.getOrganizationId())) {
            resourcePermissionService.checkPermission(PermissionConstants.CUSTOM_FORM_READ);
            if (customFormDataService.hasViewPermission(resourceId, userId, orgId)) {
                return;
            }
        }
        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }

    private void checkCustomer(String resourceId, String userId, String orgId) {
        Customer customer = customerMapper.selectByPrimaryKey(resourceId);
        if (customer == null || !orgId.equals(customer.getOrganizationId())) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(customer.getInSharedPool())) {
            resourcePermissionService.checkPermission(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ);
            if (!poolCustomerService.hasPoolMember(customer.getPoolId(), userId, orgId)) {
                throw new GenericException(CrmHttpResultCode.FORBIDDEN);
            }
            return;
        }
        resourcePermissionService.checkResourcePermissionWithoutApprovalTask(
                PermissionConstants.CUSTOMER_MANAGEMENT_READ, resourceId, FormKey.CUSTOMER.getKey(), userId, orgId);
    }

    private void checkClue(String resourceId, String userId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(resourceId);
        if (clue == null || !orgId.equals(clue.getOrganizationId())) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(clue.getInSharedPool())) {
            resourcePermissionService.checkPermission(PermissionConstants.CLUE_MANAGEMENT_POOL_READ);
            poolClueService.checkPoolMember(clue.getPoolId(), userId, orgId);
            return;
        }
        resourcePermissionService.checkResourcePermissionWithoutApprovalTask(
                PermissionConstants.CLUE_MANAGEMENT_READ, resourceId, FormKey.CLUE.getKey(), userId, orgId);
    }

    private String getReadPermission(FormKey formKey) {
        return switch (formKey) {
        case CONTACT -> PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ;
        case OPPORTUNITY -> PermissionConstants.OPPORTUNITY_MANAGEMENT_READ;
        case PRODUCT -> PermissionConstants.PRODUCT_MANAGEMENT_READ;
        case QUOTATION -> PermissionConstants.OPPORTUNITY_QUOTATION_READ;
        case CONTRACT -> PermissionConstants.CONTRACT_READ;
        case INVOICE -> PermissionConstants.CONTRACT_INVOICE_READ;
        case CONTRACT_PAYMENT_PLAN -> PermissionConstants.CONTRACT_PAYMENT_PLAN_READ;
        case CONTRACT_PAYMENT_RECORD -> PermissionConstants.CONTRACT_PAYMENT_RECORD_READ;
        case ORDER -> PermissionConstants.ORDER_READ;
        default -> throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        };
    }
}
