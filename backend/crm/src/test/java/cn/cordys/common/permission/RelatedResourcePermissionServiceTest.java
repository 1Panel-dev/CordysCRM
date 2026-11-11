package cn.cordys.common.permission;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.customer.constants.CustomerCollaborationType;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.service.CustomerCollaborationService;
import cn.cordys.crm.system.mapper.ExtStatisticMapper;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatedResourcePermissionServiceTest {
    private static final String ORG = "org-example";
    private static final String CUSTOMER = "customer-example";
    private static final String CONTACT = "contact-example";
    private static final String BEFORE = "owner-before";
    private static final String AFTER = "owner-after";
    private static final String MEMBER = "collaborator";
    private final RelatedResourcePermissionService service = new RelatedResourcePermissionService();
    private final CustomerCollaborationService collaborations = new CustomerCollaborationService();
    private final ExtStatisticMapper fields = mock(ExtStatisticMapper.class);
    private final PermissionCache permissions = mock(PermissionCache.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> members = mock(BaseMapper.class);
    private String customerOwner = BEFORE;
    private String memberId = MEMBER;
    private String contactOwner = MEMBER;
    private CustomerCollaborationType type = CustomerCollaborationType.COLLABORATION;
    private Long expireTime;

    @BeforeEach
    void 配置真实协作类型降级与关联客户() {
        ReflectionTestUtils.setField(service, "customerCollaborationService", collaborations);
        ReflectionTestUtils.setField(service, "extStatisticMapper", fields);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", mock(ModuleFormCacheService.class));
        ReflectionTestUtils.setField(service, "permissionCache", permissions);
        ReflectionTestUtils.setField(collaborations, "customerCollaborationMapper", members);
        when(members.select(any(CustomerCollaboration.class))).thenAnswer(invocation -> {
            CustomerCollaboration member = new CustomerCollaboration();
            member.setCustomerId(CUSTOMER);
            member.setUserId(memberId);
            member.setCollaborationType(type.name());
            member.setExpireTime(expireTime);
            return List.of(member);
        });
        when(fields.selectBusinessFieldValue(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String table = invocation.getArgument(0);
            String column = invocation.getArgument(1);
            String id = invocation.getArgument(2);
            if ("customer".equals(table) && CUSTOMER.equals(id)) {
                return switch (column) {
                case "organization_id" -> ORG;
                case "owner" -> customerOwner;
                case "in_shared_pool" -> "false";
                default -> null;
                };
            }
            if ("customer_contact".equals(table) && CONTACT.equals(id)) {
                return switch (column) {
                case "organization_id" -> ORG;
                case "owner" -> contactOwner;
                case "customer_id" -> CUSTOMER;
                default -> null;
                };
            }
            return null;
        });
        when(permissions.getPermissionIds(anyString(), eq(ORG)))
                .thenReturn(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ));
    }

    @ParameterizedTest
    @CsvSource({
            "COLLABORATION,,true",
            "COLLABORATION,9223372036854775807,true",
            "READ_ONLY,,false",
            "READ_ONLY,1,false",
            "JOINT_FOLLOW,,false",
            "JOINT_FOLLOW,9223372036854775807,false",
            "JOINT_FOLLOW,1,true"
    })
    void 转移前后仅隔离有效类型为仅客户信息的成员数据(CustomerCollaborationType memberType, Long expiry,
            boolean isolated) {
        type = memberType;
        expireTime = expiry;
        for (String owner : List.of(BEFORE, AFTER)) {
            customerOwner = owner;
            assertEquals(isolated ? Set.of(MEMBER) : Set.of(), service.getIsolatedOwnerIds(CUSTOMER, owner, ORG));
            assertEquals(isolated, service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, owner, ORG));
            assertEquals(!isolated, service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, owner, ORG));
            if (isolated) {
                assertThrows(GenericException.class,
                        () -> service.checkIsolatedData(FormKey.CONTACT.getKey(), CONTACT, owner, ORG));
            } else {
                assertDoesNotThrow(() -> service.checkIsolatedData(FormKey.CONTACT.getKey(), CONTACT, owner, ORG));
            }
        }
    }

    @Test
    void 客户负责人自己的关联数据不隔离() {
        memberId = BEFORE;
        contactOwner = BEFORE;
        assertTrue(service.getIsolatedOwnerIds(CUSTOMER, BEFORE, ORG).isEmpty());
        assertFalse(service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, BEFORE, ORG));
        assertTrue(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, BEFORE, ORG));
    }

    @Test
    void 转移后旧负责人不再获得客户关联读取授权() {
        type = CustomerCollaborationType.READ_ONLY;
        customerOwner = AFTER;
        assertTrue(service.getIsolatedOwnerIds(CUSTOMER, BEFORE, ORG).isEmpty());
        assertFalse(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, BEFORE, ORG));
        assertTrue(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, AFTER, ORG));
    }

    @Test
    void 非当前负责人或异组织不能取得该客户的隔离成员及关联授权() {
        assertTrue(service.getIsolatedOwnerIds(CUSTOMER, "other-user", ORG).isEmpty());
        assertFalse(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, "other-user", ORG));
        assertTrue(service.getIsolatedOwnerIds(CUSTOMER, BEFORE, "other-org").isEmpty());
        assertFalse(service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, BEFORE, "other-org"));
        assertFalse(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, BEFORE, "other-org"));
    }
}
