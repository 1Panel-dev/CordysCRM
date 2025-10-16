package cn.cordys.common.dto.chart;

import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-13  14:54
 */
@Data
public class ChartValueAxisDbParam extends ChartValueAxisConfig {
    /**
     * 是否要查blob表
     */
    private Boolean blob = false;
    /**
     * 是否是业务字段
     */
    private Boolean businessField = false;

    /**
     * 业务字段名称
     */
    private String businessFieldName;
}
