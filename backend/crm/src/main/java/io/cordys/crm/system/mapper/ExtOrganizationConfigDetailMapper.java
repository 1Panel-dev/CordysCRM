package io.cordys.crm.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.cordys.crm.system.domain.OrganizationConfigDetail;

public interface ExtOrganizationConfigDetailMapper {

    OrganizationConfigDetail getOrganizationConfigDetail(@Param("configId") String configId);

    List<OrganizationConfigDetail> getOrganizationConfigDetails(@Param("configId") String configId, @Param("name") String name);

    int getOrganizationConfigDetailCount(@Param("configId") String configId, @Param("name") String name, @Param("type") String type);

    List<OrganizationConfigDetail> getOrgConfigDetailByType(@Param("configId") String configId, @Param("name") String name, @Param("type") String type);

    int getRepeatDetails(@Param("id") String id, @Param("name") String name);

    int updateStatus(@Param("id") String id, @Param("enable") Boolean enable, @Param("type") String type, @Param("configId") String configId);

    List<OrganizationConfigDetail>  getEnableOrganizationConfigDetails(@Param("configId") String configId, @Param("type") String type);

}
