package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApplicationDetail {

  @JsonProperty("work_flow")
  private WorkFlow workflow;
}
