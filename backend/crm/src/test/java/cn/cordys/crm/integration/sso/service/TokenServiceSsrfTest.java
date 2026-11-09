package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.security.validator.SSRFValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceSsrfTest {
    private final TokenService service = new TokenService();

    @Test
    void rejectedTargetsNeverOpenASocket() {
        ReflectionTestUtils.setField(service, "ssrfValidator", new SSRFValidator());
        try (var sockets = mockConstruction(Socket.class)) {
            assertFalse(service.pingDeUrl("http://127.0.0.1:8100"));
            assertFalse(service.pingDeUrl("http://169.254.169.254"));
            assertTrue(sockets.constructed().isEmpty());
        }
    }

    @Test
    void connectsToTheValidatedAddressWithoutResolvingTheHostnameAgain() throws Exception {
        SSRFValidator validator = mock(SSRFValidator.class);
        InetAddress address = InetAddress.getByName("10.20.30.40");
        when(validator.validateIntegrationUrl("https://dataease.internal:8443/dataease")).thenReturn(address);
        ReflectionTestUtils.setField(service, "ssrfValidator", validator);
        try (var sockets = mockConstruction(Socket.class)) {
            assertTrue(service.pingDeUrl("https://dataease.internal:8443/dataease"));
            assertEquals(1, sockets.constructed().size());
            verify(sockets.constructed().getFirst()).connect(new InetSocketAddress(address, 8443), 3000);
        }
    }
}
