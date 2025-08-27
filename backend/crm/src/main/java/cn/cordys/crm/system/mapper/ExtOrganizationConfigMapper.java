package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.OrganizationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExtOrganizationConfigMapper {
    OrganizationConfig getOrganizationConfig(@Param("organizationId") String organizationId, @Param("type") String type);

    void updateSyncFlag(@Param("orgId") String orgId, @Param("syncResource") String syncResource, @Param("type") String type);

    int getSyncFlag(@Param("organizationId") String organizationId);

    OrganizationConfig getSyncStatus(@Param("organizationId") String organizationId, @Param("type") String type, @Param("syncResource") String syncResource);
}
