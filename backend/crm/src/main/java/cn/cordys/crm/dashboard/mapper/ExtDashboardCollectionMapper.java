package cn.cordys.crm.dashboard.mapper;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.crm.dashboard.dto.response.DashboardPageResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtDashboardCollectionMapper {

    List<DashboardPageResponse> collectList(@Param("request") BasePageRequest request, @Param("userId") String userId, @Param("orgId") String orgId);

    void deleteByDashboardId(@Param("dashboardId") String dashboardId);

    void unCollect(@Param("dashboardId") String dashboardId, @Param("userId") String userId);

    int countMyCollect(@Param("userId") String userId);

    List<String> getByUserId(@Param("userId") String userId);

    int checkCollect(@Param("id") String id, @Param("userId") String userId);
}
