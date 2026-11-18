package cn.cordys.common.constants;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jianxing
 */
public enum InternalUserView {
    /**
     * 全部视图
     */
    ALL,
    /**
     * 个人视图
     */
    SELF,
    /**
     * 部门视图
     */
    DEPARTMENT,
    /**
     * 已转线索视图
     */
    CLUE_TRANSITION,
    /**
     * 我协作的客户视图
     */
    CUSTOMER_COLLABORATION,
    /**
     * 客户协作范围内的业务数据视图
     */
    COLLABORATION_VISIBLE,
    /**
     * 赢单视图
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

    public static boolean isVisible(String searchType, String permission) {
        return (Strings.CS.equals(PermissionConstants.CUSTOMER_MANAGEMENT_READ, permission)
                && Strings.CS.equals(CUSTOMER_COLLABORATION.name(), searchType))
                || (permission != null
                        && List.of(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, PermissionConstants.CONTRACT_READ,
                                PermissionConstants.ORDER_READ).contains(permission)
                        && Strings.CS.equals(COLLABORATION_VISIBLE.name(), searchType));
    }
}
