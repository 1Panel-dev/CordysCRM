package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.constants.ApprovalFormTypeEnum;
import cn.cordys.crm.approval.constants.ApprovalStatus;
import cn.cordys.crm.approval.service.ApprovalFlowService;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.CluePool;
import cn.cordys.crm.clue.mapper.ExtClueMapper;
import cn.cordys.crm.system.dto.request.BatchPoolReasonRequest;
import cn.cordys.crm.system.dto.request.PoolReasonRequest;
import cn.cordys.crm.system.dto.response.BatchAffectResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ClueBatchToPoolApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String OWNER = "owner-1";

    private final ClueService service = new ClueService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Clue> clueMapper = mock(BaseMapper.class);
    private final ApprovalFlowService approvalFlowService = mock(ApprovalFlowService.class);
    private final CluePoolService cluePoolService = mock(CluePoolService.class);
    private final ClueOwnerHistoryService ownerHistoryService = mock(ClueOwnerHistoryService.class);
    private final ExtClueMapper extClueMapper = mock(ExtClueMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
    private final BatchPoolReasonRequest request = new BatchPoolReasonRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 回收日志与错误提示要取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, withSettings().mockMaker(MockMakers.INLINE)
                .defaultAnswer(invocation -> invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "clueMapper", clueMapper);
        ReflectionTestUtils.setField(service, "approvalFlowService", approvalFlowService);
        ReflectionTestUtils.setField(service, "cluePoolService", cluePoolService);
        ReflectionTestUtils.setField(service, "clueOwnerHistoryService", ownerHistoryService);
        ReflectionTestUtils.setField(service, "extClueMapper", extClueMapper);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSendService);
        // 状态权限默认放行全部, 让其余用例只关心回收本身; 收窄权限的用例自己再覆盖一次
        when(approvalFlowService.filterResourcesWithPermission(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> permittedIds((List<Clue>) invocation.getArgument(1)));
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of());
        // 未指定目标线索池时按负责人的默认线索池回收
        when(cluePoolService.getOwnersDefaultPoolMap(any(), any()))
                .thenReturn(Map.of(OWNER, pool("pool-1", "默认线索池")));

        request.setIds(List.of("clue-1", "clue-2"));
        request.setReasonId("reason-1");
    }

    private static List<String> permittedIds(List<Clue> clues) {
        return clues.stream().map(Clue::getId).toList();
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void statusPermissionIsCheckedBeforeTheRecycleWrites() {
        List<Clue> clues = List.of(clue("clue-1"), clue("clue-2"));
        when(clueMapper.selectListByLambda(any())).thenReturn(clues);

        service.batchToPool(request, USER_ID, ORG_ID);

        // 与其他批量操作一致: 先按审批状态过滤出有权回收的线索, 再去写回收 SQL
        InOrder inOrder = inOrder(approvalFlowService, extClueMapper);
        inOrder.verify(approvalFlowService).filterResourcesWithPermission(
                eq(ApprovalFormTypeEnum.CLUE.getValue()), same(clues),
                eq(PermissionConstants.CLUE_MANAGEMENT_RECYCLE), eq(ORG_ID), any(), any());
        inOrder.verify(extClueMapper, times(2)).moveToPool(any(Clue.class));
    }

    @Test
    void cluesTheOperatorHasNoPermissionForAreNotMoved() {
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of(clue("clue-1"), clue("clue-2")));
        // doReturn 而非 when: 默认 stub 已注册, when(...) 会先跑一遍它, 拿到的却是匹配器占位的 null
        doReturn(List.of("clue-1")).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        // 无权回收的线索不能跟着一起被移入线索池, 负责人也不该被清空
        verify(extClueMapper).moveToPool(argThat(clue -> "clue-1".equals(clue.getId())));
        verify(extClueMapper, never()).moveToPool(argThat(clue -> "clue-2".equals(clue.getId())));
        verify(noticeSendService, never()).sendNotice(any(), any(), eq("clue-clue-2"), anyString(), anyString(),
                anyList(), anyBoolean());
        // 没权限的那些既不算成功也不跳过, 与查不到的 id 一样落在 fail 里
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = { "PENDING", "APPROVING", "APPROVED", "UNAPPROVED", "REVOKED" })
    void recycleWithoutAnyPermittedClueIsRejectedBeforeWrites(ApprovalStatus status) {
        Clue clue = clue("clue-1");
        clue.setApprovalStatus(status.name());
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of(clue));
        doReturn(List.of()).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());

        GenericException exception = assertThrows(GenericException.class,
                () -> service.batchToPool(request, USER_ID, ORG_ID));

        assertEquals("clue.to.pool.approval.permission.denied", exception.getMessage());
        verifyNoInteractions(cluePoolService, extClueMapper, ownerHistoryService, logService, noticeSendService);
    }

    @Test
    void singleClueServiceUsesTheSameApprovalPermissionCheck() {
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of(clue("clue-1")));
        doReturn(List.of()).when(approvalFlowService)
                .filterResourcesWithPermission(any(), any(), any(), any(), any(), any());
        PoolReasonRequest singleRequest = new PoolReasonRequest();
        singleRequest.setId("clue-1");

        GenericException exception = assertThrows(GenericException.class,
                () -> service.toPool(singleRequest, USER_ID, ORG_ID));

        assertEquals("clue.to.pool.approval.permission.denied", exception.getMessage());
        verifyNoInteractions(cluePoolService, extClueMapper, ownerHistoryService, logService, noticeSendService);
    }

    @Test
    void missingCluesAreNotReportedAsApprovalPermissionDenied() {
        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        assertEquals(0, response.getSuccess());
        assertEquals(2, response.getFail());
        verifyNoInteractions(cluePoolService, extClueMapper, ownerHistoryService, logService, noticeSendService);
    }

    @Test
    void cluesAlreadyInThePoolAreSkippedAndCountedAsFail() {
        Clue inPool = clue("clue-1");
        inPool.setInSharedPool(true);
        when(clueMapper.selectListByLambda(any())).thenReturn(List.of(inPool, clue("clue-2")));

        BatchAffectResponse response = service.batchToPool(request, USER_ID, ORG_ID);

        verify(extClueMapper, never()).moveToPool(argThat(clue -> "clue-1".equals(clue.getId())));
        verify(extClueMapper).moveToPool(argThat(clue -> "clue-2".equals(clue.getId())));
        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getFail());
    }

    private Clue clue(String id) {
        Clue clue = new Clue();
        clue.setId(id);
        clue.setName("clue-" + id);
        clue.setOwner(OWNER);
        return clue;
    }

    private CluePool pool(String id, String name) {
        CluePool pool = new CluePool();
        pool.setId(id);
        pool.setName(name);
        pool.setOrganizationId(ORG_ID);
        return pool;
    }
}
