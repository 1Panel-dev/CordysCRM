package cn.cordys.common.utils;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpportunityFieldUtils {


    public static LinkedHashMap<String, Object> getSystemFieldMap(OpportunityListResponse data, Map<String, List<OptionDTO>> optionMap, Map<String, String> stageConfigMap) {
        LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
        systemFieldMap.put("name", data.getName());
        systemFieldMap.put("customerId", data.getCustomerName());
        systemFieldMap.put("amount", data.getAmount());
        systemFieldMap.put("expectedEndTime", TimeUtils.getDateStr(data.getExpectedEndTime()));
        systemFieldMap.put("actualEndTime", TimeUtils.getDateStr(data.getActualEndTime()));
        systemFieldMap.put("failureReason", data.getFailureReason());
        systemFieldMap.put("possible", data.getPossible());
        systemFieldMap.put("products", getProducts(optionMap, data.getProducts()));
        systemFieldMap.put("contactId", data.getContactName());
        systemFieldMap.put("owner", data.getOwnerName());

        systemFieldMap.put("stage", stageConfigMap.get(data.getStage()));
        systemFieldMap.put("followerName", data.getFollowerName());
        systemFieldMap.put("followTime", TimeUtils.getDateTimeStr(data.getFollowTime()));
        systemFieldMap.put("reservedDays", data.getReservedDays());
        systemFieldMap.put("departmentId", data.getDepartmentName());

        systemFieldMap.put("createUser", data.getCreateUserName());
        systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
        systemFieldMap.put("updateUser", data.getUpdateUserName());
        systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));


        return systemFieldMap;
    }


    private static Object getProducts(Map<String, List<OptionDTO>> optionMap, List<String> products) {
        List<String> productNames = new ArrayList<>();
        if (optionMap.containsKey(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey()) && CollectionUtils.isNotEmpty(products)) {
            Map<String, String> productsMap = optionMap.get(BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey()).stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
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
