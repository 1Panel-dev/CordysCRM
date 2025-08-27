package cn.cordys.common.constants;

import cn.cordys.common.util.Translator;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 枚举值校验注解，确保值为指定枚举类中的有效值。
 * 可选排除某些枚举值。
 *
 * @author jianxing
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EnumValue.EnumValueValidator.class)
public @interface EnumValue {

    /**
     * 错误提示信息，当校验失败时使用。
     *
     * @return 错误提示消息
     */
    String message() default "{enum_value_valid_message}";

    /**
     * 必须的属性，用于分组校验。
     *
     * @return 分组校验的类
     */
    Class<?>[] groups() default {};

    /**
     * 校验负载。
     *
     * @return 校验负载的类
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * 校验时使用的枚举类。
     *
     * @return 枚举类
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 校验时排除的枚举值，仅支持字符串类型。
     *
     * @return 排除的枚举值
     */
    String[] excludeValues() default {};

    /**
     * 枚举值校验的实现类。
     *
     * @see EnumValue
     */
    class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

        private Class<? extends Enum<?>> enumClass;
        private String[] excludeValues;

        @Override
        public void initialize(EnumValue enumValue) {
            this.enumClass = enumValue.enumClass();
            this.excludeValues = enumValue.excludeValues();
        }

        /**
         * 校验参数是否在枚举值中。
         * 如果设置了排除值，校验值不应在排除列表中。
         *
         * @param value   待校验的值
         * @param context 校验上下文
         * @return 校验结果，若值有效返回 true，否则返回 false
         */
        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            // 如果值为空，则认为有效
            if (value == null) {
                return true;
            }

            // 获取枚举类的所有实例
            Enum<?>[] enums = enumClass.getEnumConstants();
            List<Object> values = new ArrayList<>();

            // 获取枚举类的所有有效值
            for (Enum<?> item : enums) {
                if (item instanceof ValueEnum) {
                    values.add(((ValueEnum<?>) item).getValue());
                } else {
                    values.add(item.name());
                }
            }

            // 判断是否排除指定的枚举值
            boolean isExcludeValue = excludeValues != null && Arrays.stream(excludeValues).anyMatch(value::equals);
            boolean valid = values.contains(value) && !isExcludeValue;

            // 如果校验失败，生成自定义错误消息
            if (!valid) {
                context.disableDefaultConstraintViolation();
                String errorValues = CollectionUtils.subtract(values, Arrays.asList(excludeValues)).toString();
                context.buildConstraintViolationWithTemplate(Translator.get("enum_value_valid_message") + errorValues).addConstraintViolation();
            }

            return valid;
        }
    }
}
