package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.contract.dto.request.BusinessTitlePageRequest;
import cn.cordys.crm.contract.dto.response.BusinessTitleListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtBusinessTitleMapper {


    List<BusinessTitleListResponse> list(@Param("request") BusinessTitlePageRequest request, @Param("orgId") String orgId, @Param("userId") String userId);
}
