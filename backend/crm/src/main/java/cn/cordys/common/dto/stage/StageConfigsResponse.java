package cn.cordys.common.dto.stage;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StageConfigsResponse {

    @Schema(description = "订单状态流配置列表")
    List<StageConfigResponse> stageConfigList;

    @Schema(description = "进行中回退设置")
    private Boolean afootRollBack = true;

    @Schema(description = "完结回退设置")
    private Boolean endRollBack = false;

    @Schema(description = "流转配置类型")
    private String circulationType;

    @Schema(description = "高级流转设置")
    private List<CirculationSetting> advancedConfigs;

    @Schema(description = "条件节点字段选项映射")
    private Map<String, List<OptionDTO>> optionMap;
}
