package io.cordys.crm.system.mapper;

import io.cordys.crm.system.dto.request.LoginLogRequest;
import io.cordys.crm.system.dto.response.LoginLogListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtLoginLogMapper {
    List<LoginLogListResponse> list(@Param("request") LoginLogRequest request, @Param("orgId") String orgId);
}
