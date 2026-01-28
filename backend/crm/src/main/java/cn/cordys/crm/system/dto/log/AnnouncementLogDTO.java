package cn.cordys.crm.system.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
public class AnnouncementLogDTO {

  @Schema(description = "公告标题")
  private String subject;

  @Schema(description = "公告内容")
  private String content;

  @Schema(description = "开始时间")
  private String startTime;

  @Schema(description = "结束时间")
  private String endTime;

  @Schema(description = "链接")
  private String url;

  @Schema(description = "重命名链接")
  private String renameUrl;

  @Schema(description = "接收者")
  private List<String> receiver;
}
