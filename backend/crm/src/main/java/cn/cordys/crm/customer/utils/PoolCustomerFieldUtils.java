package cn.cordys.crm.customer.utils;

import cn.cordys.crm.customer.dto.response.CustomerListResponse;

import java.util.LinkedHashMap;

public class PoolCustomerFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = CustomerFieldUtils.getSystemFieldMap(data);
        systemFieldMap.put("reasonId", data.getReasonName());
        return systemFieldMap;
    }

}