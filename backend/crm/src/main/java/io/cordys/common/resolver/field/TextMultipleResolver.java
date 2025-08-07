package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.InputMultipleField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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
        return String.join(",", JSON.parseArray(value));
    }

    @Override
    public Object text2Value(InputMultipleField field, String text) {
        if (StringUtils.isBlank(text) || StringUtils.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }
        List<String> textList = parseFakeJsonArray(text);
        return CollectionUtils.isEmpty(textList) ? StringUtils.EMPTY : textList;
    }
}
