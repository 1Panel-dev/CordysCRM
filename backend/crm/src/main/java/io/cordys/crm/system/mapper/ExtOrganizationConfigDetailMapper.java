package io.cordys.crm.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.cordys.crm.system.domain.OrganizationConfigDetail;

public interface ExtOrganizationConfigDetailMapper {

    OrganizationConfigDetail getOrganizationConfigDetail(@Param("configId") String configId);

    List<OrganizationConfigDetail> getOrganizationConfigDetails(@Param("configId") String configId);
}
