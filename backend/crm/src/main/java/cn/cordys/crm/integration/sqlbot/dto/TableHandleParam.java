package cn.cordys.crm.integration.sqlbot.dto;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.crm.integration.sqlbot.constant.SQLBotTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-05  15:04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TableHandleParam {
    private String userId;
    private String orgId;
    private SQLBotTable tableInfo;
    private DeptDataPermissionDTO dataPermission;
}
