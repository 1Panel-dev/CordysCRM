package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.dto.ChartAnalysisDbRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class ClueChartAnalysisDbRequest extends ChartAnalysisDbRequest {
    @Schema(description = "线索池ID{线索池线索列表时传参}")
    private String poolId;
}
