package io.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import io.cordys.crm.system.domain.OrganizationConfig;

@Mapper
public interface ExtOrganizationConfigMapper {
    OrganizationConfig getOrganizationConfig(@Param("organizationId") String organizationId, @Param("type") String type);   
}
