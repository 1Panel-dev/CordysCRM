package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.integration.sso.constants.OAuthStateFlow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OAuth state 安全机制的单元测试，覆盖随机性、流程绑定、有效期和一次性消费约束。
 */
class OAuthStateServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void generatedStateIsHighEntropyAndCanOnlyBeConsumedOnce() {
        OAuthStateService service = serviceAt(NOW);
        MockHttpSession session = new MockHttpSession();

        String first = service.generateState(OAuthStateFlow.GITHUB, session);
        String second = service.generateState(OAuthStateFlow.GITHUB, session);

        assertTrue(first.matches("github\\.[A-Za-z0-9_-]{43}"));
        assertNotEquals(first, second);
        service.validateAndConsume(OAuthStateFlow.GITHUB, first, session);
        assertThrows(GenericException.class, () -> service.validateAndConsume(OAuthStateFlow.GITHUB, first, session));
    }

    @Test
    void rejectsMissingMismatchedAndExpiredState() {
        MockHttpSession session = new MockHttpSession();
        OAuthStateService service = serviceAt(NOW);
        String state = service.generateState(OAuthStateFlow.LARK, session);

        assertThrows(GenericException.class, () -> service.validateAndConsume(OAuthStateFlow.LARK, "", session));
        assertThrows(GenericException.class, () -> service.validateAndConsume(OAuthStateFlow.WECOM, state, session));
        OAuthStateService afterExpiry = serviceAt(NOW.plusSeconds(601));
        assertThrows(GenericException.class, () -> afterExpiry.validateAndConsume(OAuthStateFlow.LARK, state, session));
    }

    @Test
    void rejectsUnsupportedFlowWhenGeneratingState() {
        OAuthStateService service = serviceAt(NOW);

        assertThrows(GenericException.class, () -> service.generateState("unsupported", new MockHttpSession()));
    }

    private OAuthStateService serviceAt(Instant instant) {
        return new OAuthStateService(new SecureRandom(), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
