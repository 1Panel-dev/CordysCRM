package io.cordys.common.resolver.field;


import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.field.SelectMultipleField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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

        try {
            List<String> list = JSON.parseArray(value, String.class);
            if (list == null || list.isEmpty()) {
                return StringUtils.EMPTY;
            }

            Map<String, String> optionValueMap = selectMultipleField.getOptions().stream()
                    .collect(Collectors.toMap(OptionProp::getValue, OptionProp::getLabel, (a, b) -> a));

            List<String> result = list.stream()
                    .filter(item -> item != null && optionValueMap.containsKey(item))
                    .map(optionValueMap::get)
                    .collect(Collectors.toList());

            return String.join(",", result);
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public Object text2Value(SelectMultipleField field, String text) {
        if (StringUtils.isBlank(text) || StringUtils.equals(text, "[]")) {
            return StringUtils.EMPTY;
        }

        try {
            List<String> texts = parseFakeJsonArray(text);
            if (CollectionUtils.isEmpty(texts)) {
                return StringUtils.EMPTY;
            }

            Map<String, String> optionMap = field.getOptions().stream()
                    .collect(Collectors.toMap(OptionProp::getLabel, OptionProp::getValue, (v1, v2) -> v1));
            List<String> values = texts.stream()
                    .filter(item -> item != null && optionMap.containsKey(item))
                    .map(optionMap::get)
                    .collect(Collectors.toList());

            return CollectionUtils.isEmpty(values) ? texts : values;
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }
}
