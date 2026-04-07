package cn.cordys.common.dto.condition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 表示组合条件，用于支持复杂的过滤和查询逻辑。
 * 包含字段名、操作符和期望值等信息。
 */
@Data
public class FilterDBCondition extends FilterCondition {

    @Schema(description = "是否是自定义字段")
    private Boolean customField = false;

    @Schema(description = "是否是显示字段")
    private Boolean refFiled = false;

    @Schema(description = "显示字段的主字段是否是自定义字段")
    private Boolean refMainCustomField = false;

    @Schema(description = "显示字段的主字段名称或ID")
    private String refMainFieldName;

    @Schema(description = "显示字段的主表名")
    private String refMainTableName;
}
