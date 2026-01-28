package cn.cordys.crm.integration.agent.response;

import cn.cordys.common.dto.OptionDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class MaxKBDataResponse extends MaxKBResponseEntity {

  @JsonProperty("data")
  List<OptionDTO> data;
}
