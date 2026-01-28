package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class Properties {

  @JsonProperty("api_input_field_list")
  private List<ApiInputFieldList> apiInputFieldList;
}
