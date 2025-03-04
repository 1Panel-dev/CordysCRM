package io.cordys.common.resolver.field;


import io.cordys.common.constants.RuleValidatorConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.field.base.OptionProp;
import io.cordys.crm.system.dto.field.base.RuleProp;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.cordys.common.constants.CommonResultCode.FIELD_VALIDATE_ERROR;


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
     * @param value
     * @return
     */
    public Object parse2Value(T selectField, String value) {
        return value;
    }

    /**
     * 将对应的参数值转换成字符串
     * @param value
     * @return
     */
    public String parse2String(T selectField, Object value) {
        return value == null ? null : value.toString();
    }

    protected void throwValidateException(String name) {
        throw new GenericException(FIELD_VALIDATE_ERROR, Translator.getWithArgs(FIELD_VALIDATE_ERROR.getMessage(), name));
    }

    protected void validateRequired(T customField, Object value) {
        if (!hasValidatorKey(customField.getRules(), RuleValidatorConstants.REQUIRED)) {
            return;
        }
        if (value == null
                || (value instanceof String && StringUtils.isBlank(value.toString()))
                || (value instanceof List listValue && CollectionUtils.isEmpty((listValue)))) {
            throwValidateException(customField.getName());
        }
    }

    protected void validateArray(String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            ((List) value).forEach(v -> validateString(name, v));
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

    private boolean hasValidatorKey(List<RuleProp> rules, String validatorKey) {
        if (CollectionUtils.isEmpty(rules)) {
            return false;
        }
        return rules.stream().anyMatch(rule -> StringUtils.equals(rule.getKey(), validatorKey));
    }
}
