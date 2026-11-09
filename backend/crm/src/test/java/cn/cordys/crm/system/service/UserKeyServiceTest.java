package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.UserKey;
import cn.cordys.crm.system.dto.request.UserKeyUpdateRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.security.SessionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class UserKeyServiceTest {
    private final UserKeyService service = new UserKeyService();
    private final BaseMapper<UserKey> mapper = mock(BaseMapper.class);
    private MockedStatic<SessionUtils> session;
    private UserKey key;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userKeyMapper", mapper);
        session = mockStatic(SessionUtils.class);
        session.when(SessionUtils::getUserId).thenReturn("owner");
        key = new UserKey();
        key.setId("key-id");
        key.setCreateUser("owner");
        key.setEnable(false);
        key.setForever(false);
        key.setExpireTime(1L);
        when(mapper.selectByPrimaryKey("key-id")).thenReturn(key);
    }

    @AfterEach
    void tearDown() {
        session.close();
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void ownerCanManageTheirOwnKey(Operation operation) {
        operate(operation);

        verify(mapper).selectByPrimaryKey("key-id");
        if (operation == Operation.DELETE) {
            verify(mapper).deleteByPrimaryKey("key-id");
        } else {
            verify(mapper).updateById(key);
            if (operation == Operation.UPDATE) {
                assertTrue(key.getForever());
                assertEquals(null, key.getExpireTime());
                assertEquals("updated", key.getDescription());
            } else {
                assertEquals(operation == Operation.ENABLE, key.getEnable());
            }
        }
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @CsvSource({ "DELETE,other", "ENABLE,other", "DISABLE,other", "UPDATE,other",
            "DELETE,admin", "ENABLE,admin", "DISABLE,admin", "UPDATE,admin" })
    void otherUsersCannotManageTheKey(Operation operation, String userId) {
        session.when(SessionUtils::getUserId).thenReturn(userId);

        assertForbiddenWithoutWriting(operation);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void missingIdentityCannotMatchAnOwnerlessKey(Operation operation) {
        session.when(SessionUtils::getUserId).thenReturn(null);
        key.setCreateUser(null);

        assertForbiddenWithoutWriting(operation);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void missingKeyKeepsTheExistingError(Operation operation) {
        when(mapper.selectByPrimaryKey("key-id")).thenReturn(null);
        try (var translations = mockStatic(Translator.class)) {
            translations.when(() -> Translator.get("api_key_not_exist")).thenReturn("API key does not exist");
            GenericException exception = assertThrows(GenericException.class, () -> operate(operation));
            assertEquals("API key does not exist", exception.getMessage());
        }
        verify(mapper).selectByPrimaryKey("key-id");
        verifyNoMoreInteractions(mapper);
    }

    private void assertForbiddenWithoutWriting(Operation operation) {
        GenericException exception = assertThrows(GenericException.class, () -> operate(operation));
        assertEquals(CrmHttpResultCode.FORBIDDEN, exception.getErrorCode());
        verify(mapper).selectByPrimaryKey("key-id");
        verifyNoMoreInteractions(mapper);
    }

    private void operate(Operation operation) {
        switch (operation) {
        case DELETE -> service.deleteUserKey("key-id");
        case ENABLE -> service.enableUserKey("key-id");
        case DISABLE -> service.disableUserKey("key-id");
        case UPDATE -> {
            UserKeyUpdateRequest request = new UserKeyUpdateRequest();
            request.setId("key-id");
            request.setForever(true);
            request.setDescription("updated");
            service.updateUserKey(request);
        }
        }
    }

    private enum Operation {
        DELETE, ENABLE, DISABLE, UPDATE
    }
}
