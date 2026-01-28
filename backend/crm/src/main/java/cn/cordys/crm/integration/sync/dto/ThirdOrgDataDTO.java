package cn.cordys.crm.integration.sync.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ThirdOrgDataDTO {
  private List<ThirdDepartment> departments;
  private Map<String, List<ThirdUser>> users;
}
