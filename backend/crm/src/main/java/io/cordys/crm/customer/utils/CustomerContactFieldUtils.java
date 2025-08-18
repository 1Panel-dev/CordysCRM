package io.cordys.crm.customer.utils;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomerContactFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerContactListResponse data, Map<String, List<OptionDTO>> optionMap) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("customerId", data.getCustomerName());
        systemFiledMap.put("name", data.getName());
        systemFiledMap.put("phone", data.getPhone());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("owner", data.getOwnerName());
        return systemFiledMap;
    }
}
