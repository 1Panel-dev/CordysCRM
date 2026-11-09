package cn.cordys.crm.integration.dataease;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.security.validator.SSRFValidator;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.integration.common.request.DeThirdConfigRequest;
import cn.cordys.crm.integration.sso.service.TokenService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

class DataEaseClientSsrfTest {
    private final SSRFValidator validator = spy(new SSRFValidator());
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger redirectTargets = new AtomicInteger();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> dataEaseToken = new AtomicReference<>();
    private HttpServer server;
    private MockedStatic<CommonBeanFactory> beans;
    private String baseUrl;
    private DataEaseClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        beans = mockStatic(CommonBeanFactory.class);
        beans.when(() -> CommonBeanFactory.getBean(SSRFValidator.class)).thenReturn(validator);
        DeThirdConfigRequest config = new DeThirdConfigRequest();
        config.setRedirectUrl(baseUrl);
        config.setDeAccessKey("0123456789abcdef");
        config.setDeSecretKey("fedcba9876543210");
        client = new DataEaseClient(config);
    }

    @AfterEach
    void tearDown() {
        if (beans != null) {
            beans.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void blockedTargetsDoNotReceiveRequests() {
        assertThrows(IllegalArgumentException.class, () -> client.get("user/personInfo"));
        assertThrows(IllegalArgumentException.class, () -> client.post("org/page/lazyTree"));
        assertEquals(0, requests.get());
    }

    @Test
    void signedRequestsAndUriVariablesRemainUnchanged() throws Exception {
        allowLocalTestServer();
        assertEquals(0, client.get("user/personInfo").getCode());
        assertTrue(dataEaseToken.get() != null && !dataEaseToken.get().isBlank());
        client.post("user/switch/{orgId}", Map.of("enabled", true), "org-a");
        assertEquals("/de2api/user/switch/org-a", path.get());
        assertEquals("{\"enabled\":true}", body.get());
        assertTrue(dataEaseToken.get() != null && !dataEaseToken.get().isBlank());
    }

    @ParameterizedTest
    @ValueSource(ints = { 301, 302, 303, 307, 308 })
    void dataEaseRedirectsAreRejectedForGetAndPost(int redirectStatus) throws Exception {
        allowLocalTestServer();
        status.set(redirectStatus);
        assertThrows(GenericException.class, () -> client.get("user/personInfo"));
        assertThrows(GenericException.class, () -> client.post("org/page/lazyTree"));
        assertEquals(2, requests.get());
        assertEquals(0, redirectTargets.get());
    }

    @Test
    void maxKbKeepsBearerAuthenticationAndDoesNotFollowRedirects() {
        // 仅放行本机假服务，实际地址策略另由 SSRFValidatorTest 覆盖。
        doNothing().when(validator).validate(anyString());
        TokenService service = new TokenService();
        assertTrue(service.getMaxKBToken(baseUrl, "synthetic-key"));
        assertEquals("Bearer synthetic-key", authorization.get());
        status.set(302);
        assertThrows(RuntimeException.class, () -> service.getMaxKBToken(baseUrl, "synthetic-key"));
        assertEquals(2, requests.get());
        assertEquals(0, redirectTargets.get());
    }

    private void allowLocalTestServer() throws Exception {
        // HTTP 测试使用本机服务，私网地址的允许与禁止策略在独立测试中验证。
        doReturn(InetAddress.getByName("127.0.0.1")).when(validator).validateIntegrationUrl(anyString());
    }

    private void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            requests.incrementAndGet();
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            dataEaseToken.set(exchange.getRequestHeaders().getFirst("x-de-ask-token"));
            if ("/target".equals(path.get())) {
                redirectTargets.incrementAndGet();
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if (status.get() != 200) {
                exchange.getResponseHeaders().set("Location", baseUrl + "/target");
            }
            String response = path.get().startsWith("/de2api/") ? "{\"code\":0}" : "{\"code\":200}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
