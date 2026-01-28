package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 用户关联角色
 *
 * @author jianxing
 * @date 2025-01-06 14:31:33
 */
@Data
@Table(name = "sys_user_role")
public class UserRole extends BaseModel {

  @Schema(description = "角色id")
  private String roleId;

  @Schema(description = "用户id")
  private String userId;
}
