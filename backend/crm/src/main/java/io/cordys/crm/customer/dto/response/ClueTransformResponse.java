package io.cordys.crm.customer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClueTransformResponse {

	@Schema(description = "目标ID")
	private String transformId;
}
