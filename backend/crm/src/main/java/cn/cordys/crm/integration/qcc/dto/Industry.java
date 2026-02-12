package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Industry {

    /**
     * 行业大类描述
     */
    @JsonProperty("SubIndustry")
    private String subIndustry;
}
