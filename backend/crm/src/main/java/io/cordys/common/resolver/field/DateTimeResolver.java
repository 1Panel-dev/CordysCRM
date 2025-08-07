package io.cordys.common.resolver.field;

import io.cordys.common.util.TimeUtils;
import io.cordys.crm.system.dto.field.DateTimeField;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

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
                .appendPattern("[yyyy/M/d H:m:s][yyyy-M-d H:m:s]")
                .toFormatter();
        LocalDateTime parse = LocalDateTime.parse(text.contains(" ") ? text : text + " 00:00:00", formatter);
        return parse.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
