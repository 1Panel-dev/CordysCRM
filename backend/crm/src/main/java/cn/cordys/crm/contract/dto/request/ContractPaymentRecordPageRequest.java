package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ContractPaymentRecordPageRequest extends BasePageRequest {

	@Schema(description = "合同ID")
	private String contractId;

	@Schema(description = "客户ID")
	private String customerId;
}
