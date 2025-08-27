package cn.cordys.crm.integration.dataease.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-21  18:21
 */
@Data
public class UserVariableTempDTO {

    /**
     * 记录用户的数据权限变量值
     * key 参考 DataScopeVariable
     */
    private Map<String, String> scopeValueMap = new HashMap<>(5);
    /**
     * 记录用户的数据部门权限变量值
     * key 参考 DataScopeDeptVariable
     */
    private Map<String, List<String>> userDeptIdMap = new HashMap<>(0);
}
