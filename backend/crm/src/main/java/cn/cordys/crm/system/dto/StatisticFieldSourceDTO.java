package cn.cordys.crm.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统计字段的宿主来源: 一个统计字段 + 它挂在哪个表单上。
 *
 * <p>「统计谁」这件事只存在于字段属性 JSON({@code sys_module_field_blob.prop}) 里, 没有独立的列,
 * 所以查询只能把属性原样捞回来, 到 Java 里再判断目标表单。</p>
 */
@Data
public class StatisticFieldSourceDTO {

    @Schema(description = "宿主表单Key")
    private String hostFormKey;

    @Schema(description = "统计字段ID")
    private String fieldId;

    @Schema(description = "统计字段属性(完整字段JSON)")
    private String prop;
}
