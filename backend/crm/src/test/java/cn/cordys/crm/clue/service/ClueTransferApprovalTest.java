package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalResourceUpdateType;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueApprovalSnapshotRequest;
import cn.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClueTransferApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String NEW_OWNER = "new-owner";

    private final ClueService service = new ClueService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    private final PoolClueService poolClueService = mock(PoolClueService.class);
    private final ClueOwnerHistoryService ownerHistoryService = mock(ClueOwnerHistoryService.class);
    private final ExtClueMapper extClueMapper = mock(ExtClueMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final ProductService productService = mock(ProductService.class);
    private final ClueFieldService clueFieldService = mock(ClueFieldService.class);
    private final StatisticFieldService statisticFieldService = mock(StatisticFieldService.class);
    private final BaseService baseService = mock(BaseService.class);
    private final ClueBatchTransferRequest request = new ClueBatchTransferRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 转移/批量编辑的返回值要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(service, "poolClueService", poolClueService);
        ReflectionTestUtils.setField(service, "clueOwnerHistoryService", ownerHistoryService);
        ReflectionTestUtils.setField(service, "extClueMapper", extClueMapper);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSendService);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "clueFieldService", clueFieldService);
        ReflectionTestUtils.setField(service, "statisticFieldService", statisticFieldService);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        // 状态权限默认放行全部, 让其余用例只关心转移本身; 收窄权限的用例自己再覆盖一次
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> permittedIds((List<Clue>) invocation.getArgument(1)));
        request.setIds(List.of("clue-1", "clue-2"));
        request.setOwner(NEW_OWNER);
    }

    private static List<String> permittedIds(List<Clue> clues) {
        return clues.stream().map(Clue::getId).toList();
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void onlyCluesWhoseOwnerChangesArePushedToApproval() {
        when(clueMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(clue("clue-1", "old-owner"), clue("clue-2", NEW_OWNER)));

        runTransfer();

        // 转移 SQL 带 owner != #{owner}, 这批线索不会被改写, 也就不该凭空多出一条审批
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("clue-1"), BusinessModuleField.CLUE_OWNER, FormKey.CLUE, ORG_ID, USER_ID, NEW_OWNER, null);
    }

    @Test
    void snapshotIsTakenBeforeTheTransferWrites() {
        when(clueMapper.selectByIds(request.getIds())).thenReturn(List.of(clue("clue-1", "old-owner")));

        runTransfer();

        // 快照必须早于转移 SQL, 否则驳回/撤回会把负责人回退成转移后的值
        InOrder inOrder = inOrder(approvalResourceService, extClueMapper);
        inOrder.verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("clue-1"), BusinessModuleField.CLUE_OWNER, FormKey.CLUE, ORG_ID, USER_ID, NEW_OWNER, null);
        inOrder.verify(extClueMapper).batchTransfer(request);
    }

    @Test
    void transferThatKeepsEveryOwnerStillGoesThroughTheEntryPointWithNoResource() {
        when(clueMapper.selectByIds(request.getIds())).thenReturn(List.of(clue("clue-1", NEW_OWNER)));

        runTransfer();

        // 入口必须被调用, 由它自己短路; 若改成调用方提前 return, 这里的空集合断言会失败
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of(), BusinessModuleField.CLUE_OWNER, FormKey.CLUE, ORG_ID, USER_ID, NEW_OWNER, null);
    }

    @Test
    void transferReportsChangedSkippedAndMissingCounts() {
        // 入参里带一个查不到的 id, 它既不算成功也不算跳过
        request.setIds(List.of("clue-1", "clue-2", "clue-3"));
        when(clueMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(clue("clue-1", "old-owner"), clue("clue-2", NEW_OWNER)));

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(1, response.getFail());
    }

    @Test
    void transferWithUnknownIdsOnlyReturnsNoResource() {
        when(clueMapper.selectByIds(request.getIds())).thenReturn(List.of());

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(0, response.getSuccess());
        verify(extClueMapper, never()).batchTransfer(any());
    }

    @Test
    void statusPermissionIsCheckedBeforeTheApprovalIsRaised() {
        List<Clue> clues = List.of(clue("clue-1", "old-owner"), clue("clue-2", "old-owner"));
        when(clueMapper.selectByIds(request.getIds())).thenReturn(clues);

        runTransfer();

        // 与其他批量操作一致: 先按审批状态过滤出有权转移的线索, 再拿这批去提审
        InOrder inOrder = inOrder(approvalFlowService, approvalResourceService);
        inOrder.verify(approvalFlowService).filterResourcesWithPermission(
                eq(ApprovalFormTypeEnum.CLUE.getValue()), same(clues),
                eq(PermissionConstants.CLUE_MANAGEMENT_TRANSFER), eq(ORG_ID), any(), any());
        inOrder.verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("clue-1", "clue-2"), BusinessModuleField.CLUE_OWNER, FormKey.CLUE, ORG_ID, USER_ID, NEW_OWNER,
                null);
    }

    @Test
    void transferLeavesOutTheCluesTheOperatorHasNoPermissionFor() {
        when(clueMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(clue("clue-1", "old-owner"), clue("clue-2", "old-owner")));
        // doReturn 而非 when: 默认 stub 已注册, when(...) 会先跑一遍它, 拿到的却是匹配器占位的 null
        doReturn(List.of("clue-1")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectReasonResponse response = runTransfer();

        // 转移 SQL 与负责人历史都按 request.ids 整批写, 入参必须同步收窄, 否则没权限的线索也跟着被改
        assertEquals(List.of("clue-1"), request.getIds());
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("clue-1"), BusinessModuleField.CLUE_OWNER, FormKey.CLUE, ORG_ID, USER_ID, NEW_OWNER, null);
        // 没权限的那些既不算成功也不算跳过, 与查不到的 id 一样落在 fail 里
        assertEquals(1, response.getSuccess());
        assertEquals(0, response.getSkip());
        assertEquals(1, response.getFail());
    }

    @Test
    void transferWithoutAnyPermittedClueTouchesNothing() {
        when(clueMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(clue("clue-1", "old-owner"), clue("clue-2", "old-owner")));
        doReturn(List.of()).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectReasonResponse response = runTransfer();

        verify(approvalResourceService, never()).batchTransferTriggerApproval(any(), any(), any(), any(), any(), any(),
                any());
        verify(extClueMapper, never()).batchTransfer(any());
        verify(poolClueService, never()).validateCapacity(anyInt(), anyString(), anyString());
        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
    }

    @Test
    void revertRestoresStageAndCollectionTimeFromTheSnapshot() {
        Clue current = clue("clue-1", NEW_OWNER);
        current.setStage("FOLLOWING");
        current.setCollectionTime(2_000L);
        when(clueMapper.selectByPrimaryKey("clue-1")).thenReturn(current);

        ClueApprovalSnapshotRequest snapshot = new ClueApprovalSnapshotRequest();
        snapshot.setId("clue-1");
        snapshot.setOwner("old-owner");
        snapshot.setStage("NEW");
        snapshot.setCollectionTime(1_000L);

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ClueService.class)).thenReturn(service);
            service.revertToSnapshot("clue-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }

        ArgumentCaptor<Clue> saved = ArgumentCaptor.forClass(Clue.class);
        verify(clueMapper).update(saved.capture());
        // 转移改写的阶段与领取时间随快照子类字段一并被编辑回退写回, 不需要单独的还原 SQL
        assertEquals("old-owner", saved.getValue().getOwner());
        assertEquals("NEW", saved.getValue().getStage());
        assertEquals(1_000L, saved.getValue().getCollectionTime());
    }

    @Test
    void revertCleansUpTheTransferHistoryAndDoesNotRecordANewOne() {
        Clue current = clue("clue-1", NEW_OWNER);
        current.setCollectionTime(2_000L);
        when(clueMapper.selectByPrimaryKey("clue-1")).thenReturn(current);

        ClueApprovalSnapshotRequest snapshot = new ClueApprovalSnapshotRequest();
        snapshot.setId("clue-1");
        snapshot.setOwner("old-owner");
        snapshot.setCollectionTime(1_000L);
        snapshot.setUpdateType(ApprovalResourceUpdateType.APPROVAL.getValue());

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ClueService.class)).thenReturn(service);
            service.revertToSnapshot("clue-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }

        // 转移时 batchAdd 写的那条记录要按快照里的(负责人, 领取时间)删掉
        verify(ownerHistoryService).deleteTransferHistory("clue-1", "old-owner", 1_000L);
        // 回退本身不是一次负责人变更, 不该再往历史里写一条
        verify(ownerHistoryService, never()).add(any(Clue.class), anyString(), anyBoolean());
        // 把线索还给原负责人也不该受原负责人的容量限制, 否则异常会被 revertToSnapshot 吞掉, 数据原地不动
        verify(poolClueService, never()).validateCapacity(anyInt(), anyString(), anyString());
    }

    private BatchAffectReasonResponse runTransfer() {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.batchTransfer(request, USER_ID, ORG_ID);
        }
    }

    private Clue clue(String id, String owner) {
        Clue clue = new Clue();
        clue.setId(id);
        clue.setName("clue-" + id);
        clue.setOwner(owner);
        return clue;
    }
}
