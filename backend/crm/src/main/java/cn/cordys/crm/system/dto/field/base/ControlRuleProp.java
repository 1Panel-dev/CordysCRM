package cn.cordys.crm.system.dto.field.base;

import java.util.List;
import lombok.Data;

@Data
public class ControlRuleProp {

  /** 选项值ID */
  private String value;

  /** 字段显示ID集合 */
  private List<String> fieldIds;
}
