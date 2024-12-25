package io.cordys.crm.system.mapper;


import io.cordys.security.UserDTO;

public interface ExtUserMapper {
    UserDTO selectById(String id);
}
