package io.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScopeNameDTO {

	@Schema(description = "ID")
	private String id;
	@Schema(description = "范围")
	private String scope;
	@Schema(description = "名称")
	private String name;
}
