package io.cordys.common.constants;

import lombok.Getter;

import java.util.Set;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）(标准字段)
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
    FOLLOW_RECORD_OWNER("recordOwner", "owner"),
    /**
     * 联系人id
     */
    FOLLOW_RECORD_CONTACT("recordContact", "contactId");
    /*------ end: FOLLOW_UP_RECORD ------*/

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
