package cn.cordys.crm.integration.qcc.dto;

import cn.cordys.crm.integration.qcc.response.QccBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class QccEnterpriseInfo extends QccBaseResponse {

  @JsonProperty("Result")
  private EnterpriseInfo result;
}
