package io.cordys.common.resolver.field;

import io.cordys.common.util.TimeUtils;
import io.cordys.crm.system.dto.field.DateTimeField;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * @author jianxing
 */
public class DateTimeResolver extends AbstractModuleFieldResolver<DateTimeField> {

    @Override
    public void validate(DateTimeField dateTimeField, Object value) {
        validateRequired(dateTimeField, value);

        validateLong(dateTimeField.getName(), value);
    }

    protected void validateLong(String name, Object value) {
        if (value != null && !(value instanceof Long)) {
            throwValidateException(name);
        }
    }

    @Override
    public Object parse2Value(DateTimeField dateTimeField, String value) {
        return parse2Long(value);
    }

    @Override
    public Object trans2Value(DateTimeField dateTimeField, String value) {
        if (StringUtils.equalsIgnoreCase(dateTimeField.getDateType(), "date")) {
            return TimeUtils.getDataStr(Long.valueOf(value));
        }
        if (StringUtils.equalsIgnoreCase(dateTimeField.getDateType(), "datetime")) {
            return TimeUtils.getDataTimeStr(Long.valueOf(value));
        }
        return value;
    }

    @Override
    public Object text2Value(DateTimeField field, String text) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-M-d H:m:s"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/M/d H:m:s"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-M-d"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/M/d"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-M"))
                .appendOptional(DateTimeFormatter.ofPattern("yyyy/M"))
                .toFormatter();

        TemporalAccessor parsed = formatter.parseBest(text,
                LocalDateTime::from,
                LocalDate::from,
                YearMonth::from);

        Instant instant;
        if (parsed instanceof LocalDateTime) {
            instant = ((LocalDateTime) parsed).atZone(ZoneId.systemDefault()).toInstant();
        } else if (parsed instanceof LocalDate) {
            instant = ((LocalDate) parsed).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else if (parsed instanceof YearMonth) {
            instant = ((YearMonth) parsed).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else {
            throw new DateTimeParseException("无法解析日期时间: " + text, text, 0);
        }

        return instant.toEpochMilli();
    }
}
