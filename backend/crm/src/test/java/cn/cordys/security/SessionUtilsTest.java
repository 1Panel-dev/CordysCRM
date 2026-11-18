package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.EncryptUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUtilsTest {

    private String originalSecret;

    @BeforeEach
    void setUpSecret() {
        originalSecret = SessionUser.secret;
        SessionUser.secret = "0123456789abcdef";
    }

    @AfterEach
    void restoreSecret() {
        SessionUser.secret = originalSecret;
    }

    @Test
    void shouldEncryptPurposeBoundSessionIdWithRandomIvForFileAccess() {
        String sessionId = "test-session-id";
        String firstToken = FileAccessTokenUtils.generateToken(sessionId);
        String secondToken = FileAccessTokenUtils.generateToken(sessionId);

        assertNotEquals(firstToken, secondToken);
        assertEquals(sessionId, FileAccessTokenUtils.getSessionId(firstToken));
        String wrongPurposeToken = CodingUtils.encryptToken("CSRF|user-1|" + sessionId, SessionUser.secret);
        assertNull(FileAccessTokenUtils.getSessionId(wrongPurposeToken));
        assertThrows(RuntimeException.class, () -> CodingUtils.decryptToken(firstToken, "9a9rdqPlTqhpZzkq"));
    }

    @Test
    void rejectsForgedLegacyAndTamperedFileTokens() {
        byte[] iv = CodingUtils.generateIv();
        byte[] ciphertext = Base64.decodeBase64(
                CodingUtils.aesEncrypt("F_A|test-session-id", "9a9rdqPlTqhpZzkq", iv));
        byte[] legacy = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, legacy, 0, iv.length);
        System.arraycopy(ciphertext, 0, legacy, iv.length, ciphertext.length);
        assertNull(FileAccessTokenUtils.getSessionId(Base64.encodeBase64URLSafeString(legacy)));
        assertNull(FileAccessTokenUtils.getSessionId(CodingUtils.TOKEN_PREFIX
                + Base64.encodeBase64URLSafeString(legacy)));

        String token = FileAccessTokenUtils.generateToken("test-session-id");
        byte[] bytes = Base64.decodeBase64(token.substring(CodingUtils.TOKEN_PREFIX.length()));
        bytes[bytes.length - 1] ^= 1;
        assertNull(FileAccessTokenUtils.getSessionId(CodingUtils.TOKEN_PREFIX
                + Base64.encodeBase64URLSafeString(bytes)));
        assertNull(FileAccessTokenUtils.getSessionId("v2.invalid!"));
        assertNull(FileAccessTokenUtils.getSessionId("v2.AA"));
    }

    @Test
    void historicalDataStillDecryptsAfterSessionKeyChanges() {
        // 固定样本来自修复前的业务数据加密方式，不能用新加密结果代替。
        String ciphertext = "MSy0Qh+Z1vd0kJ0242pp+rtTSLQzhUyfuXCj0UwsTa4=";
        assertEquals("historical-value", EncryptUtils.aesDecrypt(ciphertext));
        SessionUser.secret = CodingUtils.generateSecretKey();
        assertEquals("historical-value", EncryptUtils.aesDecrypt(ciphertext));
    }

    @Test
    void shouldOnlyAcceptAuthenticatedMatchingSession() {
        MapSession session = new MapSession();
        SessionUser user = new SessionUser();
        user.setId("user-1");
        user.setSessionId(session.getId());
        user.setCsrfToken(CodingUtils.encryptToken("CSRF|user-1|" + session.getId(), SessionUser.secret));
        session.setAttribute(SessionConstants.ATTR_USER, user);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.getId());

        assertTrue(SessionUtils.isAuthenticatedSession(session.getId(), session));

        session.removeAttribute(SessionConstants.ATTR_USER);
        assertFalse(SessionUtils.isAuthenticatedSession(session.getId(), session));

        session.setAttribute(SessionConstants.ATTR_USER, user);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, "other-user");
        assertFalse(SessionUtils.isAuthenticatedSession(session.getId(), session));
        assertFalse(SessionUtils.isAuthenticatedSession("other-session", session));
        assertFalse(SessionUtils.isAuthenticatedSession(session.getId(), null));

        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.getId());
        user.setCsrfToken(CodingUtils.aesEncrypt("user-1|random|" + session.getId() + "|0",
                "9a9rdqPlTqhpZzkq", CodingUtils.generateLegacyIv()));
        assertTrue(SessionUtils.isAuthenticatedSession(session.getId(), session));
    }
}
