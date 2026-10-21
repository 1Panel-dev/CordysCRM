package cn.cordys.common.util;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.math.BigDecimal;

public class BigDecimalNoTrailingZeroSerializer extends ValueSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext provider) {
        // 去除末尾零并转换为普通字符串
        gen.writeNumber(value.stripTrailingZeros().toPlainString());
    }
}
