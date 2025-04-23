package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.InputMultipleField;

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
}
