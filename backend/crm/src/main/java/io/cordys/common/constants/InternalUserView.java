package io.cordys.common.constants;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-25  14:05
 */
public enum InternalUserView {
    ALL,
    SELF,
    DEPARTMENT,
    /**
     * 协作客户
     */
    CUSTOMER_COLLABORATION,
    /**
     * 成功机会
     */
    OPPORTUNITY_SUCCESS;

    public static final String CURRENT_USER = "CURRENT_USER";


    public static List<String> getCurrentUserArrayValue() {
        List<String> values = new ArrayList<>(0);
        values.add(CURRENT_USER);
        return values;
    }

    public static boolean isInternalUserView(String viewId) {
        if (StringUtils.isBlank(viewId)) {
            return false;
        }
        return Arrays.stream(InternalUserView.values())
                .map(InternalUserView::name)
                .collect(Collectors.toSet()).contains(viewId);
    }

    public static boolean isAll(String searchType) {
        return Strings.CS.equals(ALL.name(), searchType);
    }

    public static boolean isSelf(String searchType) {
        return Strings.CS.equals(SELF.name(), searchType);
    }

    public static boolean isDepartment(String searchType) {
        return Strings.CS.equals(DEPARTMENT.name(), searchType);
    }

    public static boolean isVisible(String searchType) {
        return Strings.CS.equals(CUSTOMER_COLLABORATION.name(), searchType);
    }
}
