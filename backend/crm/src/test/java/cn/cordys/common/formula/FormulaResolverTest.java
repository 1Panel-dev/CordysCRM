package cn.cordys.common.formula;

import cn.cordys.common.resolver.field.FormulaResolver;
import cn.cordys.crm.system.dto.field.FormulaField;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaResolverTest {
    @Test
    void CRM原有展示与读取行为不受MCP截断规则影响() {
        FormulaField field = new FormulaField();
        field.setFormulaResultFormat("number");
        field.setDecimalPlaces(true);
        field.setPrecision(2);
        field.setShowThousandsSeparator(true);
        FormulaResolver resolver = new FormulaResolver();
        assertEquals("1,234.57", resolver.convertToValue(field, "1234.567"));
        assertEquals("1,234.57", resolver.transformToValue(field, "1234.567"));
        assertEquals("-1,234.57", resolver.transformToValue(field, "-1234.567"));
        field.setShowThousandsSeparator(false);
        field.setDecimalPlaces(false);
        assertEquals("-1234.567", resolver.transformToValue(field, "-1234.567"));
        assertEquals("TRUE", resolver.convertToValue(field, "TRUE"));
    }
}
