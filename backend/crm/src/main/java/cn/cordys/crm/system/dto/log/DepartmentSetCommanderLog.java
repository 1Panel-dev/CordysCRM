package cn.cordys.crm.system.dto.log;

import lombok.Builder;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
@Builder
public class DepartmentSetCommanderLog {
    /**
     * 部门负责人
     */
    private String commander;
}
