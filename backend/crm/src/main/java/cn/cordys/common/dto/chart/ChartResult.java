package cn.cordys.common.dto.chart;

import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-10-15  11:40
 */
@Data
public class ChartResult {
    private String categoryAxis;
    private String categoryAxisName;
    private String subCategoryAxis;
    private String subCategoryAxisName;
    private Object valueAxis;
}
