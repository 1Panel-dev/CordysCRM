package io.cordys.common.dto.condition;

import io.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 表示组合条件，用于支持复杂的过滤和查询逻辑。
 * 包含字段名、操作符和期望值等信息。
 */
@Data
public class FilterCondition {

    /**
     * 系统字段为字段名
     * 模块字段为字段ID
     */
    @Schema(description = "条件的参数名称")
    @NotNull
    private String name;

    @Schema(description = "期望值，若操作符为 BETWEEN, IN, NOT_IN 时为数组，其他操作符为单个值")
    private Object value;

    @Schema(description = "是否是多选值")
    @NotNull
    private Boolean multipleValue = false;

    @Schema(description = "操作符",
            allowableValues = {"IN", "NOT_IN", "BETWEEN", "GT", "LT", "COUNT_GT", "COUNT_LT", "EQUALS", "NOT_EQUALS", "CONTAINS", "NOT_CONTAINS", "EMPTY", "NOT_EMPTY"})
    @EnumValue(enumClass = CombineConditionOperator.class)
    private String operator;

    /**
     * 校验条件是否合法，检查字段名称、操作符和值的有效性。
     *
     * @return 如果条件合法则返回 true，否则返回 false
     */
    public boolean valid() {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(operator)) {
            return false;
        }

        // 针对空值判断操作符
        if (StringUtils.equalsAny(operator, CombineConditionOperator.EMPTY.name(), CombineConditionOperator.NOT_EMPTY.name())) {
            return true;
        }

        if (value == null) {
            return false;
        }

        // 针对值为集合类型的校验
        if (value instanceof List<?> valueList && CollectionUtils.isEmpty(valueList)) {
            return false;
        }

        // 针对值为字符串的校验
        if (value instanceof String valueStr && StringUtils.isBlank(valueStr)) {
            return false;
        }

        return true;
    }

    /**
     * 枚举：组合条件操作符，定义了各种可能的查询操作符。
     */
    public enum CombineConditionOperator {
        /**
         * 属于某个集合
         */
        IN,

        /**
         * 不属于某个集合
         */
        NOT_IN,

        /**
         * 区间操作
         */
        BETWEEN,

        /**
         * 大于
         */
        GT,

        /**
         * 小于
         */
        LT,

        /**
         * 数量大于
         */
        COUNT_GT,

        /**
         * 数量小于
         */
        COUNT_LT,

        /**
         * 等于
         */
        EQUALS,

        /**
         * 不等于
         */
        NOT_EQUALS,

        /**
         * 包含
         */
        CONTAINS,

        /**
         * 不包含
         */
        NOT_CONTAINS,

        /**
         * 为空
         */
        EMPTY,

        /**
         * 不为空
         */
        NOT_EMPTY
    }
}
