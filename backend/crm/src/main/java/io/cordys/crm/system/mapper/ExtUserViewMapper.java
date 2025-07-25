package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

public interface ExtUserViewMapper {
    Long getNextPos(@Param("orgId") String orgId, @Param("userId") String userId, @Param("resourceType") String resourceType);
}
