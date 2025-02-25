package io.cordys.crm.system.mapper;


import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.security.UserDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserMapper {

    UserDTO selectByPhoneOrEmail(String id);

    List<OptionDTO> selectUserOptionByIds(@Param("userIds") List<String> userIds);

    List<User> getOrgUserByUserIds(@Param("organizationId") String organizationId, @Param("userIds") List<String> userIds);

    List<UserRoleConvert> getUserRole(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);

    List<OptionDTO> selectUserOption(@Param("orgId") String orgId);

    UserResponse getUserDetail(@Param("id") String id);

    void batchUpdatePassword(@Param("userList") List<User> userList);

    void updateUserPassword(@Param("password") String password, @Param("id") String id);

    List<User> getAllUserIds(@Param("orgId") String orgId);

    void deleteByIds(@Param("ids") List<String> ids);

    int countByEmail(@Param("email") String email, @Param("id") String id);

    int countByPhone(@Param("phone") String phone, @Param("id") String id);

    List<OptionDTO> selectUserOptionByOrgId(@Param("orgId") String orgId);

    /**
     * 查询范围下的用户ID集合
     *
     * @param scopeIds 范围ID集合
     * @param orgId    组织ID
     * @return 用户ID集合
     */
    List<String> getUserIdsByScope(@Param("ids") List<String> scopeIds, @Param("orgId") String orgId);

    void updateUser(@Param("user") User user);
}
