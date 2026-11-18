package cn.cordys.config;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.security.SessionUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionTokenKeyInitializerTest {

    private static final String REDIS_KEY = "cordys:security:session-token-key";
    private static final String EXISTING_KEY = "0123456789abcdef0123456789abcdef";

    private String originalSecret;
    private ValueOperations<String, String> values;
    private SessionTokenKeyInitializer initializer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        originalSecret = SessionUser.secret;
        SessionUser.secret = null;
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        initializer = new SessionTokenKeyInitializer(redis);
    }

    @AfterEach
    void restoreSecret() {
        SessionUser.secret = originalSecret;
    }

    @Test
    void generatesAndReusesPersistedKeyAcrossInitializations() {
        AtomicReference<String> stored = new AtomicReference<>();
        when(values.get(REDIS_KEY)).thenAnswer(invocation -> stored.get());
        when(values.setIfAbsent(eq(REDIS_KEY), anyString()))
                .thenAnswer(invocation -> stored.compareAndSet(null, invocation.getArgument(1)));

        initializer.initialize();

        String firstKey = SessionUser.secret;
        assertNotNull(firstKey);
        assertTrue(firstKey.matches("[0-9a-f]{32}"));
        assertEquals(stored.get(), firstKey);
        byte[] iv = CodingUtils.generateIv();
        String ciphertext = CodingUtils.aesEncrypt("synthetic-session", firstKey, iv);

        SessionUser.secret = null;
        initializer.initialize();

        assertEquals(firstKey, SessionUser.secret);
        assertEquals("synthetic-session", CodingUtils.aesDecrypt(ciphertext, SessionUser.secret, iv));
    }

    @Test
    void reusesExistingKeyWithoutReplacingIt() {
        when(values.get(REDIS_KEY)).thenReturn(EXISTING_KEY);

        initializer.initialize();

        assertEquals(EXISTING_KEY, SessionUser.secret);
        verify(values, never()).setIfAbsent(anyString(), anyString());
    }

    @Test
    void readsWinningKeyWhenAnotherNodeInitializesConcurrently() {
        when(values.get(REDIS_KEY)).thenReturn(null, EXISTING_KEY);
        when(values.setIfAbsent(eq(REDIS_KEY), anyString())).thenReturn(false);

        initializer.initialize();

        assertEquals(EXISTING_KEY, SessionUser.secret);
        verify(values).setIfAbsent(eq(REDIS_KEY), anyString());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "9a9rdqPlTqhpZzkq", "invalid-key", "0123456789abcdef0123456789abcdef00" })
    void refusesMissingOrInvalidPersistedKeys(String key) {
        when(values.get(REDIS_KEY)).thenReturn(key);

        assertThrows(IllegalStateException.class, initializer::initialize);
        assertNull(SessionUser.secret);
    }

    @Test
    void refusesToStartWhenRedisIsUnavailable() {
        when(values.get(REDIS_KEY)).thenThrow(new RedisConnectionFailureException("synthetic failure"));

        assertThrows(RedisConnectionFailureException.class, initializer::initialize);
        assertNull(SessionUser.secret);
    }
}
