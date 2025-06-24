package io.cordys.common.utils;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.opportunity.constants.StageType;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.system.dto.field.base.BaseField;
import org.apache.commons.collections.CollectionUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpportunityFieldUtils {


    public static LinkedHashMap<String, Object> getSystemFieldMap(OpportunityListResponse data, Map<String, List<OptionDTO>> optionMap) {
        LinkedHashMap<String, Object> systemFiledMap = new LinkedHashMap<>();
        systemFiledMap.put("name", data.getName());
        systemFiledMap.put("customerId", data.getCustomerName());
        systemFiledMap.put("amount", data.getAmount());
        systemFiledMap.put("possible", data.getPossible());
        systemFiledMap.put("products", JSON.toJSONString(getProducts(optionMap, data.getProducts())));
        systemFiledMap.put("contactId", data.getContactName());
        systemFiledMap.put("owner", data.getOwnerName());

        systemFiledMap.put("stage", Translator.get(data.getStage()));
        systemFiledMap.put("followerName", data.getFollowerName());
        systemFiledMap.put("followTime", handleTime(data.getFollowTime()));
        systemFiledMap.put("reservedDays", data.getReservedDays());
        systemFiledMap.put("status", Translator.get("log.opportunity.status." + data.getStatus().toString()));
        systemFiledMap.put("createUser", data.getCreateUserName());
        systemFiledMap.put("createTime", handleTime(data.getCreateTime()));
        systemFiledMap.put("updateUser", data.getUpdateUserName());
        systemFiledMap.put("updateTime", handleTime(data.getUpdateTime()));


        return systemFiledMap;
    }

    private static Object handleTime(Long followTime) {
        if (followTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            String formattedDate = formatter.format(Instant.ofEpochMilli(followTime));
            return formattedDate;
        }
        return null;
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
        return productNames;
    }
}
