package cn.cordys.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppListenerTest {

    @Test
    void shouldRejectUnsafeSecret() {
        assertThrows(IllegalStateException.class, () -> AppListener.validateSecret(null));
        assertThrows(IllegalStateException.class, () -> AppListener.validateSecret("9a9rdqPlTqhpZzkq"));
        assertThrows(IllegalStateException.class, () -> AppListener.validateSecret("too-short"));
        assertDoesNotThrow(() -> AppListener.validateSecret("0123456789abcdef"));
    }
}
