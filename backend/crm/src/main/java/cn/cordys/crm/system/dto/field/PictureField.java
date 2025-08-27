package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "PICTURE")
@EqualsAndHashCode(callSuper = true)
public class PictureField extends BaseField {

	@Schema(description = "图片显示", allowableValues = {"card", "list"})
	private String pictureShowType;
	@Schema(description = "图片上传数量限制")
	private Boolean uploadLimitEnable;
	@Schema(description = "数量")
	private Long uploadLimit;
	@Schema(description = "设置单个图片大小限制")
	private Boolean uploadSizeLimitEnable;
	@Schema(description = "单个图片大小")
	private Long uploadSizeLimit;
}
