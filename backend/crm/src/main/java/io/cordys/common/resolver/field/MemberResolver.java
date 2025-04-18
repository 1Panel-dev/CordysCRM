package io.cordys.common.resolver.field;


import io.cordys.crm.system.dto.field.MemberField;
import org.apache.commons.lang3.BooleanUtils;

/**
 * @author jianxing
 */
public class MemberResolver extends AbstractModuleFieldResolver<MemberField> {

    @Override
    public void validate(MemberField memberField, Object value) {
        if (BooleanUtils.isTrue(memberField.getMultiple())) {
            ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_MEMBER)
                    .validate(memberField, value);
            return;
        }
        validateRequired(memberField, value);
        validateString(memberField.getName(), value);
    }

    @Override
    public String parse2String(MemberField memberField, Object value) {
        if (BooleanUtils.isTrue(memberField.getMultiple())) {
            return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_MEMBER)
                    .parse2String(memberField, value);
        }
        return getStringValue(value);
    }

    @Override
    public Object parse2Value(MemberField memberField, String value) {
        if (BooleanUtils.isTrue(memberField.getMultiple())) {
            return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_MEMBER)
                    .parse2Value(memberField, value);
        }
        return getStringValue(value);
    }
}
