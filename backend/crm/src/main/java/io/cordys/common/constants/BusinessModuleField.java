package io.cordys.common.constants;

import lombok.Getter;

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
    CUSTOMER_NAME("customerName", "name", Set.of("rules")),
    /**
     * 负责人
     */
    CUSTOMER_OWNER("customerOwner", "owner", Set.of("rules")),
    /*------ end: CUSTOMER ------*/


    /*------ start: CUSTOMER_MANAGEMENT_CONTACT ------*/
    /**
     * 联系人客户id
     */
    CUSTOMER_CONTACT_CUSTOMER("contactCustomer", "customerId"),
    /**
     * 联系人名称
     */
    CUSTOMER_CONTACT_NAME("contactName", "name", Set.of("rules")),
    /**
     * 联系人电话
     */
    CUSTOMER_CONTACT_PHONE("contactPhone", "phone"),
    /*------ end: CUSTOMER_MANAGEMENT_CONTACT ------*/


    /*------ start: OPPORTUNITY ------*/
    /**
     * 商机名称
     */
    OPPORTUNITY_NAME("opportunityName", "name", Set.of("rules")),
    /**
     * 客户名称
     */
    OPPORTUNITY_CUSTOMER_NAME("opportunityCustomer", "customerId", Set.of("rules")),
    /**
     * 商机金额
     */
    OPPORTUNITY_AMOUNT("opportunityPrice", "amount", Set.of("rules")),
    /**
     * 可能性
     */
    OPPORTUNITY_POSSIBLE("opportunityWinRate", "possible", Set.of("rules")),
    /**
     * 意向产品
     */
    OPPORTUNITY_PRODUCTS("opportunityProduct", "products", Set.of("rules")),
    /**
     * 联系人
     */
    OPPORTUNITY_CONTACT("opportunityContact", "contactId",Set.of("rules")),

    /**
     * 负责人
     */
    OPPORTUNITY_OWNER("opportunityOwner", "owner", Set.of("rules")),
    /*------ end: OPPORTUNITY ------*/



    /*------ start: FOLLOW_UP_RECORD ------*/
    /**
     * 客户id
     */
    FOLLOW_RECORD_CUSTOMER("recordCustomer", "customerId"),
    /**
     * 商机id
     */
    FOLLOW_RECORD_OPPORTUNITY("recordOpportunity", "opportunityId"),
    /**
     * 线索id
     */
    FOLLOW_RECORD_CLUE("recordClue", "clueId"),
    /**
     * 责任人id
     */
    FOLLOW_RECORD_OWNER("recordOwner", "owner", Set.of("rules")),
    /**
     * 联系人id
     */
    FOLLOW_RECORD_CONTACT("recordContact", "contactId", Set.of("rules")),

    /**
     * 意向产品
     */
    FOLLOW_RECORD_PRODUCTS("recordProduct", "products", Set.of("rules")),
    /**
     * 跟进内容
     */
    FOLLOW_RECORD_CONTENT("recordDescription", "content", Set.of("rules")),
    /*------ end: FOLLOW_UP_RECORD ------*/


    /*------ start: FOLLOW_UP_PLAN ------*/
    /**
     * 客户id
     */
    FOLLOW_PLAN_CUSTOMER("planCustomer", "customerId"),
    /**
     * 商机id
     */
    FOLLOW_PLAN_OPPORTUNITY("planOpportunity", "opportunityId"),
    /**
     * 线索id
     */
    FOLLOW_PLAN_CLUE("planClue", "clueId"),
    /**
     * 责任人id
     */
    FOLLOW_PLAN_OWNER("planOwner", "owner", Set.of("rules")),
    /**
     * 联系人id
     */
    FOLLOW_PLAN_CONTACT("planContact", "contactId", Set.of("rules")),
    /**
     * 预计开始时间
     */
    FOLLOW_PLAN_ESTIMATED_TIME("planStartTime", "estimatedTime", Set.of("rules")),
    /**
     * 预计沟通内容
     */
    FOLLOW_PLAN_CONTENT("planContent", "content", Set.of("rules"));


    /*------ end: FOLLOW_UP_PLAN ------*/

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

    BusinessModuleField(String key, String businessKey) {
        this(key, businessKey, Set.of());
    }

    BusinessModuleField(String key, String businessKey, Set<String> disabledProps) {
        this.key = key;
        this.businessKey = businessKey;
        this.disabledProps = disabledProps;
    }
}
