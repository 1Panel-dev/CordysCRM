package cn.cordys.crm.clue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoolCluePickRequest {

	@NotBlank
	@Schema(description = "线索ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String clueId;

	@NotBlank
	@Schema(description = "线索池ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String poolId;

}
