package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.SelectField;
import org.apache.commons.lang3.BooleanUtils;

/**
 * @author jianxing
 */
public class SelectResolver extends AbstractModuleFieldResolver<SelectField> {

    @Override
    public void validate(SelectField selectField, Object value) {
        if (BooleanUtils.isTrue(selectField.getMultiple())) {
            ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_SELECT).validate(selectField, value);
            return;
        }

        // 校验必填
        validateRequired(selectField, value);

        // 校验值类型
        validateString(selectField.getName(), value);

        // 校验选项正确
        validateOptions(selectField.getName(), value, selectField.getOptions());
    }

    @Override
    public String parse2String(SelectField selectField, Object value) {
        if (BooleanUtils.isTrue(selectField.getMultiple())) {
            return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_SELECT)
                    .parse2String(selectField, value);
        }
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(SelectField selectField, String value) {
        if (BooleanUtils.isTrue(selectField.getMultiple())) {
            return ModuleFieldResolverFactory.getResolver(ModuleFieldResolverFactory.MULTI_SELECT)
                    .parse2Value(selectField, value);
        }
        return parse2Array(value);
    }
}
