package cn.cordys.common.resolver.field;


import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.cordys.common.constants.CommonResultCode.FIELD_VALIDATE_ERROR;


/**
 * @author jainxing
 */
public abstract class AbstractModuleFieldResolver<T extends BaseField> {

    /**
     * 校验参数是否合法
     *
     * @param customField
     * @param value
     */
    abstract public void validate(T customField, Object value);

    /**
     * 将数据库的字符串值转换为对应的参数值
     *
     * @param value
     *
     * @return
     */
    public Object convertToValue(T selectField, String value) {
        return value;
    }

    /**
     * 将数据库的字符串值转换为对应的参数值
     *
     * @param value
     *
     * @return
     */
    public Object transformToValue(T selectField, String value) {
        return value;
    }

    /**
     * 字段文本 => 值
     *
     * @param field 字段
     * @param text  文本
     *
     * @return 字段值
     */
    public Object textToValue(T field, String text) {
        return text;
    }


    /**
     * 将对应的参数值转换成字符串
     *
     * @param value
     *
     * @return
     */
    public String convertToString(T selectField, Object value) {
        return value == null ? null : value.toString();
    }

    protected void throwValidateException(String name) {
        throw new GenericException(FIELD_VALIDATE_ERROR, Translator.getWithArgs(FIELD_VALIDATE_ERROR.getMessage(), name));
    }

    protected void validateRequired(T customField, Object value) {
        // 移动端，不一定需要校验必填，暂时不校验
    }

    protected void validateArray(String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(v -> validateString(name, v));
        } else {
            throwValidateException(name);
        }
    }

    protected void validateString(String name, Object v) {
        if (v != null && !(v instanceof String)) {
            throwValidateException(name);
        }
    }

    protected void validateOptions(String name, Object value, List<OptionProp> options) {
        if (value == null) {
            return;
        }
        if (options == null) {
            options = List.of();
        }
        Set<String> values = options.stream()
                .map(OptionProp::getValue)
                .collect(Collectors.toSet());
        if (!values.contains(value)) {
            throwValidateException(name);
        }
    }

    protected String getStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    protected Object parse2Array(String value) {
        return value == null ? null : JSON.parseArray(value);
    }

    protected Object parse2Long(String value) {
        return value == null ? null : Long.valueOf(value);
    }

    protected String getJsonString(Object value) {
        return value == null ? null : JSON.toJSONString(value);
    }

    protected List<String> parseFakeJsonArray(String content) {
        return Arrays.stream(content.replaceAll("^\\[|]$", "").replaceAll("['\"]", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
