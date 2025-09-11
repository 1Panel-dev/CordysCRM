package cn.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Mcp表单字段描述信息
 */
@Data
public class SimpleField {

	@Schema(description = "名称")
	private String name;

	@Schema(description = "选项枚举值")
	private List<String> options;

	@Schema(description = "是否必填")
	private boolean required;

	@Schema(description = "是否唯一")
	private boolean unique;
}
