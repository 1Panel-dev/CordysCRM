package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.CheckBoxField;
import io.cordys.crm.system.dto.field.SelectMultipleField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianxing
 */
public class SelectMultipleResolver extends AbstractModuleFieldResolver<SelectMultipleField> {

    @Override
    public void validate(SelectMultipleField selectField, Object value) {
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
    public String parse2String(SelectMultipleField selectField, Object value) {
        return getJsonString(value);
    }

    @Override
    public Object parse2Value(SelectMultipleField selectField, String value) {
        return parse2Array(value);
    }

    @Override
    public Object trans2Value(SelectMultipleField selectMultipleField, String value) {
        if (StringUtils.isBlank(value) || StringUtils.equals(value, "[]")) {
            return StringUtils.EMPTY;
        }
        List list = JSON.parseArray(value);
        List<String> result = new ArrayList<>();
        Map<String, String> optionValueMap = selectMultipleField.getOptions().stream().collect(Collectors.toMap(OptionProp::getValue, OptionProp::getLabel));
        list.forEach(item -> {
            if (optionValueMap.containsKey(item.toString())) {
                result.add(optionValueMap.get(item.toString()));
            }
        });
        return String.join(",", JSON.parseArray(JSON.toJSONString(result)));
    }
}
