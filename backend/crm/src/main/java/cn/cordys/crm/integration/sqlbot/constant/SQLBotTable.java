package cn.cordys.crm.integration.sqlbot.constant;

import cn.cordys.common.constants.PermissionConstants;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

/** SQLBot支持的表枚举类 */
@Getter
public enum SQLBotTable {
  PRODUCT("product", "产品表", null, false),
  SYS_DEPARTMENT("sys_department", "部门表", null, false),
  SYS_USER("sys_user", "用户表", null, false),

  CUSTOMER("customer", "客户表", PermissionConstants.CUSTOMER_MANAGEMENT_READ, true),
  CLUE("clue", "线索表", PermissionConstants.CLUE_MANAGEMENT_READ, true),
  OPPORTUNITY("opportunity", "商机表", PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, true),
  CONTACT("customer_contact", "联系人表", PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, true),

  POOL_CUSTOMER(
      "pool_customer",
      "公海中的客户表",
      PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ,
      true,
      CUSTOMER.getTableName()),
  POOL_CLUE(
      "pool_clue",
      "线索池中的线索表",
      PermissionConstants.CLUE_MANAGEMENT_POOL_READ,
      true,
      CLUE.getTableName()),
  ;

  /** 表名，可能是虚拟表 */
  private final String tableName;

  /** 复制的表名，如果是虚拟表，则表示从哪个表复制数据 */
  private final String copyFromTableName;

  /** 表描述 */
  private final String description;

  /** 查询需要的权限，null 表示不需要权限 */
  private final String permission;

  /** 是否有数据权限 */
  private final boolean isDataScope;

  /** 禁用的字段列表 */
  private final Set<String> disableFields;

  SQLBotTable(String tableName, String description, String permission, boolean isDataScope) {
    this(tableName, description, permission, isDataScope, null, Set.of());
  }

  SQLBotTable(
      String tableName,
      String description,
      String permission,
      boolean isDataScope,
      String copyFromTableName) {
    this(tableName, description, permission, isDataScope, copyFromTableName, Set.of());
  }

  SQLBotTable(
      String tableName,
      String description,
      String permission,
      boolean isDataScope,
      String copyFromTableName,
      Set<String> disableFields) {
    this.tableName = tableName;
    this.description = description;
    this.permission = permission;
    this.isDataScope = isDataScope;
    this.disableFields = disableFields;
    this.copyFromTableName = copyFromTableName;
  }

  public static Set<String> getDataScopePermissions() {
    return Arrays.stream(values())
        .filter(SQLBotTable::isDataScope)
        .map(SQLBotTable::getPermission)
        .collect(Collectors.toSet());
  }
}
