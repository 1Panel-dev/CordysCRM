package cn.cordys.crm.integration.dataease.constanst;

import cn.cordys.common.constants.PermissionConstants;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-19  16:29
 */
public enum DataScopeVariable {
    ACCOUNT_DATA_SCOPE_TYPE(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DataScopeDeptVariable.ACCOUNT_DATA_SCOPE_DEPT_ID),
    LEAD_DATA_SCOPE_TYPE(PermissionConstants.CLUE_MANAGEMENT_READ, DataScopeDeptVariable.LEAD_DATA_SCOPE_DEPT_ID),
    OPPORTUNITY_DATA_SCOPE_TYPE(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, DataScopeDeptVariable.OPPORTUNITY_DATA_SCOPE_DEPT_ID);

    private String permission;
    private DataScopeDeptVariable dataScopeDeptVariable;
    DataScopeVariable(String permission, DataScopeDeptVariable dataScopeDeptVariable) {
        this.permission = permission;
        this.dataScopeDeptVariable = dataScopeDeptVariable;
    }

    public String getPermission() {
        return permission;
    }

    public DataScopeDeptVariable getDataScopeDeptVariable() {
        return dataScopeDeptVariable;
    }
}
