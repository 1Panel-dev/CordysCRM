package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerInvoiceStatisticResponse {
    @Schema(description = "合同金额")
    private BigDecimal contractAmount;
    @Schema(description = "未开票金额")
    private BigDecimal uninvoicedAmount;
    @Schema(description = "已开票金额")
    private BigDecimal invoicedAmount;
}
