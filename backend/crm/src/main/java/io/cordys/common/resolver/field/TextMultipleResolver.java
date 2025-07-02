package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.InputMultipleField;
import org.apache.commons.lang3.StringUtils;

public class TextMultipleResolver extends AbstractModuleFieldResolver<InputMultipleField> {

    @Override
    public void validate(InputMultipleField inputMultipleField, Object value) {
        validateRequired(inputMultipleField, value);
        validateArray(inputMultipleField.getName(), value);
    }

    @Override
    public String parse2String(InputMultipleField inputMultipleField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(InputMultipleField inputMultipleField, String value) {
        return parse2Array(value);
    }


    @Override
	public Object trans2Value(InputMultipleField inputMultipleField, String value) {
        if (StringUtils.isEmpty(value)) {
            return StringUtils.EMPTY;
        }
        // replace [ and ]
        return value.substring(1, value.length() - 1);
    }
}
