package cn.cordys.crm.dashboard.mapper;


import cn.cordys.common.dto.NodeSortQueryParam;
import cn.cordys.crm.dashboard.dto.DashboardTreeNode;
import cn.cordys.crm.dashboard.dto.DropNode;
import cn.cordys.crm.dashboard.dto.request.DashboardPageRequest;
import cn.cordys.crm.dashboard.dto.response.DashboardDetailResponse;
import cn.cordys.crm.dashboard.dto.response.DashboardPageResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDashboardMapper {

    int countByName(@Param("name") String name, @Param("dashboardModuleId") String dashboardModuleId, @Param("orgId") String orgId, @Param("id") String id);

    DashboardDetailResponse getDetail(@Param("id") String id);

    Long getNextPosByOrgId(@Param("orgId") String orgId);

    List<DashboardTreeNode> selectDashboardNode(@Param("departmentIds") List<String> departmentIds, @Param("orgId") String orgId, @Param("userId") String userId);

    List<DashboardPageResponse> list(@Param("request") DashboardPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId, @Param("departmentIds") List<String> departmentIds);

    DropNode selectDragInfoById(@Param("dragNodeId") String dragNodeId);

    DropNode selectNodeByPosOperator(@Param("nodeSortQueryParam") NodeSortQueryParam nodeSortQueryParam);

    void updatePos(@Param("id") String id, @Param("pos") long pos);

    List<String> selectIdByOrgIdOrderByPos(@Param("orgId") String orgId);
}
