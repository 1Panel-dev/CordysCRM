package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.request.AuthSourceRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtOrganizationConfigDetailMapper {

  OrganizationConfigDetail getOrganizationConfigDetail(@Param("configId") String configId);

  OrganizationConfigDetail getOrgConfigDetailByIdAndType(
      @Param("configId") String configId, @Param("type") String type);

  List<OrganizationConfigDetail> getOrganizationConfigDetails(
      @Param("configId") String configId, @Param("name") String name);

  List<OrganizationConfigDetail> getOrganizationConfigDetailList(
      @Param("request") AuthSourceRequest request);

  int getOrganizationConfigDetailCount(
      @Param("configId") String configId, @Param("name") String name, @Param("type") String type);

  List<OrganizationConfigDetail> getOrgConfigDetailByType(
      @Param("configId") String configId,
      @Param("name") String name,
      @Param("types") List<String> types);

  int getRepeatDetails(@Param("id") String id, @Param("name") String name);

  int updateStatus(
      @Param("enable") Boolean enable,
      @Param("type") String type,
      @Param("configId") String configId);

  List<OrganizationConfigDetail> getEnableOrganizationConfigDetails(
      @Param("configId") String configId, @Param("types") List<String> types);
}
