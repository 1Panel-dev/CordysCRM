package cn.cordys.crm.integration.qcc.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class QccBaseResponse {


    @JsonProperty("Status")
    private String status;

    @JsonProperty("Message")
    private String message;

}
