package cn.cordys.crm.system.dto.log;

import lombok.Builder;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-01-22  14:55
 */
@Data
@Builder
public class DepartmentSetCommanderLog {
    /**
     * 部门负责人
     */
    private String commander;
}
