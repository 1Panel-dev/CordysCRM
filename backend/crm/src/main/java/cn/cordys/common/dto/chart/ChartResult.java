package cn.cordys.common.dto.chart;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class ChartResult {
    private String categoryAxis;
    private String categoryAxisName;
    private String subCategoryAxis;
    private String subCategoryAxisName;
    private Object valueAxis;
}
