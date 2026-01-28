package cn.cordys.crm.integration.qcc.dto;

import cn.cordys.crm.integration.qcc.response.QccBaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class QccFuzzyQueryInfo extends QccBaseResponse {

  @JsonProperty("Result")
  private List<NameInfo> result;

  @JsonProperty("Paging")
  private Paging paging;
}
