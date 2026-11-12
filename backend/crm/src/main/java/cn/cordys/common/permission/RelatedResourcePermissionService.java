package cn.cordys.common.permission;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.CaseFormatUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.domain.CustomerRelation;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线索、商机负责人及客户负责人、协作成员通过已启用标签只读访问真实关联的数据。
 */
@Service
public class RelatedResourcePermissionService {
    private static final Map<String, String> PARENT_PERMISSIONS = Map.of(
            FormKey.CLUE.getKey(), PermissionConstants.CLUE_MANAGEMENT_READ,
            FormKey.CUSTOMER.getKey(), PermissionConstants.CUSTOMER_MANAGEMENT_READ,
            FormKey.OPPORTUNITY.getKey(), PermissionConstants.OPPORTUNITY_MANAGEMENT_READ);

    @Resource
    private ExtStatisticMapper extStatisticMapper;
    @Lazy
    @Resource
    private ModuleFormCacheService moduleFormCacheService;
    @Resource
    private PermissionCache permissionCache;
    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;
    @Resource
    private BaseMapper<CustomerRelation> customerRelationMapper;
    @Lazy
    @Resource
    private CustomerCollaborationService customerCollaborationService;

    public static String isolatedCollaborationType() {
        return CustomerCollaborationType.COLLABORATION.name();
    }

    public Set<String> getIsolatedOwnerIds(String customerId, String userId, String orgId) {
        DeptDataPermissionDTO scope = getCustomerTabDataPermission(customerId, userId, orgId);
        return scope == null ? Set.of() : scope.getExcludedOwnerIds();
    }

    /** 客户详情标签优先按协作关系确定数据范围，无有效关系时由调用方保留普通角色范围。 */
    public DeptDataPermissionDTO getCustomerTabDataPermission(String customerId, String userId, String orgId) {
        if (StringUtils.isAnyBlank(customerId, userId, orgId)
                || !orgId.equals(value("customer", "organization_id", customerId))) {
            return null;
        }
        String inSharedPool = value("customer", "in_shared_pool", customerId);
        if ("1".equals(inSharedPool) || Boolean.parseBoolean(inSharedPool)) {
            return null;
        }
        String customerOwner = value("customer", "owner", customerId);
        List<CustomerCollaboration> members = customerCollaborationService.selectByCustomerId(customerId);
        boolean owner = userId.equals(customerOwner);
        boolean collaborator = members.stream().anyMatch(member -> userId.equals(member.getUserId()));
        if (!owner && !collaborator) {
            return null;
        }
        boolean fullRead = owner || members.stream().anyMatch(member -> userId.equals(member.getUserId())
                && (CustomerCollaborationType.READ_ONLY.name().equals(member.getCollaborationType())
                        || CustomerCollaborationType.JOINT_FOLLOW.name().equals(member.getCollaborationType())));
        DeptDataPermissionDTO scope = new DeptDataPermissionDTO();
        scope.setAll(fullRead);
        scope.setSelf(!fullRead);
        for (CustomerCollaboration member : members) {
            if (isolatedCollaborationType().equals(member.getCollaborationType())
                    && StringUtils.isNotBlank(member.getUserId()) && !userId.equals(member.getUserId())
                    && !member.getUserId().equals(customerOwner)) {
                scope.getExcludedOwnerIds().add(member.getUserId());
            }
        }
        return scope;
    }

    public boolean isIsolatedData(String formKey, String resourceId, String userId, String orgId) {
        if (StringUtils.isAnyBlank(formKey, resourceId, userId, orgId)) {
            return false;
        }
        FormKey form = FormKey.ofKey(formKey);
        if (form == FormKey.PRODUCT || form == FormKey.PRICE || form == FormKey.QUOTATION) {
            return false;
        }
        String table = tableName(formKey);
        if (!orgId.equals(value(table, "organization_id", resourceId))
                || (form == null && !formKey.equals(value(table, "custom_form_id", resourceId)))) {
            return false;
        }
        String owner = value(table, "owner", resourceId);
        if (userId.equals(owner)) {
            return false;
        }
        Set<String> customerIds = relatedCustomerIds(form, resourceId, table, orgId);
        for (FormDetailTab tab : detailTabs(FormKey.CUSTOMER.getKey(), orgId)) {
            if (tab.getRelatedForm() != null && tab.getRelatedField() != null
                    && formKey.equals(tab.getRelatedForm().getIdAsString())) {
                customerIds.addAll(fieldRelatedIds(FormKey.CUSTOMER.getKey(), tab.getRelatedField().getIdAsString(),
                        formKey, resourceId, table, orgId));
            }
        }
        return customerIds.stream().anyMatch(customerId -> {
            DeptDataPermissionDTO scope = getCustomerTabDataPermission(customerId, userId, orgId);
            return scope != null && (scope.getSelf() || scope.getExcludedOwnerIds().contains(owner));
        });
    }

    public void checkIsolatedData(String formKey, String resourceId, String userId, String orgId) {
        if (isIsolatedData(formKey, resourceId, userId, orgId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    private Set<String> relatedCustomerIds(FormKey form, String resourceId, String table, String orgId) {
        Set<String> customerIds = new HashSet<>();
        switch (form) {
        case CONTACT, OPPORTUNITY, CONTRACT, ORDER, FOLLOW_RECORD, FOLLOW_PLAN ->
            customerIds.add(value(table, "customer_id", resourceId));
        case INVOICE, CONTRACT_PAYMENT_PLAN, CONTRACT_PAYMENT_RECORD -> {
            String contractId = value(table, "contract_id", resourceId);
            if (StringUtils.isNotBlank(contractId) && orgId.equals(value("contract", "organization_id", contractId))) {
                customerIds.add(value("contract", "customer_id", contractId));
            }
        }
        case QUOTATION -> {
            String opportunityId = value(table, "opportunity_id", resourceId);
            if (StringUtils.isNotBlank(opportunityId)
                    && orgId.equals(value("opportunity", "organization_id", opportunityId))) {
                customerIds.add(value("opportunity", "customer_id", opportunityId));
            }
        }
        case null, default -> {
        }
        }
        if (form == FormKey.FOLLOW_RECORD || form == FormKey.FOLLOW_PLAN) {
            String opportunityId = value(table, "opportunity_id", resourceId);
            if (StringUtils.isNotBlank(opportunityId)
                    && orgId.equals(value("opportunity", "organization_id", opportunityId))) {
                customerIds.add(value("opportunity", "customer_id", opportunityId));
            }
        }
        return customerIds;
    }

    public boolean isOwner(String formKey, String resourceId, String userId, String orgId) {
        if (StringUtils.isAnyBlank(formKey, resourceId, userId, orgId)
                || !PARENT_PERMISSIONS.containsKey(formKey)) {
            return false;
        }
        String table = tableName(formKey);
        if (!orgId.equals(value(table, "organization_id", resourceId))
                || !userId.equals(value(table, "owner", resourceId))) {
            return false;
        }
        if (FormKey.OPPORTUNITY.getKey().equals(formKey)) {
            return true;
        }
        String inSharedPool = value(table, "in_shared_pool", resourceId);
        return !"1".equals(inSharedPool) && !Boolean.parseBoolean(inSharedPool);
    }

    public boolean hasRelatedReadPermission(String formKey, String resourceId, String userId, String orgId) {
        if (StringUtils.isAnyBlank(formKey, resourceId, userId, orgId)) {
            return false;
        }
        String table = tableName(formKey);
        if (!orgId.equals(value(table, "organization_id", resourceId))
                || (FormKey.ofKey(formKey) == null && !formKey.equals(value(table, "custom_form_id", resourceId)))) {
            return false;
        }
        if (isIsolatedData(formKey, resourceId, userId, orgId)) {
            return false;
        }
        if (FormKey.CUSTOMER.getKey().equals(formKey) || FormKey.CLUE.getKey().equals(formKey)) {
            String inSharedPool = value(table, "in_shared_pool", resourceId);
            if ("1".equals(inSharedPool) || Boolean.parseBoolean(inSharedPool)) {
                return false;
            }
        }
        Set<String> permissions = permissionCache.getPermissionIds(userId, orgId);
        boolean admin = InternalUser.ADMIN.getValue().equals(userId);
        for (var parent : PARENT_PERMISSIONS.entrySet()) {
            if (!admin && !permissions.contains(parent.getValue())) {
                continue;
            }
            for (FormDetailTab tab : detailTabs(parent.getKey(), orgId)) {
                if (!Boolean.TRUE.equals(tab.getEnable())) {
                    continue;
                }
                InternalDetailTab internal = InternalDetailTab.findByInternalKey(parent.getKey(), tab.getInternalKey())
                        .orElse(null);
                if (internal != null) {
                    if (hasInternalRelation(internal, formKey, resourceId, table, userId, orgId)) {
                        return true;
                    }
                } else if (tab.getRelatedForm() != null && tab.getRelatedField() != null
                        && formKey.equals(tab.getRelatedForm().getIdAsString())
                        && hasFieldRelation(parent.getKey(), tab.getRelatedField().getIdAsString(),
                                formKey, resourceId, table, userId, orgId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<FormDetailTab> detailTabs(String parentForm, String orgId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getConfig(parentForm, orgId);
        List<FormDetailTab> tabs = new ArrayList<>();
        if (config != null && config.getFormProp() != null && config.getFormProp().getDetailTabs() != null) {
            tabs.addAll(config.getFormProp().getDetailTabs());
        }
        // 旧表单未保存标签配置时，内置标签仍按系统默认启用；已保存的禁用状态优先。
        for (InternalDetailTab internal : InternalDetailTab.values()) {
            if (parentForm.equals(internal.getFormKey())
                    && tabs.stream().noneMatch(tab -> internal.name().equals(tab.getInternalKey()))) {
                FormDetailTab tab = new FormDetailTab();
                tab.setInternalKey(internal.name());
                tab.setEnable(true);
                tabs.add(tab);
            }
        }
        return tabs;
    }

    private boolean hasInternalRelation(InternalDetailTab tab, String formKey, String resourceId,
            String table, String userId, String orgId) {
        if (tab == InternalDetailTab.CLUE_TRANSITION_RECORD && FormKey.CUSTOMER.getKey().equals(formKey)) {
            Clue clue = new Clue();
            clue.setTransitionType("CUSTOMER");
            clue.setTransitionId(resourceId);
            clue.setOwner(userId);
            clue.setOrganizationId(orgId);
            clue.setInSharedPool(false);
            return clueMapper.countByExample(clue) > 0;
        }
        if (tab == InternalDetailTab.OPPORTUNITY_CONTACT && FormKey.CONTACT.getKey().equals(formKey)) {
            Opportunity opportunity = new Opportunity();
            opportunity.setContactId(resourceId);
            opportunity.setOwner(userId);
            opportunity.setOrganizationId(orgId);
            return opportunityMapper.countByExample(opportunity) > 0;
        }
        if (tab == InternalDetailTab.CUSTOMER_RELATION && FormKey.CUSTOMER.getKey().equals(formKey)) {
            CustomerRelation relation = new CustomerRelation();
            relation.setSourceCustomerId(resourceId);
            for (CustomerRelation item : customerRelationMapper.select(relation)) {
                if (isOwner(FormKey.CUSTOMER.getKey(), item.getTargetCustomerId(), userId, orgId)) {
                    return true;
                }
            }
            relation = new CustomerRelation();
            relation.setTargetCustomerId(resourceId);
            for (CustomerRelation item : customerRelationMapper.select(relation)) {
                if (isOwner(FormKey.CUSTOMER.getKey(), item.getSourceCustomerId(), userId, orgId)) {
                    return true;
                }
            }
            return false;
        }
        if (!formKey.equals(tab.getRelatedFormKey())) {
            return false;
        }
        if (tab.getRelatedFieldInternalKey() != null) {
            BusinessModuleField field = BusinessModuleField.ofKey(tab.getRelatedFieldInternalKey());
            return field != null && hasParentReadPermission(tab.getFormKey(),
                    value(table, CaseFormatUtils.camelToUnderscore(field.getBusinessKey()), resourceId), formKey,
                    userId, orgId);
        }
        if (tab == InternalDetailTab.CUSTOMER_PAYMENT_PLAN || tab == InternalDetailTab.CUSTOMER_PAYMENT_RECORD
                || tab == InternalDetailTab.CUSTOMER_INVOICE) {
            String contractId = value(table, "contract_id", resourceId);
            return StringUtils.isNotBlank(contractId) && orgId.equals(value("contract", "organization_id", contractId))
                    && hasParentReadPermission(FormKey.CUSTOMER.getKey(), value("contract", "customer_id", contractId),
                            formKey, userId, orgId);
        }
        return false;
    }

    private boolean hasFieldRelation(String parentForm, String fieldId, String formKey, String resourceId,
            String table, String userId, String orgId) {
        return fieldRelatedIds(parentForm, fieldId, formKey, resourceId, table, orgId).stream()
                .anyMatch(parentId -> hasParentReadPermission(parentForm, parentId, formKey, userId, orgId));
    }

    private boolean hasParentReadPermission(String formKey, String resourceId, String relatedForm, String userId,
            String orgId) {
        if (FormKey.CUSTOMER.getKey().equals(formKey)) {
            if (getCustomerTabDataPermission(resourceId, userId, orgId) == null) {
                return false;
            }
            if (userId.equals(value("customer", "owner", resourceId)) || InternalUser.ADMIN.getValue().equals(userId)) {
                return true;
            }
            String permission = switch (FormKey.ofKey(relatedForm)) {
            case CLUE -> PermissionConstants.CLUE_MANAGEMENT_READ;
            case CUSTOMER -> PermissionConstants.CUSTOMER_MANAGEMENT_READ;
            case CONTACT -> PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ;
            case FOLLOW_RECORD -> PermissionConstants.FOLLOW_RECORD_READ;
            case FOLLOW_PLAN -> PermissionConstants.FOLLOW_PLAN_READ;
            case OPPORTUNITY -> PermissionConstants.OPPORTUNITY_MANAGEMENT_READ;
            case PRODUCT -> PermissionConstants.PRODUCT_MANAGEMENT_READ;
            case PRICE -> PermissionConstants.PRICE_READ;
            case QUOTATION -> PermissionConstants.OPPORTUNITY_QUOTATION_READ;
            case CONTRACT -> PermissionConstants.CONTRACT_READ;
            case INVOICE -> PermissionConstants.CONTRACT_INVOICE_READ;
            case CONTRACT_PAYMENT_PLAN -> PermissionConstants.CONTRACT_PAYMENT_PLAN_READ;
            case CONTRACT_PAYMENT_RECORD -> PermissionConstants.CONTRACT_PAYMENT_RECORD_READ;
            case ORDER -> PermissionConstants.ORDER_READ;
            case null -> PermissionConstants.CUSTOM_FORM_READ;
            };
            return permissionCache.getPermissionIds(userId, orgId).contains(permission);
        }
        return isOwner(formKey, resourceId, userId, orgId);
    }

    private List<String> fieldRelatedIds(String parentForm, String fieldId, String formKey, String resourceId,
            String table, String orgId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getConfig(formKey, orgId);
        if (config == null || config.getFields() == null) {
            return List.of();
        }
        for (BaseField field : config.getFields()) {
            if (!fieldId.equals(field.getId()) || !(field instanceof DatasourceField source)
                    || !parentForm.equalsIgnoreCase(source.getDataSourceType())
                    || StringUtils.isNotBlank(field.getSubTableFieldId())) {
                continue;
            }
            BusinessModuleField business = BusinessModuleField.ofKey(field.getInternalKey());
            String relatedValue = business == null
                    ? extStatisticMapper.selectFieldValue(table + "_field", table + "_field_blob",
                            fieldId, resourceId, field.isBlob())
                    : value(table, CaseFormatUtils.camelToUnderscore(business.getBusinessKey()), resourceId);
            return relatedIds(relatedValue);
        }
        return List.of();
    }

    private List<String> relatedIds(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        if (!value.stripLeading().startsWith("[")) {
            return List.of(value);
        }
        try {
            return JSON.parseArray(value, String.class);
        } catch (RuntimeException e) {
            // 损坏的历史多选值不能作为授权依据。
            return List.of();
        }
    }

    private String value(String table, String column, String resourceId) {
        return extStatisticMapper.selectBusinessFieldValue(table, column, resourceId);
    }

    private String tableName(String formKey) {
        return switch (FormKey.ofKey(formKey)) {
        case CLUE -> "clue";
        case CUSTOMER -> "customer";
        case CONTACT -> "customer_contact";
        case OPPORTUNITY -> "opportunity";
        case PRODUCT -> "product";
        case PRICE -> "product_price";
        case QUOTATION -> "opportunity_quotation";
        case CONTRACT -> "contract";
        case INVOICE -> "contract_invoice";
        case CONTRACT_PAYMENT_PLAN -> "contract_payment_plan";
        case CONTRACT_PAYMENT_RECORD -> "contract_payment_record";
        case ORDER -> "sales_order";
        case FOLLOW_RECORD -> "follow_up_record";
        case FOLLOW_PLAN -> "follow_up_plan";
        case null -> "custom_form_data";
        };
    }
}
