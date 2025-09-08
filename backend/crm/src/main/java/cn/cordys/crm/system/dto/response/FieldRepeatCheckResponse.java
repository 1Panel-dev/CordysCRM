package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldRepeatCheckResponse {

	@Schema(description = "是否重复")
	private boolean repeat;
	@Schema(description = "重复名称")
	private String name;
}
