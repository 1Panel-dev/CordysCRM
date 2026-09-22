package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.crm.system.domain.StageAdvancedConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtStageAdvancedConfigMapper {


    void update(@Param("sourceTable") String sourceTable, @Param("circulationType") String circulationType, @Param("orgId") String orgId, @Param("userId") String userId, @Param("updateTime") Long updateTime);

    List<StageAdvancedConfig> selectConfigByType(@Param("orgId") String orgId, @Param("moduleType") String moduleType);

    StageConfigResponse getStageConfig(@Param("sourceTable")String sourceTable, @Param("id")String id);

    /**
     * 取阶段配置的赢率, 用于区分商机的「成功」(100) 与「失败」(0)。
     * 赢率列只有商机阶段表(opportunity_stage_config)才有, 只能对商机状态流调用。
     */
    String getStageRate(@Param("sourceTable") String sourceTable, @Param("id") String id);

    StageAdvancedConfig getConfigByOriginAndTarget(@Param("originId")String originId, @Param("targetId")String targetId,@Param("moduleType")String moduleType);
}
