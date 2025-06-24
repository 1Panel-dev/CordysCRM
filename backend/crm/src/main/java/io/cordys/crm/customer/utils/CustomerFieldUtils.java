package io.cordys.crm.customer.utils;

import io.cordys.common.util.TimeUtils;
import io.cordys.crm.customer.dto.response.CustomerListResponse;

import java.util.LinkedHashMap;

public class CustomerFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(CustomerListResponse data) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("name", data.getName());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("collectionTime",TimeUtils.getDataTimeStr(data.getCollectionTime()));
        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
        systemFiledMap.put("followerName", data.getFollowerName());
        systemFiledMap.put("followTime", TimeUtils.getDataTimeStr(data.getFollowTime()));
        systemFiledMap.put("reservedDays", data.getReservedDays());
        systemFiledMap.put("recyclePoolName", data.getRecyclePoolName());
        return systemFiledMap;
    }

}
