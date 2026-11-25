package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import cn.cordys.crm.system.domain.UserExtend;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomerCollaborationServiceTest {
    private final CustomerCollaborationService service = new CustomerCollaborationService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> mapper = mock(BaseMapper.class);
    @SuppressWarnings("unchecked")
    private final BaseMapper<UserExtend> userExtendMapper = mock(BaseMapper.class);
    private final BaseService baseService = mock(BaseService.class);
    private final UserExtendService userExtendService = new UserExtendService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "customerCollaborationMapper", mapper);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        ReflectionTestUtils.setField(service, "userExtendService", userExtendService);
        ReflectionTestUtils.setField(userExtendService, "userExtendMapper", userExtendMapper);
        when(baseService.getUserDeptMapByUserIds(anyList(), eq("org")))
                .thenReturn(Map.of("member", new UserDeptDTO("member", "dept", "部门")));
        when(baseService.getUserNameMap(anySet())).thenReturn(Map.of("member", "协作人", "operator", "操作人"));
        UserExtend member = new UserExtend();
        member.setId("member");
        member.setAvatar("https://example.com/avatar.png");
        when(userExtendMapper.selectByIds(List.of("member"))).thenReturn(List.of(member));
    }

    @ParameterizedTest
    @CsvSource({
            "READ_ONLY,,READ_ONLY", "READ_ONLY,9223372036854775807,READ_ONLY", "READ_ONLY,1,COLLABORATION",
            "JOINT_FOLLOW,,JOINT_FOLLOW", "JOINT_FOLLOW,9223372036854775807,JOINT_FOLLOW",
            "JOINT_FOLLOW,1,COLLABORATION", "COLLABORATION,,COLLABORATION",
            "COLLABORATION,9223372036854775807,COLLABORATION", "COLLABORATION,1,COLLABORATION"
    })
    void listAndPermissionLookupsUseEffectiveCollaborationType(String type, Long expireTime, String expectedType) {
        when(mapper.select(any(CustomerCollaboration.class))).thenAnswer(invocation ->
                List.of(collaboration(type, expireTime)));

        var listed = service.list("customer", "org").getFirst();
        assertEquals(expectedType, listed.getCollaborationType());
        assertEquals(expireTime, listed.getExpireTime());
        assertEquals("协作人", listed.getUserName());
        assertEquals("https://example.com/avatar.png", listed.getAvatar());
        assertEquals("部门", listed.getDepartmentName());
        assertEquals(expectedType, service.selectByCustomerId("customer").getFirst().getCollaborationType());
        assertEquals(expectedType, service.selectByCustomerIdAndUserId("customer", "member")
                .getFirst().getCollaborationType());
        verify(mapper, never()).update(any(CustomerCollaboration.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "READ_ONLY", "JOINT_FOLLOW" })
    void expirationAtCurrentTimeIsAlreadyDowngraded(String type) {
        long expireTime = System.currentTimeMillis();
        when(mapper.select(any(CustomerCollaboration.class))).thenReturn(List.of(collaboration(type, expireTime)));

        assertEquals("COLLABORATION", service.selectByCustomerId("customer").getFirst().getCollaborationType());
    }

    @ParameterizedTest
    @ValueSource(strings = { "owner", "operator" })
    void rejectsAddingCustomerOwnerAsCollaborator(String operator) {
        @SuppressWarnings("unchecked")
        BaseMapper<Customer> customers = mock(BaseMapper.class);
        CommonNoticeSendService notices = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(service, "customerMapper", customers);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", notices);
        Customer customer = new Customer();
        customer.setOwner("owner");
        customer.setInSharedPool(false);
        when(customers.selectByPrimaryKey("customer")).thenReturn(customer);
        var request = new CustomerCollaborationAddRequest();
        request.setCustomerId("customer");
        request.setUserId("owner");
        request.setCollaborationType("READ_ONLY");

        try (var translator = mockStatic(Translator.class); var ids = mockStatic(IDGenerator.class)) {
            translator.when(() -> Translator.get("customer.collaboration.owner"))
                    .thenReturn("客户负责人不能添加为协作成员");
            ids.when(IDGenerator::nextStr).thenReturn("collaboration");
            var exception = assertThrows(GenericException.class, () -> service.add(request, operator, "org"));
            assertEquals(CrmHttpResultCode.VALIDATE_FAILED, exception.getErrorCode());
            assertEquals("客户负责人不能添加为协作成员", exception.getMessage());
        }
        verifyNoInteractions(mapper, notices);
    }

    private CustomerCollaboration collaboration(String type, Long expireTime) {
        CustomerCollaboration collaboration = new CustomerCollaboration();
        collaboration.setCustomerId("customer");
        collaboration.setUserId("member");
        collaboration.setCollaborationType(type);
        collaboration.setExpireTime(expireTime);
        collaboration.setCreateUser("operator");
        collaboration.setUpdateUser("operator");
        return collaboration;
    }
}
