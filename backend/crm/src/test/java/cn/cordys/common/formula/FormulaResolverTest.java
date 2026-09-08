package cn.cordys.common.formula;

import cn.cordys.common.resolver.field.FormulaResolver;
import cn.cordys.crm.system.dto.field.FormulaField;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaResolverTest {
    @Test
    void 展示截断而原始读取保留精度且没有千分位() {
        FormulaField field = new FormulaField();
        field.setFormulaResultFormat("number");
        field.setDecimalPlaces(true);
        field.setPrecision(2);
        field.setShowThousandsSeparator(true);
        FormulaResolver resolver = new FormulaResolver();
        assertEquals(new BigDecimal("1234.567"), resolver.convertToValue(field, "1,234.567"));
        assertEquals("1,234.56", resolver.transformToValue(field, "1234.567"));
        assertEquals("-1,234.56", resolver.transformToValue(field, "-1234.567"));
        field.setShowThousandsSeparator(false);
        field.setDecimalPlaces(false);
        assertEquals("-1234", resolver.transformToValue(field, "-1234.567"));
        assertEquals("TRUE", resolver.convertToValue(field, "TRUE"));
    }
}
