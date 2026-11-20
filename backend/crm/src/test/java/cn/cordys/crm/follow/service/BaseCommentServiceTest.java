package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.permission.RelatedResourcePermissionService;
import cn.cordys.common.permission.ResourcePermissionService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.domain.FollowUpRecordComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.mapper.ExtCommentMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BaseCommentServiceTest {

    private BaseMapper<FollowUpRecordComment> commentMapper;
    private CommonNoticeSendService noticeSender;
    private ExtCommentMapper extCommentMapper;
    private MockedStatic<Translator> translator;
    private MockedStatic<IDGenerator> ids;
    private CommentAddRequest request;
    private final List<NoticeSendContext> sent = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        commentMapper = mock(BaseMapper.class);
        noticeSender = mock(CommonNoticeSendService.class);
        extCommentMapper = mock(ExtCommentMapper.class);
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        ids = mockStatic(IDGenerator.class);
        AtomicInteger sequence = new AtomicInteger();
        ids.when(IDGenerator::nextStr).thenAnswer(invocation -> "generated-" + sequence.incrementAndGet());
        request = new CommentAddRequest();
        request.setResourceId("resource-a");
        request.setContent("评论内容");
        // 记录新版通知下发的上下文
        doAnswer(invocation -> {
            sent.add(invocation.getArgument(0));
            return null;
        }).when(noticeSender).sendNoticeByConfig(any(NoticeSendContext.class));
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void tearDown() {
        OperationLogContext.clear();
        translator.close();
        ids.close();
    }

    @Test
    void recordCommentWithoutReplyOrMentionNotifiesOwner() {
        recordService("owner").add(request, "operator", "org-a");

        assertEquals(1, sent.size());
        NoticeSendContext context = sent.get(0);
        assertEquals(NotificationConstants.Module.FOLLOW_UP, context.getTaskType());
        assertEquals(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT, context.getAction());
        assertEquals(Map.of("owner", "owner"), context.getNewValues());
        assertEquals("客户甲", context.getResourceName());
        assertEquals("resource-a", context.getResourceId());
        assertEquals("operator", context.getOperatorId());
        assertEquals("org-a", context.getOrganizationId());
        assertTrue(context.isExcludeSelf());
        assertTrue(context.isEventOnly());
    }

    @Test
    void recordCommentReplyNotifiesReplyUser() {
        request.setReplyToUserId("replied");

        recordService("owner").add(request, "operator", "org-a");

        assertEquals(2, sent.size());
        NoticeSendContext reply = find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY);
        assertEquals("replied", reply.getReplyToUserId());
        assertEquals(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT, sent.get(0).getAction());
    }

    @Test
    void recordCommentMentionNotifiesMentionedUsers() {
        request.setMentionedUserIds(List.of("m1", "m2"));

        recordService("owner").add(request, "operator", "org-a");

        assertEquals(2, sent.size());
        NoticeSendContext mentioned = find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED);
        assertEquals(List.of("m1", "m2"), mentioned.getMentionedUserIds());
    }

    @Test
    void recordCommentReplyAndMentionSendAllThreeNotices() {
        request.setReplyToUserId("replied");
        request.setMentionedUserIds(List.of("m1"));

        recordService("owner").add(request, "operator", "org-a");

        assertEquals(3, sent.size());
        find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT);
        find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY);
        find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED);
    }

    @Test
    void recordCommentWithoutOwnerSkipsCommentNotice() {
        recordService("").add(request, "operator", "org-a");

        assertEquals(0, sent.size());
    }

    @Test
    void planCommentNotifiesOwner() {
        planService().add(request, "operator", "org-a");

        assertEquals(1, sent.size());
        NoticeSendContext context = sent.get(0);
        assertEquals(NotificationConstants.Module.FOLLOW_UP, context.getTaskType());
        assertEquals(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT, context.getAction());
        assertEquals(Map.of("owner", "owner"), context.getNewValues());
        assertEquals("客户甲", context.getResourceName());
        assertEquals("resource-a", context.getResourceId());
        assertEquals("operator", context.getOperatorId());
        assertEquals("org-a", context.getOrganizationId());
        assertTrue(context.isExcludeSelf());
        assertTrue(context.isEventOnly());
    }

    @Test
    void planCommentReplyAndMentionSendAllThreeNotices() {
        request.setReplyToUserId("replied");
        request.setMentionedUserIds(List.of("m1"));

        planService().add(request, "operator", "org-a");

        assertEquals(3, sent.size());
        find(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT);
        assertEquals("replied", find(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_REPALY).getReplyToUserId());
        assertEquals(List.of("m1"), find(NotificationConstants.Event.FOLLOW_UP_PLAN_COMMENT_MENTIONED)
                .getMentionedUserIds());
    }

    @Test
    void editingRecordCommentSendsNewNotice() {
        FollowUpRecordCommentService service = recordService("owner");
        FollowUpRecordComment comment = new FollowUpRecordComment();
        comment.setId("comment-a");
        comment.setResourceId("resource-a");
        comment.setOrganizationId("org-a");
        comment.setCreateUser("operator");
        comment.setReplyToUserId("replied");
        comment.setContent("原评论");
        when(commentMapper.selectByPrimaryKey("comment-a")).thenReturn(comment);
        CommentUpdateRequest update = new CommentUpdateRequest();
        update.setId("comment-a");
        update.setContent("修改后评论");
        update.setMentionedUserIds(List.of("m1"));

        service.update(update, "operator", "org-a");

        assertEquals(3, sent.size());
        find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT);
        assertEquals("replied", find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_REPALY).getReplyToUserId());
        assertEquals(List.of("m1"), find(NotificationConstants.Event.FOLLOW_UP_RECORD_COMMENT_MENTIONED)
                .getMentionedUserIds());
    }

    private NoticeSendContext find(String action) {
        return sent.stream()
                .filter(context -> action.equals(context.getAction()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到事件为 " + action + " 的通知"));
    }

    private FollowUpRecordCommentService recordService(String owner) {
        FollowUpRecordCommentService service = new FollowUpRecordCommentService();
        BaseMapper<FollowUpRecord> resourceMapper = mock(BaseMapper.class);
        FollowUpRecord record = new FollowUpRecord();
        record.setOrganizationId("org-a");
        record.setType("CUSTOMER");
        record.setOwner(owner);
        record.setCustomerId("customer-a");
        when(resourceMapper.selectByPrimaryKey("resource-a")).thenReturn(record);
        ReflectionTestUtils.setField(service, "recordMapper", resourceMapper);
        ReflectionTestUtils.setField(service, "extRecordMapper", mock(ExtFollowUpRecordMapper.class));
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        wireCommon(service);
        return service;
    }

    @SuppressWarnings("unchecked")
    private FollowUpPlanCommentService planService() {
        FollowUpPlanCommentService service = new FollowUpPlanCommentService();
        BaseMapper<FollowUpPlan> resourceMapper = mock(BaseMapper.class);
        FollowUpPlan plan = new FollowUpPlan();
        plan.setOrganizationId("org-a");
        plan.setType("CUSTOMER");
        plan.setOwner("owner");
        plan.setCustomerId("customer-a");
        when(resourceMapper.selectByPrimaryKey("resource-a")).thenReturn(plan);
        ReflectionTestUtils.setField(service, "planMapper", resourceMapper);
        ReflectionTestUtils.setField(service, "extPlanMapper", mock(ExtFollowUpPlanMapper.class));
        ReflectionTestUtils.setField(service, "commentMapper", mock(BaseMapper.class));
        wireCommon(service);
        return service;
    }

    @SuppressWarnings("unchecked")
    private void wireCommon(BaseCommentService<?> service) {
        BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
        Customer customer = new Customer();
        customer.setName("客户甲");
        when(customerMapper.selectByPrimaryKey("customer-a")).thenReturn(customer);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "extCommentMapper", extCommentMapper);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSender);
        ReflectionTestUtils.setField(service, "relatedResourcePermissionService",
                mock(RelatedResourcePermissionService.class));
        ReflectionTestUtils.setField(service, "resourcePermissionService", mock(ResourcePermissionService.class));
    }
}
