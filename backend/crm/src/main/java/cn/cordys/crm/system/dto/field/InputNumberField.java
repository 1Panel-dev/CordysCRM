package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonTypeName(value = "INPUT_NUMBER")
@EqualsAndHashCode(callSuper = true)
public class InputNumberField extends BaseField {

    @Schema(description = "默认值")
    private Double defaultValue;

    @Schema(description = "最小值范围")
    private Integer min;

    @Schema(description = "最大值范围")
    private Integer max;

    @Schema(description = "格式", allowableValues = {"percent", "number"})
    private String numberFormat;

    @Schema(description = "保留小数点位数")
    private Boolean decimalPlaces;

    @Schema(description = "位数")
    private int precision;

    @Schema(description = "显示千分位")
    private Boolean showThousandsSeparator;
}
