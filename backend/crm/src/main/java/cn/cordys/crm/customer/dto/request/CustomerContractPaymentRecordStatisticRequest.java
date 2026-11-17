package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.dto.condition.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户详情回款记录统计请求。
 */
@Data
public class CustomerContractPaymentRecordStatisticRequest extends BaseCondition {

    @NotBlank
    @Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Schema(description = "合同ID")
    private String contractId;
}
