package cn.cordys.crm.customer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CustomerMergeRequest {

	@NotNull
	@Schema(description = "被合并的客户")
	private List<String> mergeIds;

	@NotEmpty
	@Schema(description = "合并的客户", requiredMode = Schema.RequiredMode.REQUIRED)
	private String toMergeId;

	@NotEmpty
	@Schema(description = "合并的负责人", requiredMode = Schema.RequiredMode.REQUIRED)
	private String ownerId;
}
