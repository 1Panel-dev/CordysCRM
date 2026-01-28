package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtBusinessTitleMapper {

  List<BusinessTitleListResponse> list(
      @Param("request") BusinessTitlePageRequest request,
      @Param("orgId") String orgId,
      @Param("userId") String userId);

  List<BusinessTitleListResponse> getListByIds(
      @Param("ids") List<String> ids, @Param("userId") String userId, @Param("orgId") String orgId);

  int countByName(
      @Param("businessName") String businessName,
      @Param("orgId") String orgId,
      @Param("id") String id);
}
