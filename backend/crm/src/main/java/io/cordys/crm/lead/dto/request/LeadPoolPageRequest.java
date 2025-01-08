package io.cordys.crm.lead.dto.request;

import io.cordys.common.groups.Created;
import io.cordys.common.groups.Updated;
import io.cordys.common.pager.condition.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeadPoolPageRequest extends BasePageRequest {

	@Size(min = 1, max = 32, message = "{organization_id.length_range}", groups = {Created.class, Updated.class})
	@NotBlank(message = "{organization_id.not_blank}", groups = {Created.class, Updated.class})
	@Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String organizationId;
}
