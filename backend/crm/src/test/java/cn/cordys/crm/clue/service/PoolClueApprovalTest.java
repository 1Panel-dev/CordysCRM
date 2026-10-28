package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.annotation.HitApproval;
import cn.cordys.crm.approval.constants.ExecuteTimingEnum;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 线索池接入审批流: 批量编辑走编辑审批、删除/批量删除走删除审批, 领取与分配不接审批。
 */
class PoolClueApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String FIELD_ID = "field-1";

    private final PoolClueService service = new PoolClueService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    private final ClueFieldService clueFieldService = mock(ClueFieldService.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final StatisticFieldService statisticFieldService = mock(StatisticFieldService.class);
    private final LogService logService = mock(LogService.class);
    private final ExtClueMapper extClueMapper = mock(ExtClueMapper.class);
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 返回值要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(service, "clueFieldService", clueFieldService);
        ReflectionTestUtils.setField(service, "extClueMapper", extClueMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "statisticFieldService", statisticFieldService);
        ReflectionTestUtils.setField(service, "logService", logService);
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void singleDeleteEntryPointIsGuardedByTheDeleteApprovalAnnotation() throws Exception {
        HitApproval hitApproval = PoolClueService.class
                .getMethod("deleteWithApprovalCheck", String.class, String.class, String.class)
                .getAnnotation(HitApproval.class);

        assertEquals(FormKey.CLUE, hitApproval.formKey());
        assertEquals(ExecuteTimingEnum.DELETE, hitApproval.executeType());
        assertEquals("{#id}", hitApproval.resourceId());
        assertEquals("{#userId}", hitApproval.operatorId());
    }

    @Test
    void singleDeleteEntryPointDelegatesToThePlainDeleteThroughTheProxy() throws Exception {
        // 切面在单测里不生效, 这里只钉住「入口经代理委托给原有的 delete」:
        // 自调用 bypass 代理, delete 上的 @OperationLog 会失效, 所以必须从容器取代理对象
        PoolClueService spy = spy(service);
        doNothing().when(spy).delete(anyString());

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(PoolClueService.class)).thenReturn(spy);
            spy.deleteWithApprovalCheck("clue-1", USER_ID, ORG_ID);
        }

        verify(spy).delete("clue-1");
        // 触发点必须留在入口上, 原方法不能重复触发 (deleteCustomerResource 那种共用出口会重复提审)
        assertNull(PoolClueService.class.getMethod("delete", String.class).getAnnotation(HitApproval.class));
    }

    @Test
    void batchEditOnANonOwnerFieldTriggersTheEditApproval() {
        BaseField field = field("remark", "备注");
        when(clueFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field);
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("clue-1"));

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("clue-1", "clue-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        // 只有有权限的那条被提审, 也只有它被写库
        verify(approvalResourceService).batchEditTriggerApproval(
                eq(List.of("clue-1")), eq(FIELD_ID), eq(FormKey.CLUE), eq(ORG_ID), eq(USER_ID), eq("备注"), eq("value"));
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
        assertEquals(0, response.getSkip());
    }

    @Test
    void batchEditOnTheOwnerFieldGoesToBatchAssignWithoutApproval() {
        BaseField field = field("owner", "负责人");
        when(clueFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field);
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("clue-1", "clue-2"));

        PoolClueService spy = spy(service);
        ReflectionTestUtils.setField(spy, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(spy, "clueFieldService", clueFieldService);
        ReflectionTestUtils.setField(spy, "approvalFlowService", approvalFlowService);
        doNothing().when(spy).batchAssign(any(), anyString(), anyString(), anyString());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("clue-1", "clue-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("new-owner");

        BatchAffectReasonResponse response;
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            response = spy.batchUpdate(request, USER_ID, ORG_ID);
        }

        // 改负责人就是一次分配, 分配不接审批
        ArgumentCaptor<PoolBatchAssignRequest> assignRequest = ArgumentCaptor.forClass(PoolBatchAssignRequest.class);
        verify(spy).batchAssign(assignRequest.capture(), eq("new-owner"), eq(ORG_ID), eq(USER_ID));
        assertEquals(List.of("clue-1", "clue-2"), assignRequest.getValue().getBatchIds());
        verify(approvalResourceService, never()).batchEditTriggerApproval(
                anyList(), anyString(), any(), anyString(), anyString(), anyString(), any());
        assertEquals(2, response.getSuccess());
        assertEquals(0, response.getFail());
    }

    @Test
    void batchEditWithoutAnyPermittedResourceDoesNotWrite() {
        when(clueFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("remark", "备注"));
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("clue-1"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        verify(approvalResourceService, never()).batchEditTriggerApproval(
                anyList(), anyString(), any(), anyString(), anyString(), anyString(), any());
        verify(clueFieldService, never()).batchUpdate(any(), any(), anyList(), any(), anyString(), any(), anyString(),
                anyString(), eq(true));
        assertEquals(0, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    @Test
    void batchEditOnUnknownIdsReportsThemAsFailed() {
        when(clueFieldService.getAndCheckField(FIELD_ID, ORG_ID)).thenReturn(field("remark", "备注"));
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of());

        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        request.setIds(List.of("clue-1", "clue-2"));
        request.setFieldId(FIELD_ID);
        request.setFieldValue("value");

        BatchAffectReasonResponse response = runEdit(request);

        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
        verify(approvalFlowService, never()).filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void batchDeleteSkipsTheResourcesThatWerePushedToApproval() {
        when(clueMapper.selectByIds(anyList()))
                .thenReturn(List.of(clue("clue-1"), clue("clue-2"), clue("clue-3")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("clue-1", "clue-2"));
        when(approvalResourceService.batchDeleteTriggerApproval(anyList(), any(), anyString(), anyString(), anyMap()))
                .thenReturn(List.of("clue-2"));

        BatchAffectReasonResponse response = runDelete(List.of("clue-1", "clue-2", "clue-3"));

        // 进了审批的 clue-2 不能被真删掉, 统计字段重算也只覆盖真正删掉的那些
        verify(statisticFieldService).captureRelatedHosts(FormKey.CLUE.getKey(), List.of("clue-1"), ORG_ID);
        verify(clueMapper).deleteByLambda(any());
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(1, response.getFail());
        assertEquals("batch.operate.reason", response.getErrorMessages());
    }

    @Test
    void batchDeleteWithEverythingPushedToApprovalTouchesNoData() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of("clue-1"));
        when(approvalResourceService.batchDeleteTriggerApproval(anyList(), any(), anyString(), anyString(), anyMap()))
                .thenReturn(List.of("clue-1"));

        BatchAffectReasonResponse response = runDelete(List.of("clue-1"));

        verify(clueMapper, never()).deleteByLambda(any());
        verify(statisticFieldService, never()).captureRelatedHosts(anyString(), anyList(), anyString());
        verify(logService, never()).batchAdd(anyList());
        assertEquals(0, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(0, response.getFail());
    }

    @Test
    void batchDeleteWithoutAnyPermittedResourceDoesNotDelete() {
        when(clueMapper.selectByIds(anyList())).thenReturn(List.of(clue("clue-1")));
        when(approvalFlowService.filterResourcesWithPermission(
                anyString(), anyList(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(List.of());

        BatchAffectReasonResponse response = runDelete(List.of("clue-1"));

        verify(approvalResourceService, never()).batchDeleteTriggerApproval(anyList(), any(), anyString(), anyString(),
                anyMap());
        verify(clueMapper, never()).deleteByLambda(any());
        assertEquals(0, response.getSuccess());
        assertEquals(1, response.getFail());
        assertEquals("no.operation.permission", response.getErrorMessages());
    }

    /**
     * 审批通过后的删除由框架落到 {@link ClueService#delete}, 池内的线索没有负责人,
     * 通知的收件人不能是 List.of(null)。
     */
    @Test
    void clueDeleteWithNoOwnerNotifiesNobodyInsteadOfThrowing() {
        ClueService clueService = new ClueService();
        ClueOwnerHistoryService ownerHistoryService = mock(ClueOwnerHistoryService.class);
        FollowUpRecordService followUpRecordService = mock(FollowUpRecordService.class);
        FollowUpPlanService followUpPlanService = mock(FollowUpPlanService.class);
        CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
        ReflectionTestUtils.setField(clueService, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(clueService, "statisticFieldService", statisticFieldService);
        ReflectionTestUtils.setField(clueService, "clueFieldService", clueFieldService);
        ReflectionTestUtils.setField(clueService, "clueOwnerHistoryService", ownerHistoryService);
        ReflectionTestUtils.setField(clueService, "followUpRecordService", followUpRecordService);
        ReflectionTestUtils.setField(clueService, "followUpPlanService", followUpPlanService);
        ReflectionTestUtils.setField(clueService, "commonNoticeSendService", noticeSendService);
        when(clueMapper.selectByPrimaryKey("clue-1")).thenReturn(clue("clue-1"));

        clueService.delete("clue-1", USER_ID, ORG_ID);

        verify(noticeSendService).sendNotice(
                eq(NotificationConstants.Module.CLUE),
                eq(NotificationConstants.Event.CLUE_DELETED),
                eq("clue-clue-1"),
                eq(USER_ID),
                eq(ORG_ID),
                eq(List.of()),
                eq(true));
    }

    private BatchAffectReasonResponse runEdit(ResourceBatchEditRequest request) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.batchUpdate(request, USER_ID, ORG_ID);
        }
    }

    private BatchAffectReasonResponse runDelete(List<String> ids) {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.batchDelete(ids, USER_ID, ORG_ID);
        }
    }

    private BaseField field(String businessKey, String name) {
        InputField field = new InputField();
        field.setId(FIELD_ID);
        field.setBusinessKey(businessKey);
        field.setName(name);
        return field;
    }

    private Clue clue(String id) {
        Clue clue = new Clue();
        clue.setId(id);
        clue.setName("clue-" + id);
        return clue;
    }
}
