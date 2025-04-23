package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.MemberField;

/**
 * @author jianxing
 */
public class MemberResolver extends AbstractModuleFieldResolver<MemberField> {

    @Override
    public void validate(MemberField memberField, Object value) {
        validateRequired(memberField, value);
        validateString(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberField memberField, Object value) {
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(MemberField memberField, String value) {
        return getStringValue(value);
    }
}
