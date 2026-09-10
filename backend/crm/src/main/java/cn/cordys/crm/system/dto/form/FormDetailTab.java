package cn.cordys.crm.system.dto.form;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 表单详情页关联数据标签配置。
 *
 * <p>关联表单和关联字段使用 {@link OptionDTO} 同时承载 ID 与名称：保存时以后端校验后的 ID 为准，
 * 查询时重新填充名称，避免表单或字段重命名后回显旧值。</p>
 */
@Data
public class FormDetailTab {

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "关联表单")
    private OptionDTO relatedForm;

    @Schema(description = "关联字段")
    private OptionDTO relatedField;

    @Schema(description = "是否启用")
    private Boolean enable;

    @Schema(description = "内置标签的稳定标识；非空表示内置标签，自定义标签为空")
    private String internalKey;
}
