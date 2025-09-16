package cn.cordys.crm.integration.agent.mapper;

import cn.cordys.common.dto.BaseTree;
import cn.cordys.common.dto.NodeSortQueryParam;
import cn.cordys.crm.integration.agent.dto.AgentTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtAgentModuleMapper {

    int countByName(@Param("name") String name, @Param("parentId") String parentId, @Param("orgId") String orgId, @Param("id") String id);

    Long getNextPosByOrgId(@Param("orgId") String orgId);

    int countAgentByIds(@Param("agentModuleIds") List<String> agentModuleIds, @Param("orgId") String orgId);

    List<AgentTreeNode> selectTreeNode(@Param("orgId") String orgId);

    BaseTree selectBaseModuleById(@Param("dragNodeId") String dragNodeId);

    BaseTree selectModuleByParentIdAndPosOperator(@Param("nodeSortQueryParam") NodeSortQueryParam nodeSortQueryParam);

    List<String> selectChildrenIds(@Param("parentId") String parentId);

    void updatePos(@Param("id") String id, @Param("pos") long pos);
}
