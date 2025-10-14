package cn.cordys.crm.opportunity.mapper;

import cn.cordys.crm.opportunity.domain.OpportunityStageConfig;
import cn.cordys.crm.opportunity.dto.request.StageRollBackRequest;
import cn.cordys.crm.opportunity.dto.request.StageUpdateRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtOpportunityStageConfigMapper {


    int countStageConfig(@Param("orgId") String orgId);


    void moveUpStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    void moveDownStageConfig(@Param("start") Long start, @Param("orgId") String orgId, @Param("pos") Long pos);

    List<OpportunityStageConfig> getStageConfigList(@Param("orgId") String orgId);

    void updateRollBack(@Param("request") StageRollBackRequest request, @Param("orgId") String orgId);

    void updateStageConfig(@Param("request") StageUpdateRequest request, @Param("userId") String userId);

    List<OpportunityStageConfig> getAllStageConfigList();
}
