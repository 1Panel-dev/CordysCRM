package cn.cordys.crm.integration.agent.mapper;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.crm.integration.agent.dto.response.AgentPageResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtAgentCollectionMapper {

  void deleteByAgentId(@Param("agentId") String agentId);

  List<String> getByUserId(@Param("userId") String userId);

  int checkCollect(@Param("id") String id, @Param("userId") String userId);

  void unCollect(@Param("agentId") String agentId, @Param("userId") String userId);

  List<AgentPageResponse> collectList(
      @Param("request") BasePageRequest request,
      @Param("userId") String userId,
      @Param("orgId") String orgId);

  int countMyCollect(@Param("userId") String userId);
}
