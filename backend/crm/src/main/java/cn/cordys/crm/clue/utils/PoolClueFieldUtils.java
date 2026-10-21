package cn.cordys.crm.clue.utils;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.clue.dto.response.ClueListResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PoolClueFieldUtils {

    public static LinkedHashMap<String, Object> getSystemFieldMap(ClueListResponse data,
            Map<String, List<OptionDTO>> optionMap, Locale locale) {
        LinkedHashMap<String, Object> systemFieldMap = ClueFieldUtils.getSystemFieldMap(data, optionMap, locale);
        systemFieldMap.put("id", data.getId());
        systemFieldMap.put("reasonId", data.getReasonName());
        return systemFieldMap;
    }
}
