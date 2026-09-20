package cn.cordys.security;

import cn.cordys.common.util.CodingUtils;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionUtilsTest {

    @Test
    void shouldEncryptPurposeBoundSessionIdWithRandomIvForFileAccess() {
        String originalSecret = SessionUser.secret;
        SessionUser.secret = "0123456789abcdef";
        try {
            String sessionId = "test-session-id";
            String firstToken = FileAccessTokenUtils.generateToken(sessionId);
            String secondToken = FileAccessTokenUtils.generateToken(sessionId);

            assertNotEquals(firstToken, secondToken);
            assertEquals(sessionId, FileAccessTokenUtils.getSessionId(firstToken));
            String wrongPurposeToken = encryptPayload("other-purpose|" + sessionId);
            assertNull(FileAccessTokenUtils.getSessionId(wrongPurposeToken));
        } finally {
            SessionUser.secret = originalSecret;
        }
    }

    @Test
    void shouldOnlyAcceptAuthenticatedMatchingSession() {
        MapSession session = new MapSession();
        SessionUser user = new SessionUser();
        user.setId("user-1");
        user.setSessionId(session.getId());
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
    }

    private static String encryptPayload(String payload) {
        byte[] iv = CodingUtils.generateIv();
        byte[] ciphertext = Base64.decodeBase64(CodingUtils.aesEncrypt(payload, SessionUser.secret, iv));
        byte[] token = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, token, 0, iv.length);
        System.arraycopy(ciphertext, 0, token, iv.length, ciphertext.length);
        return Base64.encodeBase64URLSafeString(token);
    }
}
