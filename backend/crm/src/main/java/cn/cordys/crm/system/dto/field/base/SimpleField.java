package cn.cordys.crm.system.dto.field.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Mcp表单字段描述信息
 */
@Data
public class SimpleField {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "业务字段名")
    private String businessKey;

    @Schema(description = "选项枚举值")
    private List<OptionProp> options;

    @Schema(description = "是否必填")
    private boolean required;

    @Schema(description = "数据源类型")
    private String dataSourceType;
}
