package cn.cordys.crm.customer.service;

import cn.cordys.crm.customer.domain.CustomerPool;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PoolCustomerMembershipTest {

    private final PoolCustomerService service = new PoolCustomerService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerPool> poolMapper = mock(BaseMapper.class);
    private final UserExtendService userExtendService = mock(UserExtendService.class);
    private final CustomerPool pool = new CustomerPool();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "poolMapper", poolMapper);
        ReflectionTestUtils.setField(service, "userExtendService", userExtendService);
        pool.setId("pool-id");
        pool.setOrganizationId("org-id");
        pool.setScopeId("[\"member-scope\"]");
        pool.setOwnerId("[\"manager-scope\"]");
        when(poolMapper.selectByPrimaryKey(pool.getId())).thenReturn(pool);
        when(userExtendService.getScopeOwnerIds(List.of("member-scope"), "org-id")).thenReturn(List.of("member"));
        when(userExtendService.getScopeOwnerIds(List.of("manager-scope"), "org-id")).thenReturn(List.of("manager"));
    }

    @ParameterizedTest
    @CsvSource({ "member,true", "manager,true", "outsider,false", "admin,true" })
    void accessIncludesMembersAndAdministrators(String userId, boolean expected) {
        assertEquals(expected, service.hasPoolMember(pool.getId(), userId, "org-id"));
        verify(poolMapper).selectByPrimaryKey(pool.getId());
    }

    @Test
    void missingPoolHasNoAccess() {
        assertFalse(service.hasPoolMember("missing-pool", "admin", "org-id"));
        assertFalse(service.hasPoolMember(null, "admin", "org-id"));
        assertFalse(service.hasPoolMember("", "admin", "org-id"));
        verifyNoInteractions(userExtendService);
    }

    @Test
    void poolInAnotherOrganizationHasNoAccessEvenForAdministrator() {
        assertFalse(service.hasPoolMember(pool.getId(), "admin", "another-org"));
        verifyNoInteractions(userExtendService);
    }

    @Test
    void missingOrganizationHasNoAccess() {
        assertFalse(service.hasPoolMember(pool.getId(), "admin", null));
        assertFalse(service.hasPoolMember(pool.getId(), "admin", ""));
        verifyNoInteractions(userExtendService);
    }

    @Test
    void existingMemberCheckUsesOnePoolQuery() {
        assertDoesNotThrow(() -> service.checkPoolMember(pool.getId(), "member", "org-id"));
        verify(poolMapper).selectByPrimaryKey(pool.getId());
    }
}
