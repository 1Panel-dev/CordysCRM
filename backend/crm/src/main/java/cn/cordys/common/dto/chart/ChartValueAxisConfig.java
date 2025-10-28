package cn.cordys.common.dto.chart;

import cn.cordys.common.constants.ChartAggregateMethod;
import cn.cordys.common.constants.EnumValue;
import cn.cordys.common.uid.utils.EnumUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:54
 */
@Data
public class ChartValueAxisConfig {
    @Schema(description = "字段ID")
    private String fieldId;

    @EnumValue(enumClass = ChartAggregateMethod.class)
    @Schema(description = "聚合方式")
    private String aggregateMethod;

    public String getAggregateMethod() {
        if (this.aggregateMethod == null) {
            return ChartAggregateMethod.COUNT.name();
        }
        // 避免mapper中sql注入
        return EnumUtils.valueOf(ChartAggregateMethod.class, this.aggregateMethod).name();
    }
}
