package cn.cordys.common.constants;

import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.SubField;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）(标准字段)
 *
 * @Author: jianxing
 * @CreateTime: 2025-02-18  17:27
 */
@Getter
public enum BusinessModuleField {

    /* ======================= CUSTOMER ======================= */

    /**
     * 客户名称
     */
    CUSTOMER_NAME(
            "customerName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CUSTOMER.getKey()
    ),

    /**
     * 负责人
     */
    CUSTOMER_OWNER(
            "customerOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CUSTOMER.getKey()
    ),

    /* ======================= LEAD ======================= */

    /**
     * 线索名称
     */
    CLUE_NAME(
            "clueName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CLUE.getKey()
    ),

    /**
     * 负责人
     */
    CLUE_OWNER(
            "clueOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CLUE.getKey()
    ),

    /**
     * 联系人
     */
    CLUE_CONTACT(
            "clueContactName",
            "contact",
            Set.of(),
            FormKey.CLUE.getKey()
    ),

    /**
     * 联系人电话
     */
    CLUE_CONTACT_PHONE(
            "clueContactPhone",
            "phone",
            Set.of(),
            FormKey.CLUE.getKey()
    ),

    /**
     * 意向产品
     */
    CLUE_PRODUCTS(
            "clueProduct",
            "products",
            Set.of(),
            FormKey.CLUE.getKey()
    ),

    /* ======================= CUSTOMER_CONTACT ======================= */

    /**
     * 联系人客户 id
     */
    CUSTOMER_CONTACT_CUSTOMER(
            "contactCustomer",
            "customerId",
            Set.of(),
            FormKey.CONTACT.getKey()
    ),

    /**
     * 联系人责任人
     */
    CUSTOMER_CONTACT_OWNER(
            "contactOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CONTACT.getKey()
    ),

    /**
     * 联系人名称
     */
    CUSTOMER_CONTACT_NAME(
            "contactName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CONTACT.getKey()
    ),

    /**
     * 联系人电话
     */
    CUSTOMER_CONTACT_PHONE(
            "contactPhone",
            "phone",
            Set.of(),
            FormKey.CONTACT.getKey()
    ),

    /* ======================= OPPORTUNITY ======================= */

    /**
     * 商机名称
     */
    OPPORTUNITY_NAME(
            "opportunityName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 客户名称
     */
    OPPORTUNITY_CUSTOMER_NAME(
            "opportunityCustomer",
            "customerId",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 商机金额
     */
    OPPORTUNITY_AMOUNT(
            "opportunityPrice",
            "amount",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 可能性
     */
    OPPORTUNITY_POSSIBLE(
            "opportunityWinRate",
            "possible",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 结束时间
     */
    OPPORTUNITY_END_TIME(
            "opportunityEndTime",
            "expectedEndTime",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 意向产品
     */
    OPPORTUNITY_PRODUCTS(
            "opportunityProduct",
            "products",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 联系人
     */
    OPPORTUNITY_CONTACT(
            "opportunityContact",
            "contactId",
            Set.of(),
            FormKey.OPPORTUNITY.getKey()
    ),

    /**
     * 负责人
     */
    OPPORTUNITY_OWNER(
            "opportunityOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.OPPORTUNITY.getKey()
    ),

    /* ======================= FOLLOW_RECORD ======================= */

    FOLLOW_RECORD_TYPE(
            "recordType",
            "type",
            Set.of("options", "rules.required", "mobile", "readable"),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_CUSTOMER(
            "recordCustomer",
            "customerId",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_OPPORTUNITY(
            "recordOpportunity",
            "opportunityId",
            Set.of(),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_CLUE(
            "recordClue",
            "clueId",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_OWNER(
            "recordOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_CONTACT(
            "recordContact",
            "contactId",
            Set.of(),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_CONTENT(
            "recordDescription",
            "content",
            Set.of(),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_RECORD_TIME(
            "recordTime",
            "followTime",
            Set.of(),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    FOLLOW_METHOD(
            "recordMethod",
            "followMethod",
            Set.of(),
            FormKey.FOLLOW_RECORD.getKey()
    ),

    /* ======================= FOLLOW_PLAN ======================= */

    FOLLOW_PLAN_TYPE(
            "planType",
            "type",
            Set.of("options", "rules.required", "mobile", "readable"),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_CUSTOMER(
            "planCustomer",
            "customerId",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_OPPORTUNITY(
            "planOpportunity",
            "opportunityId",
            Set.of(),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_CLUE(
            "planClue",
            "clueId",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_OWNER(
            "planOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_CONTACT(
            "planContact",
            "contactId",
            Set.of(),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_ESTIMATED_TIME(
            "planStartTime",
            "estimatedTime",
            Set.of(),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_CONTENT(
            "planContent",
            "content",
            Set.of(),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    FOLLOW_PLAN_METHOD(
            "planMethod",
            "method",
            Set.of(),
            FormKey.FOLLOW_PLAN.getKey()
    ),

    /* ======================= PRODUCT ======================= */

    PRODUCT_NAME(
            "productName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.PRODUCT.getKey()
    ),

    PRODUCT_PRICE(
            "productPrice",
            "price",
            Set.of(),
            FormKey.PRODUCT.getKey()
    ),

    PRODUCT_STATUS(
            "productStatus",
            "status",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.PRODUCT.getKey()
    ),

    /* ======================= CONTRACT ======================= */

    CONTRACT_NAME(
            "contractName",
            "name",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CONTRACT.getKey()
    ),

    CONTRACT_CUSTOMER_NAME(
            "contractCustomer",
            "customerId",
            Set.of("rules.required", "mobile", "readable", "dataSourceType"),
            FormKey.CONTRACT.getKey()
    ),

    CONTRACT_OWNER(
            "contractOwner",
            "owner",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CONTRACT.getKey()
    ),

    CONTRACT_NO(
            "contractNo",
            "number",
            Set.of("rules.required", "mobile", "readable"),
            FormKey.CONTRACT.getKey()
    );

    /* ======================= FIELDS ======================= */

    private static final Map<String, BusinessModuleField> INTERNAL_CACHE = new HashMap<>();

    static {
        for (BusinessModuleField field : values()) {
            INTERNAL_CACHE.put(field.key, field);
        }
    }

    private final String key;
    private final String businessKey;
    private final Set<String> disabledProps;
    private final String formKey;

    BusinessModuleField(
            String key,
            String businessKey,
            Set<String> disabledProps,
            String formKey
    ) {
        this.key = key;
        this.businessKey = businessKey;
        this.disabledProps = disabledProps;
        this.formKey = formKey;
    }

    /* ======================= METHODS ======================= */

    public static boolean isBusinessDeleted(String formKey, List<BaseField> fields) {
        List<BusinessModuleField> formBusinessFields =
                Arrays.stream(values())
                        .filter(field -> Strings.CS.equals(formKey, field.getFormKey()))
                        .toList();

        if (CollectionUtils.isEmpty(formBusinessFields)) {
            return false;
        }

        return formBusinessFields.stream().anyMatch(businessField ->
                fields.stream().noneMatch(
                        field -> Strings.CS.equals(businessField.getKey(), field.getInternalKey())
                ) && businessField.noneMatchOfSubFields(fields)
        );
    }

    private boolean noneMatchOfSubFields(List<BaseField> fields) {
        for (BaseField field : fields) {
            if (field instanceof SubField subField
                    && CollectionUtils.isNotEmpty(subField.getSubFields())) {

                boolean noneMatch = subField.getSubFields().stream()
                        .noneMatch(sub -> Strings.CS.equals(this.key, sub.getInternalKey()));

                if (!noneMatch) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean hasRepeatName(List<BaseField> fields) {
        return fields.stream()
                .collect(Collectors.groupingBy(BaseField::getName, Collectors.counting()))
                .values()
                .stream()
                .anyMatch(count -> count > 1);
    }

    public static BusinessModuleField ofKey(String internalKey) {
        return INTERNAL_CACHE.get(internalKey);
    }

}
