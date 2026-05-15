package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSimple {

	@Schema(description = "用户ID")
	private String id;
	@Schema(description = "用户名")
	private String name;
	@Schema(description = "头像")
	private String avatar;
}
