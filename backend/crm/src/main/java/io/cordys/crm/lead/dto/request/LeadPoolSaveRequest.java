package io.cordys.crm.lead.dto.request;

import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.domain.LeadPoolPickRule;
import io.cordys.crm.lead.domain.LeadPoolRecycleRule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LeadPoolSaveRequest extends LeadPool {

	@Schema(description = "领取规则")
	private LeadPoolPickRule pickRule;

	@Schema(description = "回收规则")
	private LeadPoolRecycleRule recycleRule;
}
