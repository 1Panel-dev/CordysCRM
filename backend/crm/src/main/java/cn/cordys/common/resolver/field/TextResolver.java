package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.base.BaseField;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jainxing
 */
public class TextResolver extends AbstractModuleFieldResolver<BaseField> {

    @Override
    public void validate(BaseField baseField, Object value) {
        validateRequired(baseField, value);
        validateString(baseField.getName(), value);
    }

	/**
	 * 要求文本长度不超过255个字符
	 * @param text 文本
	 * @return 符合要求的文本
	 */
	public String getCorrectInputString(String text) {
		if (StringUtils.isEmpty(text)) {
			return null;
		}
		return text.length() > 255 ? text.substring(0, 255) : text;
	}
}
