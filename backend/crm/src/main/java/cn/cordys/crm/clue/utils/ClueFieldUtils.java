package cn.cordys.crm.clue.utils;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClueFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(ClueListResponse data, Map<String, List<OptionDTO>> optionMap) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("owner", data.getOwnerName());
        systemFieldMap.put("contact", data.getContact());
        systemFieldMap.put("phone", data.getPhone());
        systemFieldMap.put("products", getProducts(optionMap, data.getProducts()));
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

    public static Object getProducts(Map<String, List<OptionDTO>> optionMap, List<String> products) {
        List<String> productNames = new ArrayList<>();
        if (optionMap.containsKey(BusinessModuleField.CLUE_PRODUCTS.getBusinessKey()) && CollectionUtils.isNotEmpty(products)) {
            Map<String, String> productsMap = optionMap.get(BusinessModuleField.CLUE_PRODUCTS.getBusinessKey()).stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
            products.forEach(product -> {
                if (productsMap.containsKey(product)) {
                    productNames.add(productsMap.get(product));
                }
            });
        }
        if (CollectionUtils.isEmpty(productNames)) {
            return StringUtils.EMPTY;
        }
        return StringUtils.join(productNames, ",");
    }

}
