package cn.cordys.crm.system.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationConfigRequest {

  @Schema(description = "详情ID")
  private String detailId;

  @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(
      message = "组织ID不能为空",
      groups = {Created.class, Updated.class})
  private String organizationId;

  @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(
      message = "类型不能为空",
      groups = {Created.class, Updated.class})
  private String type;

  @Schema(description = "详情类型", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(
      message = "详情类型不能为空",
      groups = {Created.class, Updated.class})
  private String detailType;

  @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(
      message = "内容不能为空",
      groups = {Created.class, Updated.class})
  private String content;
}
