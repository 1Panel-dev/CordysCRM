package cn.cordys.crm.system.dto.request;

import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentCommanderRequest {

  @NotBlank(
      message = "{department.id.not_blank}",
      groups = {Created.class, Updated.class})
  @Schema(description = "部门id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String departmentId;

  @NotBlank(message = "{department.commander.id.not_blank}")
  @Schema(description = "责任人id", requiredMode = Schema.RequiredMode.REQUIRED)
  private String commanderId;
}
