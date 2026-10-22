package cn.cordys.common.service;

import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.Role;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.mapper.ExtRoleMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.RoleService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserQueryServiceTest {

    private final UserQueryService service = new UserQueryService();
    private final ExtUserMapper users = mock(ExtUserMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<User> userMapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<OrganizationUser> organizationUsers = mock(BaseMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "extUserMapper", users);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "organizationUserMapper", organizationUsers);
    }

    @Test
    void emptyInputsKeepTheirOriginalResultsWithoutQueries() {
        List<CustomerListResponse> empty = List.of();

        assertSame(empty, service.setCreateAndUpdateUserName(empty));
        assertNull(service.setCreateUpdateOwnerUserName((List<CustomerListResponse>) null));
        assertEquals(Map.of(), service.getUserNameMap((List<String>) null));
        assertEquals(Map.of(), service.getUserDeptMapByUserIds(Set.of(), "org-1"));
        assertNull(service.getUserName(" "));
        verifyNoInteractions(users, userMapper, organizationUsers);
    }

    @Test
    void fillsAuditAndOwnerNamesIncludingMissingUserFallback() {
        when(users.selectUserOptionByIds(anyList())).thenReturn(List.of(
                new OptionDTO("creator", "创建人"), new OptionDTO("owner", "负责人")));
        CustomerListResponse customer = new CustomerListResponse();
        customer.setCreateUser("creator");
        customer.setUpdateUser("removed-user");
        customer.setOwner("owner");

        try (MockedStatic<Translator> translator = mockStatic(Translator.class)) {
            translator.when(() -> Translator.get("common.option.not_exist")).thenReturn("已删除");

            assertSame(customer, service.setCreateAndUpdateOwnerUserName(customer));
            assertEquals("创建人", customer.getCreateUserName());
            assertEquals("已删除", customer.getUpdateUserName());
            assertEquals("负责人", customer.getOwnerName());
        }

        assertThrows(GenericException.class, () -> service.setCreateAndUpdateUserName(new Object()));
    }

    @Test
    void departmentQueriesRemainScopedToTheRequestedOrganization() {
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setDepartmentId("department-1");
        when(organizationUsers.selectOne(any(OrganizationUser.class)))
                .thenReturn(organizationUser);
        UserDeptDTO department = new UserDeptDTO("owner", "department-1", "销售部");
        when(users.getUserDeptByUserIds(List.of("owner"), "org-1")).thenReturn(List.of(department));

        assertEquals("department-1", service.getDeptIdByUserId("owner", "org-1"));
        assertSame(department, service.getUserDeptMapByUserId("owner", "org-1"));
        assertEquals(Map.of("owner", department), service.getUserDeptMapByUserIds(Set.of("owner"), "org-1"));

        ArgumentCaptor<OrganizationUser> criteria = ArgumentCaptor.forClass(OrganizationUser.class);
        verify(organizationUsers).selectOne(criteria.capture());
        assertEquals("owner", criteria.getValue().getUserId());
        assertEquals("org-1", criteria.getValue().getOrganizationId());
    }

    @Test
    void roleListsStillResolveAuditUserNamesThroughTheNarrowDependency() {
        RoleService roles = new RoleService();
        ExtRoleMapper roleMapper = mock(ExtRoleMapper.class);
        ReflectionTestUtils.setField(roles, "extRoleMapper", roleMapper);
        ReflectionTestUtils.setField(roles, "userQueryService", service);
        Role role = new Role();
        role.setId("role-1");
        role.setInternal(false);
        role.setCreateUser("user-1");
        role.setUpdateUser("user-1");
        when(roleMapper.selectList("org-1")).thenReturn(List.of(role));
        when(users.selectUserOptionByIds(List.of("user-1")))
                .thenReturn(List.of(new OptionDTO("user-1", "管理员")));

        var response = roles.list("org-1").getFirst();

        assertEquals("管理员", response.getCreateUserName());
        assertEquals("管理员", response.getUpdateUserName());
    }

    @Test
    void departmentPermissionsStillRejectOwnersOutsideTheAllowedScope() {
        DataScopeService scopes = spy(new DataScopeService());
        ReflectionTestUtils.setField(scopes, "userQueryService", service);
        DeptDataPermissionDTO permission = new DeptDataPermissionDTO();
        permission.setDeptIds(Set.of("allowed-department"));
        doReturn(permission).when(scopes).getDeptDataPermission("requester", "org-1", "CUSTOMER:READ");
        when(users.getUserDeptByUserIds(anyList(), eq("org-1")))
                .thenReturn(List.of(new UserDeptDTO("allowed-owner", "allowed-department", "销售部"),
                        new UserDeptDTO("other-owner", "other-department", "其他部门")));

        assertTrue(scopes.hasDataPermission("requester", "org-1", List.of("allowed-owner"), "CUSTOMER:READ"));
        assertFalse(scopes.hasDataPermission("requester", "org-1", List.of("other-owner"), "CUSTOMER:READ"));
        assertFalse(scopes.hasDataPermission("requester", "org-1", List.of("missing-owner"), "CUSTOMER:READ"));
        assertTrue(scopes.hasDataPermission("requester", "org-1", List.of("requester"), "CUSTOMER:READ"));
    }
}
