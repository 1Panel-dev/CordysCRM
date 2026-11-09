package cn.cordys.common.security.validator;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class SSRFValidatorTest {
    private final SSRFValidator validator = new SSRFValidator();

    @ParameterizedTest
    @ValueSource(strings = { "http://10.20.30.40:8100/dataease", "http://172.20.0.4:8100",
            "https://192.168.1.12", "http://[fd12::4]:8100" })
    void privateDeploymentsAreAllowedOnlyForIntegrationValidation(String url) {
        assertDoesNotThrow(() -> validator.validateIntegrationUrl(url));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://127.0.0.1:8100", "http://[::1]", "http://169.254.169.254",
            "http://[fe80::1]", "http://0.0.0.0", "http://224.0.0.1", "http://[::ffff:127.0.0.1]",
            "http://[64:ff9b::7f00:1]", "file:///etc/hosts", "ftp://10.20.30.40", "http://user@10.20.30.40",
            "http:///missing-host" })
    void integrationModeStillRejectsLocalMetadataAndInvalidTargets(String url) {
        assertThrows(IllegalArgumentException.class, () -> validator.validateIntegrationUrl(url));
    }

    @Test
    void integrationModeStillHonorsTheConfiguredAllowlist() {
        ReflectionTestUtils.setField(validator, "whitelistEnabled", true);
        ReflectionTestUtils.setField(validator, "allowedList", List.of("10.20.30.40"));
        assertDoesNotThrow(() -> validator.validateIntegrationUrl("http://10.20.30.40:8100"));
        try (var translations = mockStatic(Translator.class, invocation -> invocation.getArgument(0))) {
            assertThrows(GenericException.class, () -> validator.validateIntegrationUrl("http://10.20.30.41:8100"));
        }
    }
}
