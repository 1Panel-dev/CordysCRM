package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.SelectField;


import java.util.List;

/**
 * @author jianxing
 */
public class MultipleSelectResolver extends AbstractModuleFieldResolver<SelectField> {

    @Override
    public void validate(SelectField selectField, Object value) {
        // 校验必填
        validateRequired(selectField, value);

        // 校验值类型
        validateArray(selectField.getName(), value);

        // 校验选项正确
        for (String item : (List<String>) value) {
            validateOptions(selectField.getName(), item, selectField.getOptions());
        }
    }

    @Override
    public String parse2String(SelectField selectField, Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(SelectField selectField, String value) {
        return parse2Array(value);
    }
}
