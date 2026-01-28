package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class AnnouncementReceiveTypeDTO {
  @Schema(description = "部门ID")
  private List<String> deptIds;

  @Schema(description = "角色ID")
  private List<String> roleIds;

  @Schema(description = "用户ID")
  private List<String> userIds;
}
