package io.cordys.crm.customer.constants;

import org.apache.commons.lang3.StringUtils;

public class RecycleConditionColumnKey {

	public static final String STORAGE_TIME = "storageTime";
	public static final String CREATE_TIME = "createTime";
	public static final String OPPORTUNITY_STAGE = "opportunityStage";

	public static boolean matchReserved(String key) {
		return StringUtils.equalsAny(key, STORAGE_TIME, CREATE_TIME);
	}
}
