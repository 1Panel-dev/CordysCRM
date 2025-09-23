package cn.cordys.crm.system.constants;

import org.apache.commons.lang3.Strings;

public class RecycleConditionColumnKey {

    public static final String STORAGE_TIME = "storageTime";
    public static final String CREATE_TIME = "createTime";
    public static final String OPPORTUNITY_STAGE = "opportunityStage";

    public static boolean matchReserved(String key) {
        return Strings.CS.equalsAny(key, STORAGE_TIME, CREATE_TIME);
    }
}
