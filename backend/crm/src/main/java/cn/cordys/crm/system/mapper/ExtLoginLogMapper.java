package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.request.LoginLogRequest;
import cn.cordys.crm.system.dto.response.LoginLogListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtLoginLogMapper {
    List<LoginLogListResponse> list(@Param("request") LoginLogRequest request, @Param("orgId") String orgId);
}
