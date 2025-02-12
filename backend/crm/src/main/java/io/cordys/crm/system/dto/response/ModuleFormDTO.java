package io.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleFormDTO {

	@NonNull
	@Schema(description = "前台缓存", defaultValue = "false")
	private Boolean frontCache;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "布局(常量)")
	private String layout;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "标题位置(常量)")
	private String labelPos;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "标题宽度")
	private String labelWidth;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "标题对齐方式")
	private String labelAlignment;

	@NonNull
	@Schema(description = "是否展示描述信息", defaultValue = "true")
	private Boolean showDesc;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "输入框宽度")
	private String inputWidth;

	@Size(max = 10)
	@NotBlank
	@Schema(description = "操作按钮位置(常量)")
	private String optBtnPos;

	@NonNull
	@Schema(description = "保存按钮", defaultValue = "true")
	private Boolean saveBtn;

	@NonNull
	@Schema(description = "保存并继续", defaultValue = "true")
	private Boolean saveContinueBtn;

	@NonNull
	@Schema(description = "取消按钮", defaultValue = "true")
	private Boolean cancelBtn;
}
