package cn.cordys.crm.integration.agent.response;

import cn.cordys.common.dto.OptionDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MaxKBDataResponse extends MaxKBResponseEntity {

    @JsonProperty("data")
    List<OptionDTO> data;

}
