package cn.cordys.crm.system.dto.response;

import cn.cordys.crm.system.domain.LoginLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginLogListResponse extends LoginLog {

  @Schema(description = "操作人名称")
  private String operatorName;
}
