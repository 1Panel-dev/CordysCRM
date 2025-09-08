package cn.cordys.crm.system.dto.form;

import cn.cordys.crm.system.dto.form.base.BtnContentProp;
import cn.cordys.crm.system.dto.form.base.LinkField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表单属性
 */
@Data
public class FormProp {

	@Schema(description = "布局")
	private int layout;
	@Schema(description = "标题位置")
	private String labelPos;
	@Schema(description = "输入框宽度")
	private String inputWidth;
	@Schema(description = "操作按钮位置")
	private String optBtnPos;
	@Schema(description = "操作按钮内容")
	private List<BtnContentProp> optBtnContent;
	@Schema(description = "联动配置")
	private Map<String, List<LinkField>> linkProp;
}
