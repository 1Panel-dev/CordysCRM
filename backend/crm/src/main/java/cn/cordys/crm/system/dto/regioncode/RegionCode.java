package cn.cordys.crm.system.dto.regioncode;

import java.util.List;
import lombok.Data;

@Data
public class RegionCode {

  private String code;
  private String name;
  private List<RegionCode> children;
}
