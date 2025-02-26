package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.MultipleInputField;

public class MultipleTextResolver extends AbstractModuleFieldResolver<MultipleInputField> {

    @Override
    public void validate(MultipleInputField multipleInputField, Object value) {
        validateArrayRequired(multipleInputField, value);
        validateArray(multipleInputField.getName(), value);
    }

    @Override
    public String parse2String(MultipleInputField multipleInputField, Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(MultipleInputField multipleInputField, String value) {
        return parse2Array(value);
    }
}
