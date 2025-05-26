package io.cordys.crm.system.mapper;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.domain.OrganizationUser;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.UserBatchEditRequest;
import io.cordys.crm.system.dto.request.UserBatchEnableRequest;
import io.cordys.crm.system.dto.request.UserBatchRequest;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.response.UserImportDTO;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.security.UserDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-01-09 16:11:37
 */
public interface ExtOrganizationUserMapper {

    int countUserByDepartmentIds(@Param("departmentIds") List<String> departmentIds, @Param("orgId") String orgId);

    List<UserPageResponse> list(@Param("request") UserPageRequest request, @Param("orderByClause") String orderByClause);

    void enable(@Param("request") UserBatchEnableRequest request, @Param("operatorId") String operatorId, @Param("time") long time);

    List<User> getUserList(@Param("request") UserBatchRequest request);

    void deleteUserByOrgId(@Param("orgId") String orgId);

    UserDTO getEnableUser(@Param("resourceUserId") String resourceUserId);

    List<OptionDTO> selectEnableOrgUser(@Param("ids") List<String> ids, @Param("enable") boolean enable);

    void updateUserByIds(@Param("request") UserBatchEditRequest request, @Param("operatorId") String operatorId, @Param("orgId") String orgId);

    List<UserImportDTO> selectSupervisor(@Param("nameList") List<String> nameList, @Param("orgId") String orgId);

    List<OrganizationUser> getUserByOrgId(@Param("orgId") String orgId);

    void updateOrganizationUser(@Param("organizationUser") OrganizationUser organizationUser);

    void deleteUserByIds(@Param("ids") List<String> ids);

    List<OrganizationUser> selectUserByUserIds(@Param("userIds") List<String> userIds);

    List<UserResponse> getUserDepAndPhoneByUserIds(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);

    String getOrgUserIdByUserId(@Param("orgId") String orgId, @Param("userId") String userId);

    List<UserResponse> selectByIds(@Param("ids") List<String> ids);

    void updateById(@Param("organizationUser") OrganizationUser organizationUser);

    void disableUser(@Param("organizationUser") OrganizationUser organizationUser);

    void updateUserByUserId(@Param("userId") String userId, @Param("time") long time, @Param("operatorId") String operatorId);
}
