package cn.cordys.crm.opportunity.mapper;

import cn.cordys.crm.opportunity.domain.OpportunityStageConfig;
import cn.cordys.crm.opportunity.dto.request.StageRollBackRequest;
import cn.cordys.crm.opportunity.dto.request.StageUpdateRequest;
import cn.cordys.crm.opportunity.dto.response.StageConfigResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtOpportunityStageConfigMapper {

  int countStageConfig(@Param("orgId") String orgId);

  void moveUpStageConfig(
      @Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

  void moveDownStageConfig(
      @Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

  List<StageConfigResponse> getStageConfigList(@Param("orgId") String orgId);

  void updateRollBack(@Param("request") StageRollBackRequest request, @Param("orgId") String orgId);

  void updateStageConfig(
      @Param("request") StageUpdateRequest request, @Param("userId") String userId);

  List<OpportunityStageConfig> getAllStageConfigList();

  void moveUp(
      @Param("start") Long start,
      @Param("end") Long end,
      @Param("orgId") String orgId,
      @Param("defaultPos") Long defaultPos);

  void moveDown(
      @Param("start") Long start,
      @Param("end") Long end,
      @Param("orgId") String orgId,
      @Param("defaultPos") Long defaultPos);

  void updatePos(@Param("id") String id, @Param("pos") Long pos);

  int countByType(@Param("type") String type, @Param("orgId") String orgId);
}
