package io.cordys.crm.system.mapper;

import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;
/**
 *
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
public interface ExtUserRoleMapper {

    List<RoleUserListResponse> list(@Param("request") RoleUserPageRequest request, @Param("orgId") String orgId);

    List<DeptUserTreeNode> selectUserDeptForRelevance(@Param("orgId") String orgId, @Param("roleId") String roleId);

    List<DeptUserTreeNode> selectUserRoleForRelevance(@Param("orgId") String orgId, @Param("roleId") String roleId);

    void deleteUserRole(@Param("ids") List<String> ids);

    List<String> getUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    void deleteByIds(@Param("ids") List<String> ids);
}
