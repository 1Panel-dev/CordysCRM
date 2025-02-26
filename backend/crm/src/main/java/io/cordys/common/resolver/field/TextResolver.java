package io.cordys.common.resolver.field;

import io.cordys.crm.system.dto.field.base.BaseField;

/**
 * @author jainxing
 */
public class TextResolver extends AbstractModuleFieldResolver<BaseField> {

    @Override
    public void validate(BaseField baseField, Object value) {
        validateRequired(baseField, value);
        validateString(baseField.getName(), value);
    }

}
