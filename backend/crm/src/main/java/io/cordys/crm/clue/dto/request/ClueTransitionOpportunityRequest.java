package io.cordys.crm.clue.dto.request;

import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class ClueTransitionOpportunityRequest extends OpportunityAddRequest {

	@Schema(description = "线索ID")
	private String clueId;
}
