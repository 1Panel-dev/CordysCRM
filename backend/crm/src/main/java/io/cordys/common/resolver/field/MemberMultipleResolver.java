package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.MemberMultipleField;

/**
 * @author jianxing
 */
public class MemberMultipleResolver extends AbstractModuleFieldResolver<MemberMultipleField> {

    @Override
    public void validate(MemberMultipleField memberField, Object value) {
        validateRequired(memberField, value);
        validateArray(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberMultipleField memberField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(MemberMultipleField memberField, String value) {
        return parse2Array(value);
    }
}
