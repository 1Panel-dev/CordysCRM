package cn.cordys.crm.system.constants;

import cn.cordys.common.exception.IResultCode;

/**
 * @author jianxing
 */
public enum SystemResultCode implements IResultCode {
  ROLE_EXIST(101001, "role.exist"),
  INTERNAL_ROLE_PERMISSION(101002, "internal.role.permission.error"),
  MODULE_ENABLE(101003, "module.has.closed");

  private final int code;
  private final String message;

  SystemResultCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return getTranslationMessage(this.message);
  }
}
