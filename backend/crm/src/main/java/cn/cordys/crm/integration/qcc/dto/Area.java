package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Area {

    /**
     * 省份
     */
    @JsonProperty("Province")
    private String province;

    /**
     * 城市
     */
    @JsonProperty("City")
    private String city;
}
