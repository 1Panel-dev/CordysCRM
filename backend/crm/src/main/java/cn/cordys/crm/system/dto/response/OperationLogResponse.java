package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
public class OperationLogResponse implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "id")
  private String id;

  @Schema(description = "操作人")
  private String operator;

  @Schema(description = "操作人名称")
  private String operatorName;

  @Schema(description = "操作时间")
  private Long createTime;

  @Schema(description = "操作对象")
  private String module;

  @Schema(description = "操作类型")
  private String type;

  @Schema(description = "资源名称")
  private String resourceName;

  @Schema(description = "日志描述")
  private String detail;
}
