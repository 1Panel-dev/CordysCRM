package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.MemberField;

/**
 * @author jianxing
 */
public class MultipleMemberResolver extends MemberResolver {

    @Override
    public void validate(MemberField memberField, Object value) {
        validateRequired(memberField, value);
        validateArray(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberField memberField, Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(MemberField memberField, String value) {
        return parse2Array(value);
    }
}
