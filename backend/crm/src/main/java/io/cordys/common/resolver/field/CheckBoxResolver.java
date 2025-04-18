package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.CheckBoxField;

import java.util.List;

/**
 * @author jianxing
 */
public class CheckBoxResolver extends AbstractModuleFieldResolver<CheckBoxField> {

    @Override
    public void validate(CheckBoxField checkBoxField, Object value) {
        // 校验必填
        validateRequired(checkBoxField, value);

        // 校验值类型
        validateArray(checkBoxField.getName(), value);

        // 校验选项正确
        for (String item : (List<String>) value) {
            validateOptions(checkBoxField.getName(), item, checkBoxField.getOptions());
        }
    }

    @Override
    public String parse2String(CheckBoxField checkBoxField, Object value) {
        return JSON.toJSONString(value);
    }

    @Override
    public Object parse2Value(CheckBoxField checkBoxField, String value) {
        return parse2Array(value);
    }
}
