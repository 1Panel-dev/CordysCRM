package cn.cordys.crm.customer.utils;

import cn.cordys.crm.customer.dto.response.CustomerListResponse;

import java.util.LinkedHashMap;
import java.util.Locale;

public class PoolCustomerFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerListResponse data, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = CustomerFieldUtils.getSystemFieldMap(data, locale);
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("reasonId", data.getReasonName());
        return systemFieldMap;
    }

}
