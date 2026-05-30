package cn.cordys.crm.system.dto.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.List;

@Data
@JsonTypeName(value = "SERIAL_NUMBER")
@EqualsAndHashCode(callSuper = true)
public class SerialNumberField extends BaseField {

	public static final String FORMULA = "formula";

    @Schema(description = "流水号规则")
    private List<String> serialNumberRules;

	@Schema(description = "前缀固定字符类型", allowableValues = {"custom", "formula"})
	private String prefixType;

	@Schema(description = "公式, prefixType 为 formula")
	private String formula;

	/**
	 * 获取流水号规则 (替换可解析的公式前缀)
	 * @param formulaPrefix 公式前缀
	 * @return 流水号规则
	 */
	public List<String> getSerialNumberRules(String formulaPrefix) {
		if (Strings.CI.equals(this.prefixType, SerialNumberField.FORMULA) && StringUtils.isNotEmpty(formulaPrefix)) {
			this.serialNumberRules.set(0 ,formulaPrefix);
		}
		return this.serialNumberRules;
	}
}
