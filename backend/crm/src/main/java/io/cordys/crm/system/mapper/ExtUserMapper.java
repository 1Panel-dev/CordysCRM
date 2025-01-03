package io.cordys.crm.system.mapper;


import io.cordys.aspectj.dto.OptionDTO;
import io.cordys.security.UserDTO;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExtUserMapper {
    UserDTO selectById(String id);

    List<OptionDTO> selectUserOptionByIds(@Param("userIds") List<String> userIds);
}
