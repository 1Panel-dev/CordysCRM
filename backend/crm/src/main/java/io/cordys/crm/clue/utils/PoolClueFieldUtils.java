package io.cordys.crm.clue.utils;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.clue.dto.response.ClueListResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PoolClueFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(ClueListResponse data, Map<String, List<OptionDTO>> optionMap) {
        LinkedHashMap<String, Object> systemFiledMap = ClueFieldUtils.getSystemFieldMap(data, optionMap);
        systemFiledMap.put("reasonId", data.getReasonName());
        return systemFiledMap;
    }
}
