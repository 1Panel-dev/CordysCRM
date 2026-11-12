package cn.cordys.common.permission;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.DeptDataPermissionDTO;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private boolean inSharedPool;

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
                case "in_shared_pool" -> String.valueOf(inSharedPool);
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
                .thenReturn(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ,
                        PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ));
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

    @ParameterizedTest
    @CsvSource({ "READ_ONLY,,false", "JOINT_FOLLOW,,false", "JOINT_FOLLOW,1,true", "COLLABORATION,,true" })
    void 协作标签数据范围及详情只读授权按有效类型判断(CustomerCollaborationType memberType, Long expiry,
            boolean selfOnly) {
        type = memberType;
        expireTime = expiry;
        DeptDataPermissionDTO scope = service.getCustomerTabDataPermission(CUSTOMER, MEMBER, ORG);
        assertEquals(selfOnly, scope.getSelf());
        assertEquals(!selfOnly, scope.getAll());
        contactOwner = BEFORE;
        assertEquals(selfOnly, service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        assertEquals(!selfOnly, service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        contactOwner = MEMBER;
        assertFalse(service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        assertTrue(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
    }

    @ParameterizedTest
    @CsvSource({ "READ_ONLY", "JOINT_FOLLOW" })
    void 全量协作成员排除其他私有成员数据并保留当前客户负责人(CustomerCollaborationType memberType) {
        CustomerCollaboration viewer = new CustomerCollaboration();
        viewer.setUserId(MEMBER);
        viewer.setCollaborationType(memberType.name());
        CustomerCollaboration isolated = new CustomerCollaboration();
        isolated.setUserId("private-member");
        isolated.setCollaborationType(CustomerCollaborationType.COLLABORATION.name());
        CustomerCollaboration owner = new CustomerCollaboration();
        owner.setUserId(BEFORE);
        owner.setCollaborationType(CustomerCollaborationType.COLLABORATION.name());
        when(members.select(any(CustomerCollaboration.class))).thenReturn(List.of(viewer, isolated, owner));
        assertEquals(Set.of("private-member"), service.getCustomerTabDataPermission(CUSTOMER, MEMBER, ORG)
                .getExcludedOwnerIds());
        contactOwner = "private-member";
        assertTrue(service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        assertFalse(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        contactOwner = BEFORE;
        assertFalse(service.isIsolatedData(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        assertTrue(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
    }

    @Test
    void 标签范围不授权无关系异组织或公海资源() {
        assertNull(service.getCustomerTabDataPermission(CUSTOMER, "unrelated", ORG));
        assertNull(service.getCustomerTabDataPermission(CUSTOMER, MEMBER, "other-org"));
        inSharedPool = true;
        assertNull(service.getCustomerTabDataPermission(CUSTOMER, MEMBER, ORG));
        assertNull(service.getCustomerTabDataPermission(CUSTOMER, BEFORE, ORG));
    }

    @Test
    void 无负责人报价单不按创建人隔离() {
        assertFalse(service.isIsolatedData(FormKey.QUOTATION.getKey(), "quotation", MEMBER, ORG));
    }

    @Test
    void 协作关系不替代关联表单查看功能权限() {
        type = CustomerCollaborationType.READ_ONLY;
        contactOwner = BEFORE;
        when(permissions.getPermissionIds(anyString(), eq(ORG)))
                .thenReturn(Set.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ));
        assertFalse(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, MEMBER, ORG));
        assertTrue(service.hasRelatedReadPermission(FormKey.CONTACT.getKey(), CONTACT, BEFORE, ORG));
    }
}
