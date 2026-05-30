package cn.cordys.crm.integration.dataease.dto;

import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.crm.integration.dataease.DataEaseClient;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.crm.system.dto.response.RoleListResponse;
import cn.cordys.security.UserDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-21  18:21
 */
@Data
public class DeTempResourceDTO {

    private DataEaseClient dataEaseClient;
    /**
     * CRM组织ID
     */
    private String crmOrgId;
    /**
     * DE组织ID
     */
    private String deOrgId;
    /**
     * CRM角色
     */
    private List<RoleListResponse> crmRoles;
    /**
     * CRM用户
     */
    private List<UserDTO> crmUsers;
    /**
     * 记录DE用户ID
     */
    private List<String> deUserIds;
    /**
     * 记录角色和权限的映射关系
     */
    private Map<String, Set<String>> rolePermissionMap;
    /**
     * 记录角色ID和角色的映射关系
     */
    private Map<String, RoleListResponse> crmRoleMap;
    /**
     * 部门树
     */
    private List<BaseTreeNode> deptTree;
    /**
     * 用户下的角色信息
     */
    private Map<String, List<UserRole>> crmUserRoleMap;
    /**
     * 自定义部门角色对应的部门ID
     */
    private Map<String, List<String>> customDeptRoleDeptMap;
    /**
     * 记录变量名和变量的映射
     */
    private Map<String, SysVariableDTO> sysVariableMap;
    /**
     * 记录系统变量和对应的值,key 为变量ID，值为变量值的名字和ID映射
     */
    private Map<String, Map<String, String>> variableValueMap;
    /**
     * 记录角色名和角色的映射
     */
    private Map<String, RoleDTO> deRoleMap;
}
