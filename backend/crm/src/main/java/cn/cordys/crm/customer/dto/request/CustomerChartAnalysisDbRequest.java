package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.dto.ChartAnalysisDbRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class CustomerChartAnalysisDbRequest extends ChartAnalysisDbRequest {
    @Schema(description = "公海ID{公海客户列表时传参}")
    private String poolId;
}
