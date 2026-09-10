package cn.cordys.crm.contract;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractPaymentRecord;
import cn.cordys.crm.contract.service.ContractFieldService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BatchDeleteServiceTest {

    private MessageSource originalMessageSource;

    @BeforeEach
    void setUp() {
        originalMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        ReflectionTestUtils.setField(Translator.class, "messageSource", new StaticMessageSource());
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(Translator.class, "messageSource", originalMessageSource);
    }

    @Test
    @SuppressWarnings("unchecked")
    void contractBatchDeleteRejectsRelatedDataBeforeDeletion() {
        ContractService service = new ContractService();
        BaseMapper<Contract> contractMapper = mock(BaseMapper.class);
        BaseMapper<ContractPaymentRecord> paymentRecordMapper = mock(BaseMapper.class);
        ContractFieldService fieldService = mock(ContractFieldService.class);
        ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
        ReflectionTestUtils.setField(service, "contractMapper", contractMapper);
        ReflectionTestUtils.setField(service, "contractPaymentRecordMapper", paymentRecordMapper);
        ReflectionTestUtils.setField(service, "contractFieldService", fieldService);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);

        Contract contract = new Contract();
        contract.setId("related");
        when(contractMapper.selectByIds(anyList())).thenReturn(List.of(contract));
        when(approvalFlowService.filterResourcesWithPermission(anyString(), anyList(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of("related"));
        when(paymentRecordMapper.selectListByLambda(any())).thenReturn(List.of(new ContractPaymentRecord()));

        assertThrows(GenericException.class, () -> service.batchDelete(List.of("related"), "user", "org"));
        verify(fieldService, never()).deleteByResourceIds(anyList());
        verify(contractMapper, never()).deleteByIds(anyList());
    }
}
