package io.cordys.crm.system.dto.field;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "PICTURE")
@EqualsAndHashCode(callSuper = true)
public class PictureField extends BaseField {

	@Schema(description = "图片显示")
	private String layout;
	@Schema(description = "图片上传数量限制")
	private Boolean limitNumber;
	@Schema(description = "数量")
	private Long number;
	@Schema(description = "设置单个图片大小限制")
	private Boolean singleSizeLimit;
	@Schema(description = "单个图片大小")
	private Long size;
}
