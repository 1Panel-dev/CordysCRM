package io.cordys.common.constants;

import io.cordys.crm.system.dto.field.base.BaseField;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）(标准字段)
 *
 * @Author: jianxing
 * @CreateTime: 2025-02-18  17:27
 */
@Getter
public enum BusinessModuleField {

    /*------ start: CUSTOMER ------*/
    /**
     * 客户名称
     */
    CUSTOMER_NAME("customerName", "name", Set.of("rules.required"), FormKey.CUSTOMER.getKey()),
    /**
     * 负责人
     */
    CUSTOMER_OWNER("customerOwner", "owner", Set.of("rules.required"), FormKey.CUSTOMER.getKey()),
    /*------ end: CUSTOMER ------*/

    /*------ start: CLUE ------*/
    /**
     * 线索名称
     */
    CLUE_NAME("clueName", "name", Set.of("rules.required"), FormKey.CLUE.getKey()),
    /**
     * 负责人
     */
    CLUE_OWNER("clueOwner", "owner", Set.of("rules.required"), FormKey.CLUE.getKey()),
    /**
     * 联系人
     */
    CLUE_CONTACT("clueContactName", "contact", Set.of(), FormKey.CLUE.getKey()),
    /**
     * 联系人电话
     */
    CLUE_CONTACT_PHONE("clueContactPhone", "phone", Set.of(), FormKey.CLUE.getKey()),
    /**
     * 意向产品
     */
    CLUE_PRODUCTS("clueProduct", "products", Set.of(), FormKey.CLUE.getKey()),
    /*------ end: CUSTOMER ------*/

    /*------ start: CUSTOMER_MANAGEMENT_CONTACT ------*/
    /**
     * 联系人客户id
     */
    CUSTOMER_CONTACT_CUSTOMER("contactCustomer", "customerId", Set.of(), FormKey.CONTACT.getKey()),
    /**
     * 联系人责任人
     */
    CUSTOMER_CONTACT_OWNER("contactOwner", "owner", Set.of("rules.required"), FormKey.CONTACT.getKey()),
    /**
     * 联系人名称
     */
    CUSTOMER_CONTACT_NAME("contactName", "name", Set.of("rules.required"), FormKey.CONTACT.getKey()),
    /**
     * 联系人电话
     */
    CUSTOMER_CONTACT_PHONE("contactPhone", "phone", Set.of(), FormKey.CONTACT.getKey()),
    /*------ end: CUSTOMER_MANAGEMENT_CONTACT ------*/


    /*------ start: OPPORTUNITY ------*/
    /**
     * 商机名称
     */
    OPPORTUNITY_NAME("opportunityName", "name", Set.of("rules.required"), FormKey.OPPORTUNITY.getKey()),
    /**
     * 客户名称
     */
    OPPORTUNITY_CUSTOMER_NAME("opportunityCustomer", "customerId", Set.of("rules.required"), FormKey.OPPORTUNITY.getKey()),
    /**
     * 商机金额
     */
    OPPORTUNITY_AMOUNT("opportunityPrice", "amount", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 可能性
     */
    OPPORTUNITY_POSSIBLE("opportunityWinRate", "possible", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 结束时间
     */
    OPPORTUNITY_END_TIME("opportunityEndTime", "expectedEndTime", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 意向产品
     */
    OPPORTUNITY_PRODUCTS("opportunityProduct", "products", Set.of(), FormKey.OPPORTUNITY.getKey()),
    /**
     * 联系人
     */
    OPPORTUNITY_CONTACT("opportunityContact", "contactId",Set.of(), FormKey.OPPORTUNITY.getKey()),

    /**
     * 负责人
     */
    OPPORTUNITY_OWNER("opportunityOwner", "owner", Set.of("rules.required"), FormKey.OPPORTUNITY.getKey()),
    /*------ end: OPPORTUNITY ------*/



    /*------ start: FOLLOW_UP_RECORD ------*/
    /**
     * 跟进类型
     */
    FOLLOW_RECORD_TYPE("recordType", "type", Set.of("options", "rules.required"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 客户id
     */
    FOLLOW_RECORD_CUSTOMER("recordCustomer", "customerId", Set.of("rules.required"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 商机id
     */
    FOLLOW_RECORD_OPPORTUNITY("recordOpportunity", "opportunityId", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 线索id
     */
    FOLLOW_RECORD_CLUE("recordClue", "clueId", Set.of("rules.required"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 责任人id
     */
    FOLLOW_RECORD_OWNER("recordOwner", "owner", Set.of("rules.required"), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 联系人id
     */
    FOLLOW_RECORD_CONTACT("recordContact", "contactId", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进内容
     */
    FOLLOW_RECORD_CONTENT("recordDescription", "content", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进时间
     */
    FOLLOW_RECORD_TIME("recordTime", "followTime", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /**
     * 跟进方式
     */
    FOLLOW_METHOD("recordMethod", "followMethod", Set.of(), FormKey.FOLLOW_RECORD.getKey()),
    /*------ end: FOLLOW_UP_RECORD ------*/


    /*------ start: FOLLOW_UP_PLAN ------*/
    /**
     * 跟进类型
     */
    FOLLOW_PLAN_TYPE("planType", "type", Set.of("options", "rules.required"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 客户id
     */
    FOLLOW_PLAN_CUSTOMER("planCustomer", "customerId", Set.of("rules.required"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 商机id
     */
    FOLLOW_PLAN_OPPORTUNITY("planOpportunity", "opportunityId", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 线索id
     */
    FOLLOW_PLAN_CLUE("planClue", "clueId", Set.of("rules.required"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 责任人id
     */
    FOLLOW_PLAN_OWNER("planOwner", "owner", Set.of("rules.required"), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 联系人id
     */
    FOLLOW_PLAN_CONTACT("planContact", "contactId", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 预计开始时间
     */
    FOLLOW_PLAN_ESTIMATED_TIME("planStartTime", "estimatedTime", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 预计沟通内容
     */
    FOLLOW_PLAN_CONTENT("planContent", "content", Set.of(), FormKey.FOLLOW_PLAN.getKey()),
    /**
     * 跟进方式
     */
    FOLLOW_PLAN_METHOD("planMethod", "method", Set.of(), FormKey.FOLLOW_PLAN.getKey()),


    /*------ end: FOLLOW_UP_PLAN ------*/

    /*------ start: PRODUCT ------*/
    PRODUCT_NAME("productName", "name", Set.of("rules.required"), FormKey.PRODUCT.getKey()),
    PRODUCT_PRICE("productPrice", "price", Set.of(), FormKey.PRODUCT.getKey()),
    PRODUCT_STATUS("productStatus", "status", Set.of("rules.required"), FormKey.PRODUCT.getKey());
    /*------ end: PRODUCT ------*/
    /**
     * 字段 key，field.json 中的 internalKey
     */
    private final String key;
    /**
     * 业务字段 key
     */
    private final String businessKey;
    /**
     * 禁止修改的参数列表
     */
    private final Set<String> disabledProps;
    /**
     * 表单 key
     */
    private final String formKey;

    BusinessModuleField(String key, String businessKey, String formKey) {
        this(key, businessKey, Set.of(), formKey);
    }

    BusinessModuleField(String key, String businessKey, Set<String> disabledProps, String formKey) {
        this.key = key;
        this.businessKey = businessKey;
        this.disabledProps = disabledProps;
        this.formKey = formKey;
    }

    /**
     * 判断业务字段是否被删除
     * @param formKey 表单 key
     * @param fields 字段集合
     * @return 是否被删除
     */
    public static boolean isBusinessDeleted(String formKey, List<BaseField> fields) {
        List<BusinessModuleField> formBusinessFields = Arrays.stream(BusinessModuleField.values()).filter(field -> StringUtils.equals(formKey, field.getFormKey())).toList();
        if (CollectionUtils.isEmpty(formBusinessFields)) {
            return false;
        }
        return formBusinessFields.stream()
                .anyMatch(businessField ->
                        fields.stream().noneMatch(field -> StringUtils.equals(businessField.getKey(), field.getInternalKey())));
    }
}
