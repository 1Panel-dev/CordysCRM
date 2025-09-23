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
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("name", data.getName());
        systemFiledMap.put("owner", data.getOwnerName());
        systemFiledMap.put("contact", data.getContact());
        systemFiledMap.put("phone", data.getPhone());
        systemFiledMap.put("products", getProducts(optionMap, data.getProducts()));
        systemFiledMap.put("collectionTime", TimeUtils.getDataTimeStr(data.getCollectionTime()));
        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", TimeUtils.getDataTimeStr(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", TimeUtils.getDataTimeStr(data.getUpdateTime()));
        systemFiledMap.put("follower", data.getFollowerName());
        systemFiledMap.put("followTime", TimeUtils.getDataTimeStr(data.getFollowTime()));
        systemFiledMap.put("reservedDays", data.getReservedDays());
        systemFiledMap.put("recyclePoolName", data.getRecyclePoolName());
        systemFiledMap.put("departmentId", data.getDepartmentName());
        return systemFiledMap;
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
