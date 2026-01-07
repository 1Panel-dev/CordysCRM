package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContactInfo {

    /**
     * 注册电话
     */
    @JsonProperty("Tel")
    private String tel;
}
