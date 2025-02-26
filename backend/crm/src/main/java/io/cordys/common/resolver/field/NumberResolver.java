package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.InputNumberField;

/**
 * @author jianxing
 */
public class NumberResolver extends AbstractModuleFieldResolver<InputNumberField> {
    @Override
    public void validate(InputNumberField numberField, Object value) {
        validateRequired(numberField, value);

        if (value != null && !(value instanceof Number)) {
            throwValidateException(numberField.getName());
        }
    }

    @Override
    public Object parse2Value(InputNumberField numberField, String value) {
        return value == null ? null : Double.parseDouble(value);
    }
}
