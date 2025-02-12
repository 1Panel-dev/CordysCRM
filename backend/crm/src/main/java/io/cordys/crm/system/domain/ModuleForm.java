package io.cordys.crm.system.domain;

import io.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_module_form")
public class ModuleForm extends BaseModel {

	@Schema(description = "表单Key")
	private String formKey;

	@Schema(description = "组织id")
	private String organizationId;

	@Schema(description = "前台缓存")
	private Boolean frontCache;

	@Schema(description = "布局(常量)")
	private String layout;

	@Schema(description = "标题位置(常量)")
	private String labelPos;

	@Schema(description = "标题宽度")
	private String labelWidth;

	@Schema(description = "标题对齐方式")
	private String labelAlignment;

	@Schema(description = "是否展示描述信息")
	private Boolean showDesc;

	@Schema(description = "输入框宽度")
	private String inputWidth;

	@Schema(description = "操作按钮位置(常量)")
	private String optBtnPos;

	@Schema(description = "保存按钮")
	private Boolean saveBtn;

	@Schema(description = "保存并继续")
	private Boolean saveContinueBtn;

	@Schema(description = "取消按钮")
	private Boolean cancelBtn;
}
