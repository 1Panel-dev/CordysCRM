package cn.cordys.crm.integration.lark.response;

import cn.cordys.crm.integration.lark.dto.LarkDepartmentData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LarkDepartmentResponse extends LarkResponseEntity {

  @JsonProperty("data")
  private LarkDepartmentData data;
}
