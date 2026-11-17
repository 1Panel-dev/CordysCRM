package cn.cordys.crm.customer.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.crm.customer.domain.CustomerCollaboration;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerCollaborationServiceTest {
    private final CustomerCollaborationService service = new CustomerCollaborationService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<CustomerCollaboration> mapper = mock(BaseMapper.class);
    private final BaseService baseService = mock(BaseService.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "customerCollaborationMapper", mapper);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        when(baseService.getUserDeptMapByUserIds(anyList(), eq("org")))
                .thenReturn(Map.of("member", new UserDeptDTO("member", "dept", "部门")));
        when(baseService.getUserNameMap(anySet())).thenReturn(Map.of("member", "协作人", "operator", "操作人"));
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
