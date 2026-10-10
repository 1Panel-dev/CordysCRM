package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.service.ApprovalResourceService;
import cn.cordys.crm.customer.mapper.ExtCustomerContactMapper;
import cn.cordys.crm.opportunity.domain.Opportunity;
import cn.cordys.crm.opportunity.dto.request.OpportunityApprovalSnapshotRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityTransferRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.dto.response.BatchAffectReasonResponse;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.StatisticFieldService;
import cn.cordys.mybatis.BaseMapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OpportunityTransferApprovalTest {

    private static final String USER_ID = "operator";
    private static final String ORG_ID = "test-org";
    private static final String NEW_OWNER = "new-owner";

    private final OpportunityService service = new OpportunityService();
    @SuppressWarnings("unchecked")
    private final BaseMapper<Opportunity> opportunityMapper = mock(BaseMapper.class);
    private final ExtOpportunityMapper extOpportunityMapper = mock(ExtOpportunityMapper.class);
    private final SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
    private final SqlSession sqlSession = mock(SqlSession.class);
    private final ExtCustomerContactMapper extCustomerContactMapper = mock(ExtCustomerContactMapper.class);
    private final LogService logService = mock(LogService.class);
    private final CommonNoticeSendService noticeSendService = mock(CommonNoticeSendService.class);
    private final ApprovalResourceService approvalResourceService = mock(ApprovalResourceService.class);
    private final OpportunityFieldService opportunityFieldService = mock(OpportunityFieldService.class);
    private final StatisticFieldService statisticFieldService = mock(StatisticFieldService.class);
    private final ProductService productService = mock(ProductService.class);
    private final ModuleFormCacheService moduleFormCacheService = mock(ModuleFormCacheService.class);
    private final BaseService baseService = mock(BaseService.class);
    private final OpportunityTransferRequest request = new OpportunityTransferRequest();
    private MockedStatic<Translator> translator;

    @BeforeEach
    void setUp() {
        // 编辑回退内部会取翻译, 单测里没有 Spring 上下文, 直接回显 key
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(service, "opportunityMapper", opportunityMapper);
        ReflectionTestUtils.setField(service, "extOpportunityMapper", extOpportunityMapper);
        ReflectionTestUtils.setField(service, "sqlSessionFactory", sqlSessionFactory);
        ReflectionTestUtils.setField(service, "extCustomerContactMapper", extCustomerContactMapper);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSendService);
        ReflectionTestUtils.setField(service, "opportunityFieldService", opportunityFieldService);
        ReflectionTestUtils.setField(service, "statisticFieldService", statisticFieldService);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", moduleFormCacheService);
        ReflectionTestUtils.setField(service, "baseService", baseService);
        when(sqlSessionFactory.openSession(ExecutorType.BATCH)).thenReturn(sqlSession);
        when(sqlSession.getMapper(ExtOpportunityMapper.class)).thenReturn(extOpportunityMapper);
        request.setIds(List.of("opportunity-1", "opportunity-2"));
        request.setOwner(NEW_OWNER);
    }

    @AfterEach
    void tearDown() {
        translator.close();
    }

    @Test
    void onlyOpportunitiesWhoseOwnerChangesArePushedToApproval() {
        when(opportunityMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(opportunity("opportunity-1", "old-owner"), opportunity("opportunity-2", NEW_OWNER)));

        runTransfer();

        // 转移 SQL 逐条改写商机, 但负责人本就相同的商机不需要审批介入
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("opportunity-1"), BusinessModuleField.OPPORTUNITY_OWNER, FormKey.OPPORTUNITY, ORG_ID, USER_ID, NEW_OWNER);
    }

    @Test
    void snapshotIsTakenBeforeTheTransferWrites() {
        when(opportunityMapper.selectByIds(request.getIds())).thenReturn(List.of(opportunity("opportunity-1", "old-owner")));

        runTransfer();

        // 快照必须早于转移 SQL, 否则驳回/撤回会把负责人回退成转移后的值
        InOrder inOrder = inOrder(approvalResourceService, extOpportunityMapper);
        inOrder.verify(approvalResourceService).batchTransferTriggerApproval(
                List.of("opportunity-1"), BusinessModuleField.OPPORTUNITY_OWNER, FormKey.OPPORTUNITY, ORG_ID, USER_ID, NEW_OWNER);
        inOrder.verify(extOpportunityMapper).transfer(eq(NEW_OWNER), eq(USER_ID), eq("opportunity-1"), anyLong());
    }

    @Test
    void transferThatKeepsEveryOwnerStillGoesThroughTheEntryPointWithNoResource() {
        when(opportunityMapper.selectByIds(request.getIds())).thenReturn(List.of(opportunity("opportunity-1", NEW_OWNER)));

        runTransfer();

        // 入口必须被调用, 由它自己短路; 若改成调用方提前 return, 这里的空集合断言会失败
        verify(approvalResourceService).batchTransferTriggerApproval(
                List.of(), BusinessModuleField.OPPORTUNITY_OWNER, FormKey.OPPORTUNITY, ORG_ID, USER_ID, NEW_OWNER);
    }

    @Test
    void onlyOpportunitiesWhoseOwnerChangesAreWrittenAndSynced() {
        when(opportunityMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(opportunity("opportunity-1", "old-owner"), opportunity("opportunity-2", NEW_OWNER)));

        runTransfer();

        // 转移 SQL 没有 owner != 的条件, 负责人本就相同的商机不该被无谓地改写 update_user/update_time
        verify(extOpportunityMapper).transfer(eq(NEW_OWNER), eq(USER_ID), eq("opportunity-1"), anyLong());
        verify(extOpportunityMapper, never()).transfer(anyString(), anyString(), eq("opportunity-2"), anyLong());
        // 联系人负责人同理: 只跟着真正换人的商机走
        verify(extCustomerContactMapper).updateContactById("contact-opportunity-1", NEW_OWNER);
        verify(extCustomerContactMapper, never()).updateContactById(eq("contact-opportunity-2"), anyString());
    }

    @Test
    void transferReportsChangedSkippedAndMissingCounts() {
        // 入参里带一个查不到的 id, 它既不算成功也不算跳过
        request.setIds(List.of("opportunity-1", "opportunity-2", "opportunity-3"));
        when(opportunityMapper.selectByIds(request.getIds()))
                .thenReturn(List.of(opportunity("opportunity-1", "old-owner"), opportunity("opportunity-2", NEW_OWNER)));

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(1, response.getSuccess());
        assertEquals(1, response.getSkip());
        assertEquals(1, response.getFail());
    }

    @Test
    void transferWithUnknownIdsOnlyReturnsNoResource() {
        when(opportunityMapper.selectByIds(request.getIds())).thenReturn(List.of());

        BatchAffectReasonResponse response = runTransfer();

        assertEquals(0, response.getSuccess());
        verify(extOpportunityMapper, never()).transfer(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void revertPutsTheContactBackUnderTheRestoredOwner() {
        Opportunity row = opportunity("opportunity-1", NEW_OWNER);
        when(opportunityMapper.selectByPrimaryKey("opportunity-1")).thenReturn(row);
        // 让编辑回退的写回落回同一行上, 后续同步联系人时才能读到还原后的负责人
        doAnswer(invocation -> {
            Opportunity written = invocation.getArgument(0);
            row.setOwner(written.getOwner());
            row.setContactId(written.getContactId());
            return null;
        }).when(extOpportunityMapper).updateIncludeNullById(any(Opportunity.class));

        OpportunityApprovalSnapshotRequest snapshot = new OpportunityApprovalSnapshotRequest();
        snapshot.setId("opportunity-1");
        snapshot.setOwner("old-owner");
        snapshot.setContactId("contact-1");

        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(OpportunityService.class)).thenReturn(service);
            service.revertToSnapshot("opportunity-1", USER_ID, ORG_ID, JSON.toJSONString(snapshot));
        }

        // 转移会把联系人负责人一起改掉, 回退时必须跟着商机的负责人一起还原
        assertEquals("old-owner", row.getOwner());
        verify(extCustomerContactMapper).updateContactById("contact-1", "old-owner");
    }

    @Test
    void editWithOwnerChangeMovesTheContactAndNotifiesLikeATransfer() {
        Opportunity row = opportunity("opportunity-1", "old-owner");
        when(opportunityMapper.selectByPrimaryKey("opportunity-1")).thenReturn(row);

        service.update(editRequest(NEW_OWNER), USER_ID, ORG_ID);

        // 编辑里改负责人与转移同义: 关联联系人跟着走, 新负责人也要收到通知
        verify(extCustomerContactMapper).updateContactById("contact-1", NEW_OWNER);
        verify(noticeSendService).sendNotice(any(), eq(NotificationConstants.Event.BUSINESS_TRANSFER),
                anyString(), eq(USER_ID), eq(ORG_ID), eq(List.of(NEW_OWNER)), eq(true));
    }

    @Test
    void editThatKeepsTheOwnerLeavesTheContactAndTheNoticeAlone() {
        Opportunity row = opportunity("opportunity-1", NEW_OWNER);
        when(opportunityMapper.selectByPrimaryKey("opportunity-1")).thenReturn(row);

        service.update(editRequest(NEW_OWNER), USER_ID, ORG_ID);

        // 只是改改金额之类的, 不该当成一次转移
        verify(extCustomerContactMapper, never()).updateContactById(anyString(), anyString());
        verify(noticeSendService, never()).sendNotice(any(), any(), anyString(), anyString(), anyString(), any(), anyBoolean());
    }

    private BatchAffectReasonResponse runTransfer() {
        try (MockedStatic<CommonBeanFactory> beanFactory = mockStatic(CommonBeanFactory.class)) {
            beanFactory.when(() -> CommonBeanFactory.getBean(ApprovalResourceService.class))
                    .thenReturn(approvalResourceService);
            return service.transfer(request, USER_ID, ORG_ID);
        }
    }

    private OpportunityUpdateRequest editRequest(String owner) {
        OpportunityUpdateRequest update = new OpportunityUpdateRequest();
        update.setId("opportunity-1");
        update.setOwner(owner);
        update.setContactId("contact-1");
        return update;
    }

    private Opportunity opportunity(String id, String owner) {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(id);
        opportunity.setName("opportunity-" + id);
        opportunity.setOwner(owner);
        opportunity.setContactId("contact-" + id);
        return opportunity;
    }
}
