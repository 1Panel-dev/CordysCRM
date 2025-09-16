package cn.cordys.crm.integration.agent.mapper;


import cn.cordys.common.dto.NodeSortQueryParam;
import cn.cordys.crm.dashboard.dto.DropNode;
import cn.cordys.crm.integration.agent.dto.AgentTreeNode;
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

    DropNode selectDragInfoById(@Param("dragNodeId") String dragNodeId);

    DropNode selectNodeByPosOperator(@Param("nodeSortQueryParam") NodeSortQueryParam nodeSortQueryParam);

    void updatePos(@Param("id") String id, @Param("pos") long pos);

    List<String> selectIdByOrgIdOrderByPos(@Param("orgId") String orgId);

    List<AgentTreeNode> selectAgentNode(@Param("departmentIds") List<String> departmentIds, @Param("orgId") String orgId, @Param("userId") String userId);
}
