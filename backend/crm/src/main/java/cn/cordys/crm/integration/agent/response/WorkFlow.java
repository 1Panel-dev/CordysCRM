package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WorkFlow {

    @JsonProperty("nodes")
    private List<Nodes> nodes;
}
