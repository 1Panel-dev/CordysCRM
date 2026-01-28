package cn.cordys.crm.system.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name = "sys_department")
public class Department extends BaseModel {

  @Schema(description = "名称")
  private String name;

  @Schema(description = "组织id")
  private String organizationId;

  @Schema(description = "父级")
  private String parentId;

  @Schema(description = "排序")
  private Long pos;

  @Schema(description = "来源")
  private String resource;

  @Schema(description = "来源id")
  private String resourceId;
}
