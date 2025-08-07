package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.PhoneField;
import org.apache.commons.lang3.StringUtils;

public class PhoneResolver extends AbstractModuleFieldResolver<PhoneField> {

	@Override
	public void validate(PhoneField field, Object value) {
		validateRequired(field, value);
		validateString(field.getName(), value);
	}

	@Override
	public Object text2Value(PhoneField field, String text) {
		if (StringUtils.isEmpty(text)) {
			return text;
		}
		return text.replaceAll("（", "(").replaceAll("）", ")").replaceAll("\\s+", "");
	}
}
