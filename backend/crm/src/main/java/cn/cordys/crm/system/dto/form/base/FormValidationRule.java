package cn.cordys.crm.system.dto.form.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 表单校验规则
 */
@Data
public class FormValidationRule {

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "校验公式（由前端解析执行）")
    private String formula;

    @Schema(description = "不满足校验条件时的提示文案，必填")
    private String message;

    @Schema(description = "不满足校验条件后是否允许忽略并继续提交，默认 false（阻止提交）")
    private Boolean allowIgnore = false;
}
