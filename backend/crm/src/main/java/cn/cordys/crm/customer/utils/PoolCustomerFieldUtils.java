package cn.cordys.crm.customer.utils;

import cn.cordys.crm.customer.dto.response.CustomerListResponse;

import java.util.LinkedHashMap;

public class PoolCustomerFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = CustomerFieldUtils.getSystemFieldMap(data);
        systemFiledMap.put("reasonId", data.getReasonName());
        return systemFiledMap;
    }

}