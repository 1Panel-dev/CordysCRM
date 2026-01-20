package cn.cordys.crm.customer.utils;

import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;

import java.util.LinkedHashMap;

public class CustomerFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerListResponse data) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("collectionTime", TimeUtils.getDateTimeStr(data.getCollectionTime()));
        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
        systemFieldMap.put("follower", data.getFollowerName());
        systemFieldMap.put("followTime", TimeUtils.getDateTimeStr(data.getFollowTime()));
        systemFieldMap.put("reservedDays", data.getReservedDays());
        systemFieldMap.put("recyclePoolName", data.getRecyclePoolName());
        systemFieldMap.put("departmentId", data.getDepartmentName());
        return systemFieldMap;
    }

}
