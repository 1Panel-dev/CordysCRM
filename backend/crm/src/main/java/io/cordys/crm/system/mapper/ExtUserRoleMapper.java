package io.cordys.crm.system.mapper;


import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserRoleMapper {

    void deleteUserRole(@Param("ids") List<String> ids);
}
