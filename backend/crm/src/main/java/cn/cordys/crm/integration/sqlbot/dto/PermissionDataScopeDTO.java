package cn.cordys.crm.integration.sqlbot.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 部门的数据权限
 *
 * @author jianxing
 */
@Data
public class PermissionDataScopeDTO {
    /**
     * 是否可查看全部数据
     */
    private Boolean all = false;
    /**
     * 是否可查看自己的数据
     */
    private Boolean self = false;
    /**
     * 可查看的部门Id
     */
    private Set<String> deptIds = new HashSet<>();
}
