package cn.cordys.crm.integration.agent.mapper;


import cn.cordys.crm.integration.agent.dto.request.AgentPageRequest;
import cn.cordys.crm.integration.agent.dto.response.AgentDetailResponse;
import cn.cordys.crm.integration.agent.dto.response.AgentPageResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtAgentMapper {


    int countByName(@Param("name") String name, @Param("agentModuleId") String agentModuleId, @Param("orgId") String orgId, @Param("id") String id);

    Long getNextPosByOrgId(@Param("orgId") String orgId);

    AgentDetailResponse getDetail(@Param("id") String id);

    List<AgentPageResponse> list(@Param("request") AgentPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("departmentIds") List<String> departmentIds);
}
