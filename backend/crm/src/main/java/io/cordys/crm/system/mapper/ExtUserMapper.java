package io.cordys.crm.system.mapper;


import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.security.UserDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExtUserMapper {
    UserDTO selectById(String id);

    List<OptionDTO> selectUserOptionByIds(@Param("userIds") List<String> userIds);

    List<UserRoleConvert> getUserRole(@Param("userIds") List<String> userIds, @Param("orgId") String orgId);
}
