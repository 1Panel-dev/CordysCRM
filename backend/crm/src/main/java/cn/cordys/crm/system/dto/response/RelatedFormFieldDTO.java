package cn.cordys.crm.system.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关联表单中指向当前表单的数据源字段。
 *
 * <p>与通用选项对象相比，该对象保留了字段的稳定业务标识和数据源类型，
 * 调用方可以据此区分系统字段、自定义字段以及字段实际指向的数据类型。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatedFormFieldDTO {

    @Schema(description = "字段 ID")
    private String id;

    @Schema(description = "字段名称")
    private String name;

    @Schema(description = "字段内置 Key；自定义字段为空")
    private String internalKey;

    @Schema(description = "字段业务 Key；没有业务映射的自定义字段为空")
    private String businessKey;

    @Schema(description = "字段的数据源类型")
    private String sourceType;
}
