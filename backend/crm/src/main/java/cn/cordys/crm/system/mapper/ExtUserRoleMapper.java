package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.crm.system.dto.request.RoleUserPageRequest;
import cn.cordys.crm.system.dto.response.RoleUserListResponse;
import cn.cordys.crm.system.dto.response.RoleUserOptionResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-01-13 17:33:23
 */
public interface ExtUserRoleMapper {

    List<RoleUserListResponse> list(@Param("request") RoleUserPageRequest request, @Param("orgId") String orgId);

    List<DeptUserTreeNode> selectUserDeptForRelevance(@Param("orgId") String orgId, @Param("roleId") String roleId);

    List<DeptUserTreeNode> selectUserDeptForOrg(@Param("orgId") String orgId);

    List<RoleUserTreeNode> selectUserRoleForRelevance(@Param("orgId") String orgId, @Param("roleId") String roleId);

    void deleteUserRoleByUserId(@Param("userId") String userId);

    void deleteUserRoleByUserIds(@Param("userIds")List<String> userIds);

    List<String> getUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    void deleteByIds(@Param("ids") List<String> ids);

    List<RoleUserOptionResponse> selectUserOptionByRoleId(@Param("orgId") String orgId, @Param("roleId") String roleId);

    List<String> selectUserRole(@Param("userId") String userId, @Param("orgId") String orgId);

    List<String> selectPermissionsByUserId(@Param("userId") String userId);


}
