package cn.cordys.crm.system.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchPoolReasonRequest {

	@NotNull
	@Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> ids;

	@Schema(description = "原因")
	private String reasonId;
}
