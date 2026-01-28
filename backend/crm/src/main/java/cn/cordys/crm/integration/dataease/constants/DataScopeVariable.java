package cn.cordys.crm.integration.dataease.constants;

import cn.cordys.common.constants.PermissionConstants;
import java.util.Objects;

/** 数据权限变量与部门范围变量的绑定枚举。 */
public enum DataScopeVariable {
  ACCOUNT_DATA_SCOPE_TYPE(
      PermissionConstants.CUSTOMER_MANAGEMENT_READ,
      DataScopeDeptVariable.ACCOUNT_DATA_SCOPE_DEPT_ID),
  LEAD_DATA_SCOPE_TYPE(
      PermissionConstants.CLUE_MANAGEMENT_READ, DataScopeDeptVariable.LEAD_DATA_SCOPE_DEPT_ID),
  OPPORTUNITY_DATA_SCOPE_TYPE(
      PermissionConstants.OPPORTUNITY_MANAGEMENT_READ,
      DataScopeDeptVariable.OPPORTUNITY_DATA_SCOPE_DEPT_ID);

  private final String permission;
  private final DataScopeDeptVariable dataScopeDeptVariable;

  DataScopeVariable(String permission, DataScopeDeptVariable dataScopeDeptVariable) {
    this.permission = Objects.requireNonNull(permission, "permission");
    this.dataScopeDeptVariable =
        Objects.requireNonNull(dataScopeDeptVariable, "dataScopeDeptVariable");
  }

  public String getPermission() {
    return permission;
  }

  public DataScopeDeptVariable getDataScopeDeptVariable() {
    return dataScopeDeptVariable;
  }
}
