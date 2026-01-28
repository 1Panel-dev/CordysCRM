package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Data;

@Data
public class TimeDTO implements Serializable {
  @Schema(description = "时间值")
  private Integer timeValue;

  @Schema(description = "时间单位: MINUTE/HOUR/DAY/WEEK/MONTH")
  private String timeUnit;
}
