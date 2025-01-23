package io.cordys.crm.system.mapper;


import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserExtendMapper {
    void deleteUser(@Param("ids") List<String> ids);
}
