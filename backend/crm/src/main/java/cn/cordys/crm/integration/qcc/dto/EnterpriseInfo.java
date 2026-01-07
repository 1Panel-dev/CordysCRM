package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnterpriseInfo {

    /**
     * 数据是否存在（1-存在，0-不存在）
     */
    @JsonProperty("VerifyResult")
    private Integer verifyResult;


    /**
     * 数据信息
     */
    @JsonProperty("Data")
    private InfoData data;
}
