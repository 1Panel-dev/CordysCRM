package cn.cordys.crm.customer.dto.request;

import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class CustomerOpportunityPageRequest extends OpportunityPageRequest {

	@NotBlank
	@Schema(description = "客户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String customerId;

	@Override
	public String getCustomerId() {
		return customerId;
	}
}
