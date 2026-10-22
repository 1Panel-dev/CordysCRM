package cn.cordys.common.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.ExportDTO;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.clue.service.ClueExportService;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.customer.dto.response.CustomerListResponse;
import cn.cordys.crm.customer.mapper.ExtCustomerMapper;
import cn.cordys.crm.customer.service.CustomerExportService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.opportunity.service.OpportunityExportService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.system.excel.domain.MergeResult;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportApprovalPaginationTest {

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void clueKeepsQueryCountBeforeApprovalFiltering(int visibleCount) throws Exception {
        ClueExportService service = new ClueExportService();
        CluePageRequest request = new CluePageRequest();
        ClueListResponse allowed = new ClueListResponse();
        allowed.setId("allowed");
        ClueListResponse blocked = new ClueListResponse();
        blocked.setId("blocked");
        ExtClueMapper mapper = mock(ExtClueMapper.class);
        when(mapper.list(request, "org", "user", null, false)).thenReturn(List.of(allowed, blocked));
        ClueService business = mock(ClueService.class);
        when(business.buildListData(anyList(), eq("org"))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "extClueMapper", mapper);
        ReflectionTestUtils.setField(service, "clueService", business);

        assertCounts(service, request, FormKey.CLUE, visibleCount, allowed);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void customerKeepsQueryCountBeforeApprovalFiltering(int visibleCount) throws Exception {
        CustomerExportService service = new CustomerExportService();
        CustomerPageRequest request = new CustomerPageRequest();
        CustomerListResponse allowed = new CustomerListResponse();
        allowed.setId("allowed");
        CustomerListResponse blocked = new CustomerListResponse();
        blocked.setId("blocked");
        ExtCustomerMapper mapper = mock(ExtCustomerMapper.class);
        when(mapper.list(request, "org", "user", null)).thenReturn(List.of(allowed, blocked));
        CustomerService business = mock(CustomerService.class);
        when(business.buildListData(anyList(), eq("org"))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "extCustomerMapper", mapper);
        ReflectionTestUtils.setField(service, "customerService", business);

        assertCounts(service, request, FormKey.CUSTOMER, visibleCount, allowed);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void opportunityKeepsQueryCountBeforeApprovalFiltering(int visibleCount) throws Exception {
        OpportunityExportService service = new OpportunityExportService();
        OpportunityPageRequest request = new OpportunityPageRequest();
        OpportunityListResponse allowed = new OpportunityListResponse();
        allowed.setId("allowed");
        OpportunityListResponse blocked = new OpportunityListResponse();
        blocked.setId("blocked");
        ExtOpportunityMapper mapper = mock(ExtOpportunityMapper.class);
        when(mapper.list(request, "org", "user", null, false)).thenReturn(List.of(allowed, blocked));
        OpportunityService business = mock(OpportunityService.class);
        when(business.buildListData(anyList(), eq("org"))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "extOpportunityMapper", mapper);
        ReflectionTestUtils.setField(service, "opportunityService", business);

        assertCounts(service, request, FormKey.OPPORTUNITY, visibleCount, allowed);
    }

    private void assertCounts(BaseExportService original, BasePageRequest request, FormKey formKey,
            int visibleCount, Object allowed) throws Exception {
        request.setCurrent(1);
        request.setPageSize(BaseExportService.EXPORT_MAX_COUNT);
        ExportDTO dto = ExportDTO.builder().orgId("org").userId("user").pageRequest(request).build();
        dto.getExtraParams().put("stageConfigMap", Map.of());
        ApprovalFlowService approvals = mock(ApprovalFlowService.class);
        when(approvals.filterResourcesWithExportPermission(eq(formKey.getKey()), anyList(), eq("org"), any(), any()))
                .thenReturn(visibleCount == 0 ? List.of() : List.of("allowed"));
        ReflectionTestUtils.setField(original, "approvalFlowService", approvals);
        BaseExportService service = spy(original);
        doReturn(Map.of()).when(service).getFieldConfigMap(formKey.getKey(), "org");
        doAnswer(invocation -> {
            List<?> exported = invocation.getArgument(2);
            assertEquals(List.of(allowed), exported);
            return MergeResult.builder().dataList(List.of()).mergeRegions(List.of()).handleCount(exported.size()).build();
        }).when(service).buildExportMergeResult(eq("task"), eq(dto), anyList(), any(), any());

        MergeResult result = service.getExportMergeData("task", dto);

        assertEquals(2, result.getQueryCount());
        assertEquals(visibleCount, result.getHandleCount());
        verify(approvals).filterResourcesWithExportPermission(eq(formKey.getKey()), anyList(), eq("org"), any(), any());
        verify(service, times(visibleCount)).buildExportMergeResult(eq("task"), eq(dto), anyList(), any(), any());
    }
}
