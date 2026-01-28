package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class WorkFlow {

  @JsonProperty("nodes")
  private List<Nodes> nodes;
}
