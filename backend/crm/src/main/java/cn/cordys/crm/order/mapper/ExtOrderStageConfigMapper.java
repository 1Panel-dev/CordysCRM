package cn.cordys.crm.order.mapper;

import cn.cordys.crm.opportunity.dto.request.StageRollBackRequest;
import cn.cordys.crm.order.dto.request.OrderStageUpdateRequest;
import cn.cordys.crm.order.dto.response.OrderStageConfigResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOrderStageConfigMapper {


    List<OrderStageConfigResponse> getStageConfigList(@Param("orgId") String orgId);

    int countStageConfig(@Param("orgId") String orgId);

    void moveUpStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    void moveDownStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    void updateRollBack(@Param("request") StageRollBackRequest request, @Param("orgId") String orgId);

    void updateStageConfig(@Param("request") OrderStageUpdateRequest request, @Param("userId") String userId);

    void updatePos(@Param("id") String id, @Param("pos") Long pos);
}
