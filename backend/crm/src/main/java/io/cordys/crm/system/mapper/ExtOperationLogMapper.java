package io.cordys.crm.system.mapper;

import io.cordys.crm.system.dto.request.OperationLogRequest;
import io.cordys.crm.system.dto.response.OperationLogResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOperationLogMapper {

    List<OperationLogResponse> list(@Param("request") OperationLogRequest request);

    List<OperationLogResponse> loginList(@Param("request") OperationLogRequest request);
}
