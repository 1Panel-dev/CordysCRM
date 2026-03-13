package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.text.DecimalFormat;

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

    @Schema(description = "格式", allowableValues = {"percent", "number", "currency"})
    private String numberFormat;

    @Schema(description = "保留小数点位数")
    private Boolean decimalPlaces;

    @Schema(description = "位数")
    private int precision;

    @Schema(description = "显示千分位")
    private Boolean showThousandsSeparator;

	public static String formatThousands(BigDecimal num) {
		if (num == null) {
			return null;
		}
		String plain = num.toPlainString();
		int dotIndex = plain.indexOf('.');
		String integerPart;
		String decimalPart = "";
		if (dotIndex >= 0) {
			integerPart = plain.substring(0, dotIndex);
			decimalPart = plain.substring(dotIndex);
		} else {
			integerPart = plain;
		}
		DecimalFormat df = new DecimalFormat("#,##0");
		return df.format(new BigDecimal(integerPart)) + decimalPart;
	}
}
