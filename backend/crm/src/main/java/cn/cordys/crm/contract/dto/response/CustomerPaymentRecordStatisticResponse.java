package cn.cordys.crm.contract.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author song-cc-rock
 */
@Data
public class CustomerPaymentRecordStatisticResponse {

    @Schema(description = "应回款")
    private BigDecimal totalAmount;
	@Schema(description = "已回款")
	private BigDecimal receivedAmount;
	@Schema(description = "待回款")
	private BigDecimal pendingAmount;
}
