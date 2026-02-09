package cn.cordys.crm.product.utils;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.util.TimeUtils;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
public class ProductPriceUtils {

	public static LinkedHashMap<String, Object> getSystemFieldMap(ProductPriceResponse data, Map<String, List<OptionDTO>> optionMap) {
		LinkedHashMap<String, Object> systemFieldMap = new LinkedHashMap<>();
		systemFieldMap.put("name", data.getName());
		systemFieldMap.put("status", getStatusName(data.getStatus(), optionMap));
		systemFieldMap.put("createUser", data.getCreateUserName());
		systemFieldMap.put("createTime", TimeUtils.getDateTimeStr(data.getCreateTime()));
		systemFieldMap.put("updateUser", data.getUpdateUserName());
		systemFieldMap.put("updateTime", TimeUtils.getDateTimeStr(data.getUpdateTime()));
		return systemFieldMap;
	}

	public static String getStatusName(String status, Map<String, List<OptionDTO>> optionMap) {
		if (optionMap.containsKey(BusinessModuleField.PRICE_STATUS.getBusinessKey())) {
			for (OptionDTO option : optionMap.get(BusinessModuleField.PRICE_STATUS.getBusinessKey())) {
				if (option.getId().equals(status)) {
					return option.getName();
				}
			}
		}
		return status;
	}
}
