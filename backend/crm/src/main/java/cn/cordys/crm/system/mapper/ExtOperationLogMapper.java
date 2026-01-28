package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.OperationLogRequest;
import cn.cordys.crm.system.dto.response.OperationLogResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtOperationLogMapper {

  List<OperationLogResponse> list(
      @Param("request") OperationLogRequest request, @Param("orgId") String orgId);
}
