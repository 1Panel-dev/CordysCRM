package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.convert.UserRoleConvert;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.security.UserDTO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtUserMapper {

  UserDTO selectByPhoneOrEmail(String id);

  List<OptionDTO> selectUserOptionByIds(@Param("userIds") List<String> userIds);

  List<String> selectUserNameByIds(@Param("userIds") List<String> userIds);

  List<String> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

  List<String> selectUserIdsByNames(@Param("names") List<String> names);

  List<User> getOrgUserByUserIds(
      @Param("organizationId") String organizationId, @Param("userIds") List<String> userIds);

  List<UserRoleConvert> getUserRole(
      @Param("userIds") List<String> userIds, @Param("orgId") String orgId);

  List<OptionDTO> selectUserOption(@Param("orgId") String orgId);

  UserResponse getUserDetail(@Param("id") String id);

  List<UserResponse> getUserDetailList(@Param("userIds") List<String> userIds);

  void batchUpdatePassword(@Param("userList") List<User> userList);

  void updateUserPassword(@Param("password") String password, @Param("id") String id);

  List<User> getAllUserIds(@Param("orgId") String orgId);

  void deleteByIds(@Param("ids") List<String> ids);

  int countByEmail(@Param("email") String email, @Param("id") String id);

  int countByPhone(@Param("phone") String phone, @Param("id") String id);

  List<OptionDTO> selectUserOptionByOrgId(
      @Param("orgId") String orgId, @Param("defaultOrder") String defaultOrder);

  List<UserDTO> selectNameAndEmail(@Param("orgId") String orgId);

  void updateUser(@Param("user") User user);

  List<UserDeptDTO> getUserDeptByUserIds(
      @Param("userIds") List<String> userIds, @Param("orgId") String orgId);

  void updateUserInfo(@Param("user") OrganizationUser user);

  List<String> getOrgUserResourceIds(
      @Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
