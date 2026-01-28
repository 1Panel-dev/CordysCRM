package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NameInfo {

  @JsonProperty("Name")
  private String name;
}
