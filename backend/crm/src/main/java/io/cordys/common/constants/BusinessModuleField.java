package io.cordys.common.constants;

import lombok.Getter;

/**
 * 业务模块字段（定义在主表中，有特定业务含义）
 * @Author: jianxing
 * @CreateTime: 2025-02-18  17:27
 */
@Getter
public enum BusinessModuleField {
    /**
     * 客户名称
     */
    CUSTOMER_NAME("customerName", "name"),
    /**
     * 负责人
     */
    CUSTOMER_OWNER("customerOwner", "owner");

    private final String key;
    private final String businessKey;

    BusinessModuleField(String key, String businessKey) {
        this.key = key;
        this.businessKey = businessKey;
    }
}
