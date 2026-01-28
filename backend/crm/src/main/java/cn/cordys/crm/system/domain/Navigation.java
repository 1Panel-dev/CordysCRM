package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_navigation")
public class Navigation extends BaseModel {

  @Schema(description = "组织id")
  private String organizationId;

  @Schema(description = "导航栏key")
  private String navigationKey;

  @Schema(description = "启用/禁用")
  private Boolean enable;

  @Schema(description = "自定义排序")
  private Long pos;
}
