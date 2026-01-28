package cn.cordys.crm.integration.dingtalk.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class SubDeptIdListResult {
  @JsonProperty("dept_id_list")
  private List<Long> deptIdList;
}
