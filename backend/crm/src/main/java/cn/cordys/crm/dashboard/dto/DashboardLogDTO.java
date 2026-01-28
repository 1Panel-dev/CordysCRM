package cn.cordys.crm.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class DashboardLogDTO {

  @Schema(description = "名称")
  private String name;

  @Schema(description = "仪表板id")
  private String resourceId;

  @Schema(description = "文件夹名称")
  private String dashboardModuleName;

  @Schema(description = "可查看人员集合")
  private List<String> members;

  @Schema(description = "描述")
  private String description;
}
