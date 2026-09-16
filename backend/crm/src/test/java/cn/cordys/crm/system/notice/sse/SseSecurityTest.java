package cn.cordys.crm.system.notice.sse;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.handler.RestControllerExceptionHandler;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.crm.system.service.SendModuleService;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SseSecurityTest {
    private final SseService service = new SseService();
    private final List<Disposable> subscriptions = new ArrayList<>();
    private MockedStatic<Translator> translator;
    private MockedStatic<SessionUtils> session;
    private MockedStatic<OrganizationContext> organization;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        session = mockStatic(SessionUtils.class);
        organization = mockStatic(OrganizationContext.class);
        organization.when(OrganizationContext::getOrganizationId).thenReturn("org-a");
        login("user-a", "session-a");
        mvc = MockMvcBuilders.standaloneSetup(new SseController(service))
                .setControllerAdvice(new RestControllerExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() {
        subscriptions.forEach(Disposable::dispose);
        session.close();
        organization.close();
        translator.close();
    }

    private void login(String userId, String sessionId) {
        session.when(SessionUtils::getUserId).thenReturn(userId);
        session.when(SessionUtils::getSessionId).thenReturn(sessionId);
    }

    private List<String> listen(Flux<String> flux) {
        List<String> messages = new ArrayList<>();
        subscriptions.add(flux.subscribe(messages::add));
        return messages;
    }

    @Test
    void rejectsForgedUserOnBothEndpoints() throws Exception {
        for (String endpoint : List.of("subscribe", "close")) {
            mvc.perform(get("/sse/" + endpoint).param("userId", "user-b").param("clientId", "client"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void rejectsMissingLoginAndSession() throws Exception {
        for (String endpoint : List.of("subscribe", "close")) {
            login(null, null);
            mvc.perform(get("/sse/" + endpoint).param("clientId", "client"))
                    .andExpect(status().isUnauthorized());
            login("user-a", null);
            mvc.perform(get("/sse/" + endpoint).param("clientId", "client"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void derivesIdentityAndDeliversOnlyToRecipient() {
        List<String> first = listen(service.addClient(null, "same-client"));
        login("user-b", "session-b");
        List<String> second = listen(service.addClient("user-b", "same-client"));
        first.clear();
        second.clear();
        service.sendToUser("user-b", "org-a", "synthetic notification");
        assertTrue(first.isEmpty());
        assertEquals(List.of("\"synthetic notification\""), second);
    }

    @Test
    void anotherSessionCannotCloseOrReuseConnection() {
        Flux<String> first = service.addClient(null, "client");
        List<String> messages = listen(first);
        login("user-a", "session-b");
        service.removeClient(null, "client");
        Flux<String> second = service.addClient(null, "client");
        assertNotSame(first, second);
        listen(second);
        messages.clear();
        service.sendToUser("user-a", "org-a", "still connected");
        assertEquals(List.of("\"still connected\""), messages);
    }

    @Test
    void limitDoesNotEvictExistingSessions() {
        List<String> first = listen(service.addClient(null, "one"));
        login("user-a", "session-b");
        List<String> second = listen(service.addClient(null, "two"));
        GenericException error = assertThrows(GenericException.class, () -> service.addClient(null, "three"));
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
        first.clear();
        second.clear();
        service.sendToUser("user-a", "org-a", "preserved");
        assertEquals(List.of("\"preserved\""), first);
        assertEquals(first, second);
    }

    @Test
    void closeTerminatesHeartbeatAndFreesSlot() throws Exception {
        AtomicBoolean completed = new AtomicBoolean();
        Flux<String> original = service.addClient(null, "one");
        subscriptions.add(original.subscribe(message -> { }, error -> fail(error), () -> completed.set(true)));
        listen(service.addClient(null, "two"));
        mvc.perform(get("/sse/close").param("clientId", "one")).andExpect(status().isOk());
        assertTrue(completed.get());
        assertNotSame(original, service.addClient(null, "one"));
    }

    @Test
    void cancellationFreesSlot() {
        Disposable first = service.addClient(null, "one").subscribe();
        listen(service.addClient(null, "two"));
        first.dispose();
        assertNotNull(service.addClient(null, "three"));
    }

    @Test
    void adminCanRegisterAndCannotImpersonateAnotherUser() {
        login("admin", "admin-session");
        List<String> messages = listen(service.addClient(null, "one"));
        listen(service.addClient("admin", "two"));
        messages.clear();
        service.sendToUser("admin", "org-a", "admin notification");
        assertEquals(List.of("\"admin notification\""), messages);
        GenericException limit = assertThrows(GenericException.class, () -> service.addClient(null, "three"));
        assertEquals(CrmHttpResultCode.FORBIDDEN, limit.getErrorCode());
        GenericException error = assertThrows(GenericException.class, () -> service.addClient("user-b", "three"));
        assertEquals(CrmHttpResultCode.FORBIDDEN, error.getErrorCode());
    }

    @Test
    void organizationsCannotShareOrCloseStreams() {
        List<String> first = listen(service.addClient(null, "client"));
        organization.when(OrganizationContext::getOrganizationId).thenReturn("org-b");
        service.removeClient(null, "client");
        List<String> second = listen(service.addClient(null, "client"));
        first.clear();
        second.clear();
        service.sendToUser("user-a", "org-a", "organization notification");
        assertEquals(List.of("\"organization notification\""), first);
        assertTrue(second.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcastFiltersCachedNotificationsByRecipientAndOrganization() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        ZSetOperations<String, String> sorted = mock(ZSetOperations.class);
        SendModuleService modules = mock(SendModuleService.class);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "sendModuleService", modules);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(modules.getNoticeModules("org-a")).thenReturn(List.of());
        when(sorted.range("msg_user:user-a", 0, -1)).thenReturn(Set.of("one", "two", "three"));
        when(values.get("msg_content:one")).thenReturn(notification("user-a", "org-a", "allowed"));
        when(values.get("msg_content:two")).thenReturn(notification("user-a", "org-b", "other-org"));
        when(values.get("msg_content:three")).thenReturn(notification("user-b", "org-a", "other-user"));
        List<String> messages = listen(service.addClient(null, "client"));
        messages.clear();
        // Redis consumer threads have no request organization context.
        organization.when(OrganizationContext::getOrganizationId).thenReturn(null);
        service.broadcastPeriodically("user-a", NotificationConstants.Type.SYSTEM_NOTICE.toString());
        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().contains("allowed"));
        assertFalse(messages.getFirst().contains("other-org"));
        assertFalse(messages.getFirst().contains("other-user"));
    }

    private String notification(String userId, String organizationId, String subject) {
        Notification notification = new Notification();
        notification.setReceiver(userId);
        notification.setOrganizationId(organizationId);
        notification.setSubject(subject);
        notification.setContent(new byte[0]);
        notification.setCreateTime(1L);
        return JSON.toJSONString(notification);
    }

    @Test
    void oldStreamCancellationCannotRemoveReconnectedStream() {
        Disposable old = service.addClient(null, "client").subscribe();
        service.removeClient(null, "client");
        List<String> messages = listen(service.addClient(null, "client"));
        old.dispose();
        messages.clear();
        service.sendToUser("user-a", "org-a", "reconnected");
        assertEquals(List.of("\"reconnected\""), messages);
    }

    @Test
    void rejectsBlankClientIdAndDuplicateSubscriptionsWithoutClosingOriginal() throws Exception {
        mvc.perform(get("/sse/subscribe").param("clientId", " ")).andExpect(status().isBadRequest());
        List<String> messages = listen(service.addClient(null, "one"));
        for (int i = 0; i < 8; i++) {
            mvc.perform(get("/sse/subscribe").param("userId", "user-a").param("clientId", "one"))
                    .andExpect(status().isForbidden());
        }
        listen(service.addClient(null, "two"));
        assertThrows(GenericException.class, () -> service.addClient(null, "three"));
        messages.clear();
        service.sendToUser("user-a", "org-a", "original connection");
        assertEquals(List.of("\"original connection\""), messages);
    }

    @ParameterizedTest
    @ValueSource(strings = {"other-organization", "expired", "empty"})
    @SuppressWarnings("unchecked")
    void fallsBackToCurrentOrganizationWhenCacheHasNoMatchingNotifications(String scenario) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        ZSetOperations<String, String> sorted = mock(ZSetOperations.class);
        SendModuleService modules = mock(SendModuleService.class);
        ExtNotificationMapper mapper = mock(ExtNotificationMapper.class);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "sendModuleService", modules);
        ReflectionTestUtils.setField(service, "extNotificationMapper", mapper);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(modules.getNoticeModules("org-a")).thenReturn(List.of("CUSTOMER"));
        when(sorted.range("msg_user:user-a", 0, -1))
                .thenReturn("empty".equals(scenario) ? Set.of() : Set.of("cached"));
        if ("other-organization".equals(scenario)) {
            when(values.get("msg_content:cached")).thenReturn(notification("user-a", "org-b", "other organization"));
        }
        NotificationDTO stored = new NotificationDTO();
        stored.setReceiver("user-a");
        stored.setOrganizationId("org-a");
        stored.setSubject("database notification");
        stored.setContent(new byte[0]);
        when(mapper.selectLastList("user-a", "org-a", List.of("CUSTOMER"))).thenReturn(List.of(stored));
        List<String> messages = listen(service.addClient(null, "client"));
        messages.clear();
        organization.when(OrganizationContext::getOrganizationId).thenReturn(null);
        service.broadcastPeriodically("user-a", NotificationConstants.Type.SYSTEM_NOTICE.toString());
        verify(mapper).selectLastList("user-a", "org-a", List.of("CUSTOMER"));
        assertEquals(1, messages.size());
        assertTrue(messages.getFirst().contains("database notification"));
        assertFalse(messages.getFirst().contains("other organization"));
    }
}
