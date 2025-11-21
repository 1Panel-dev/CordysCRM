package cn.cordys.crm.contract.dto.response;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Data
public class ContractPaymentPlanListResponse extends ContractPaymentPlan {
    @Schema(description = "创建人名称")
    private String createUserName;

    @Schema(description = "更新人名称")
    private String updateUserName;
}
