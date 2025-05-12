package io.cordys.crm.system.dto.log;

import io.cordys.crm.system.domain.Role;
import lombok.Data;

import java.util.List;

@Data
public class RoleLogDTO extends Role {

    /**
     * 指定部门列表
     */
    private List<String> deptIds;

    /**
     * 权限列表
     */
    private List<String> permissions;
}
