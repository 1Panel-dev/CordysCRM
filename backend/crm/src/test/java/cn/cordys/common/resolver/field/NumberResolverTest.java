package cn.cordys.common.resolver.field;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.dto.field.InputNumberField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberResolverTest {

    private final NumberResolver resolver = new NumberResolver();
    private MessageSource originalMessageSource;

    @BeforeEach
    void initializeTranslator() {
        originalMessageSource = (MessageSource) ReflectionTestUtils.getField(
                Translator.class, "messageSource");
        ReflectionTestUtils.setField(
                Translator.class, "messageSource", new StaticMessageSource());
    }

    @AfterEach
    void restoreTranslator() {
        ReflectionTestUtils.setField(
                Translator.class, "messageSource", originalMessageSource);
    }

    @Test
    void validatesConfiguredRangeAndDecimalPrecision() {
        InputNumberField field = numberField();

        assertDoesNotThrow(() -> resolver.validate(field, new BigDecimal("-10")));
        assertDoesNotThrow(() -> resolver.validate(field, new BigDecimal("100")));
        assertDoesNotThrow(() -> resolver.validate(field, new BigDecimal("1.23")));
        assertThrows(GenericException.class,
                () -> resolver.validate(field, new BigDecimal("-10.01")));
        assertThrows(GenericException.class,
                () -> resolver.validate(field, new BigDecimal("100.01")));
        assertThrows(GenericException.class,
                () -> resolver.validate(field, new BigDecimal("1.234")));
    }

    @Test
    void validatesGlobalNineIntegerAndFourDecimalDigitLimits() {
        InputNumberField field = numberField();
        field.setMin(null);
        field.setMax(null);
        field.setPrecision(4);

        assertDoesNotThrow(() -> resolver.validate(
                field, new BigDecimal("999999999.9999")));
        assertThrows(GenericException.class, () -> resolver.validate(
                field, new BigDecimal("1000000000")));
        assertThrows(GenericException.class, () -> resolver.validate(
                field, new BigDecimal("0.00001")));
    }

    @Test
    void rejectsFractionWhenDecimalPlacesAreDisabled() {
        InputNumberField field = numberField();
        field.setDecimalPlaces(false);

        assertDoesNotThrow(() -> resolver.validate(field, new BigDecimal("1.0")));
        assertThrows(GenericException.class,
                () -> resolver.validate(field, new BigDecimal("1.1")));
    }

    private InputNumberField numberField() {
        InputNumberField field = new InputNumberField();
        field.setName("金额");
        field.setMin(-10);
        field.setMax(100);
        field.setDecimalPlaces(true);
        field.setPrecision(2);
        return field;
    }
}
