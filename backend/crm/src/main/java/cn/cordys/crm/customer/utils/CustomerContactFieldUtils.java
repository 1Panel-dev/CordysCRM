package cn.cordys.crm.customer.utils;

import cn.cordys.common.util.TimeUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;

import java.util.LinkedHashMap;

public class CustomerContactFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerContactListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("customerId", data.getCustomerName());
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("phone", data.getPhone());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("enable", Translator.get("log.enable." + data.getEnable()));
        systemFieldMap.put("disableReason", data.getDisableReason());
        systemFieldMap.put("departmentId", data.getDepartmentName());

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
        return systemFieldMap;
    }
}
