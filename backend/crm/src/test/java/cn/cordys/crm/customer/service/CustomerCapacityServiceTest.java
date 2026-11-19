package cn.cordys.crm.customer.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.CustomerCapacity;
import cn.cordys.crm.customer.mapper.ExtCustomerCapacityMapper;
import cn.cordys.crm.system.dto.request.CapacityUpdateRequest;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerCapacityServiceTest {

    @Test
    void updateLogsCapacityBeforeAndAfterChange() {
        CustomerCapacityService service = new CustomerCapacityService();
        @SuppressWarnings("unchecked")
        BaseMapper<CustomerCapacity> mapper = mock(BaseMapper.class);
        ExtCustomerCapacityMapper updateMapper = mock(ExtCustomerCapacityMapper.class);
        ReflectionTestUtils.setField(service, "customerCapacityMapper", mapper);
        ReflectionTestUtils.setField(service, "extCustomerCapacityMapper", updateMapper);
        ReflectionTestUtils.setField(service, "userExtendService", mock(UserExtendService.class));

        CustomerCapacity original = new CustomerCapacity();
        original.setId("capacity-1");
        original.setCapacity(10);
        CustomerCapacity persisted = new CustomerCapacity();
        persisted.setId("capacity-1");
        persisted.setCapacity(20);
        when(mapper.selectByPrimaryKey("capacity-1")).thenReturn(original, persisted);

        CapacityUpdateRequest request = new CapacityUpdateRequest();
        request.setId("capacity-1");
        request.setScopeIds(List.of("sales"));
        request.setCapacity(20);

        OperationLogContext.putEmptySpan();
        try (MockedStatic<Translator> translator = mockStatic(Translator.class)) {
            translator.when(() -> Translator.get("module.customer.capacity.setting")).thenReturn("客户库容");

            service.update(request, "operator", "org");

            verify(updateMapper).updateCapacity(argThat(capacity -> "capacity-1".equals(capacity.getId())
                    && Integer.valueOf(20).equals(capacity.getCapacity())));
            LogContextInfo context = OperationLogContext.getContext();
            assertEquals(10, assertInstanceOf(CustomerCapacity.class, context.getOriginalValue()).getCapacity());
            assertEquals(20, assertInstanceOf(CustomerCapacity.class, context.getModifiedValue()).getCapacity());
        } finally {
            OperationLogContext.clear();
        }
    }
}
