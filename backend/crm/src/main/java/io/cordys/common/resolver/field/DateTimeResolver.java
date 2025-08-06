package io.cordys.common.resolver.field;

import io.cordys.common.util.TimeUtils;
import io.cordys.crm.system.dto.field.DateTimeField;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
        DateTimeFormatter formatter;
         if (StringUtils.equalsIgnoreCase(field.getDateType(), "datetime")) {
            formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        } else if (StringUtils.equalsIgnoreCase(field.getDateType(), "date")) {
            formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        } else {
            formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        }
        LocalDateTime parse = LocalDateTime.parse(text, formatter);
        return String.valueOf(parse.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
