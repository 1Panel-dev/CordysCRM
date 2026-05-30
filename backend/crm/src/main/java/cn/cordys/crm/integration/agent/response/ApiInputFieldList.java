package cn.cordys.crm.integration.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiInputFieldList {

    @JsonProperty("default_value")
    private String defaultValue;

    @JsonProperty("variable")
    private String variable;
}
