package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BtnContentProp {

	@Schema(description = "文本")
	private String text;
	@Schema(description = "是否开启")
	private Boolean enable;
}
