package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.DateTimeField;

/**
 * @author jianxing
 */
public class DateTimeResolver extends AbstractModuleFieldResolver<DateTimeField> {

    @Override
    public void validate(DateTimeField dateTimeField, Object value) {
        validateRequired(dateTimeField, value);

        validateLong(dateTimeField.getName(), value);
    }

    protected void validateLong(String name, Object value) {
        if (value != null && !(value instanceof Long)) {
            throwValidateException(name);
        }
    }

    @Override
    public Object parse2Value(DateTimeField dateTimeField, String value) {
        return parse2Long(value);
    }
}
