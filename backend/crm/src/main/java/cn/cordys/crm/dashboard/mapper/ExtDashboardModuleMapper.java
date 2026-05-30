package cn.cordys.crm.dashboard.mapper;

import cn.cordys.common.dto.BaseTree;
import cn.cordys.common.dto.NodeSortQueryParam;
import cn.cordys.crm.dashboard.domain.DashboardModule;
import cn.cordys.crm.dashboard.dto.DashboardTreeNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDashboardModuleMapper {


    int countByName(@Param("name") String name, @Param("parentId") String parentId, @Param("orgId") String orgId, @Param("id") String id);

    Long getNextPosByOrgId(@Param("orgId") String orgId);

    int countDefaultModuleByIds(@Param("ids") List<String> ids, @Param("orgId") String orgId);

    int countDashboardByIds(@Param("dashboardModuleIds") List<String> dashboardModuleIds, @Param("orgId") String orgId);

    List<DashboardTreeNode> selectTreeNode(@Param("orgId") String orgId);

    List<String> selectChildrenIds(@Param("parentId") String parentId);

    void batchUpdate(@Param("dashboardModules") List<DashboardModule> dashboardModules);

    BaseTree selectBaseModuleById(@Param("dragNodeId") String dragNodeId);

    BaseTree selectModuleByParentIdAndPosOperator(@Param("nodeSortQueryParam") NodeSortQueryParam nodeSortQueryParam);
}
