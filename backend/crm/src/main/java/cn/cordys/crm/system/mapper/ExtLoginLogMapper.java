package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.LoginLogRequest;
import cn.cordys.crm.system.dto.response.LoginLogListResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtLoginLogMapper {
  List<LoginLogListResponse> list(
      @Param("request") LoginLogRequest request, @Param("orgId") String orgId);
}
