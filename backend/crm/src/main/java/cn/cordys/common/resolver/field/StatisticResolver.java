package cn.cordys.common.resolver.field;

import cn.cordys.crm.system.dto.field.InputNumberField;
import cn.cordys.crm.system.dto.field.StatisticField;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 统计字段解析器。
 *
 * <p>统计字段由后端聚合计算后持久化, 前端不可直接编辑, 因此保存表单数据时
 * 不校验必填与取值范围; 展示时按 {@code resultFormat / decimalPlaces / precision /
 * showThousandsSeparator} 格式化, 与数值、公式字段保持一致。</p>
 */
public class StatisticResolver extends AbstractModuleFieldResolver<StatisticField> {

    public static final String PERCENT_FORMAT = "percent";
    public static final String PERCENT_SUFFIX = "%";

    @Override
    public void validate(StatisticField statisticField, Object value) {
        // 统计值由后端计算写入, 客户端不提交该字段, 因此不做必填校验;
        // 若客户端携带了刷新后的值, 只校验类型, 避免非法值落库。
        if (value != null && !(value instanceof Number) && !(value instanceof String)) {
            throwValidateException(statisticField.getName());
        }
    }

    @Override
    public Object convertToValue(StatisticField statisticField, String value) {
        // 统计字段可能因无关联数据而未被写入, 空串按空值处理, 交由展示层显示 "-"
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    @Override
    public Object transformToValue(StatisticField statisticField, String value) {
        if (value == null) {
            return null;
        }
        try {
            BigDecimal actualDecimal = new BigDecimal(value).stripTrailingZeros();
            if (BooleanUtils.isTrue(statisticField.getDecimalPlaces())) {
                actualDecimal = actualDecimal.setScale(statisticField.getPrecision(), RoundingMode.HALF_UP);
            }
            String formatActualVal;
            if (BooleanUtils.isTrue(statisticField.getShowThousandsSeparator())) {
                formatActualVal = InputNumberField.formatThousands(actualDecimal);
            } else {
                formatActualVal = actualDecimal.toPlainString();
            }
            return formatActualVal + (Strings.CI.equals(statisticField.getNumberFormat(), PERCENT_FORMAT) ? PERCENT_SUFFIX : StringUtils.EMPTY);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @Override
    public Object textToValue(StatisticField field, String text) {
        if (Strings.CS.equals(field.getNumberFormat(), PERCENT_FORMAT)) {
            text = text.replace(PERCENT_SUFFIX, StringUtils.EMPTY);
        }
        if (BooleanUtils.isTrue(field.getShowThousandsSeparator())) {
            text = text.replace(",", StringUtils.EMPTY).replace("，", StringUtils.EMPTY);
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return text;
        }
    }
}
