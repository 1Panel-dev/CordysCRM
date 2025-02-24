package io.cordys.common.constants;

import lombok.Getter;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）
 * @Author: jianxing
 * @CreateTime: 2025-02-18  17:27
 */
@Getter
public enum BusinessModuleField {

    /*------ start: CUSTOMER ------*/
    /**
     * 客户名称
     */
    CUSTOMER_NAME("customerName", "name"),
    /**
     * 负责人
     */
    CUSTOMER_OWNER("customerOwner", "owner"),
    /*------ end: CUSTOMER ------*/
    
    
    /*------ start: CUSTOMER_MANAGEMENT_CONTACT ------*/
    /**
     * 负责人
     */
    CUSTOMER_CONTACT_CUSTOMER("customerContactCustomer", "customerId"),
    /**
     * 负责人
     */
    CUSTOMER_CONTACT_NAME("customerContactName", "name");
    /*------ end: CUSTOMER_MANAGEMENT_CONTACT ------*/

    private final String key;
    private final String businessKey;

    BusinessModuleField(String key, String businessKey) {
        this.key = key;
        this.businessKey = businessKey;
    }
}
