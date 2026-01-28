package cn.cordys.crm.system.dto.log;

import cn.cordys.crm.system.domain.Role;
import java.util.List;
import lombok.Data;

@Data
public class RoleLogDTO extends Role {

  /** 指定部门列表 */
  private List<String> deptIds;

  /** 权限列表 */
  private List<String> permissions;
}
