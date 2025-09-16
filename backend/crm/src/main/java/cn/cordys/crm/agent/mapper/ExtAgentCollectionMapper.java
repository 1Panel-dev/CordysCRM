package cn.cordys.crm.agent.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtAgentCollectionMapper {

    void deleteByAgentId(@Param("agentId") String agentId);

    List<String> getByUserId(@Param("userId") String userId);
}
