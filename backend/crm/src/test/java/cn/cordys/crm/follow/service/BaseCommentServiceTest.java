package cn.cordys.crm.follow.service;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.constants.FollowUpCommentTargetType;
import cn.cordys.crm.follow.domain.Comment;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.crm.follow.domain.FollowUpRecordComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.mapper.ExtCommentMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.mapper.ExtFollowUpRecordMapper;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.mapper.ExtMessageTaskMapper;
import cn.cordys.crm.system.notice.CommonNoticeSendService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BaseCommentServiceTest {

    private BaseCommentService<? extends Comment> service;
    private BaseMapper<Comment> commentMapper;
    private ExtMessageTaskMapper messageTaskMapper;
    private CommonNoticeSendService noticeSender;
    private MockedStatic<Translator> translator;
    private CommentAddRequest request;
    private String addedEvent;
    private String mentionedEvent;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        commentMapper = mock(BaseMapper.class);
        messageTaskMapper = mock(ExtMessageTaskMapper.class);
        noticeSender = mock(CommonNoticeSendService.class);
        translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0));
        request = new CommentAddRequest();
        request.setResourceId("resource-a");
        request.setContent("评论内容");
        configureTarget(FollowUpCommentTargetType.RECORD);
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void tearDown() {
        OperationLogContext.clear();
        translator.close();
    }

    @ParameterizedTest
    @EnumSource(FollowUpCommentTargetType.class)
    void ownerAndMentionedUsersReceiveSeparateEvents(FollowUpCommentTargetType targetType) {
        configureTarget(targetType);
        request.setMentionedUserIds(List.of("mentioned", "mentioned"));

        service.add(request, "operator", "org-a");

        verifyNotice(addedEvent, targetType, List.of("owner"));
        verifyNotice(mentionedEvent, targetType, List.of("mentioned"));
        verifyNoMoreInteractions(noticeSender);
    }

    @Test
    void mentionedOwnerReceivesOnlyMentionEvent() {
        request.setMentionedUserIds(List.of("owner", "owner"));
        request.setReplyToUserId("owner");

        service.add(request, "operator", "org-a");

        verifyNotice(mentionedEvent, FollowUpCommentTargetType.RECORD, List.of("owner"));
        verifyNoMoreInteractions(noticeSender);
    }

    @Test
    void commentWithoutMentionsOnlyNotifiesOwner() {
        service.add(request, "operator", "org-a");

        verifyNotice(addedEvent, FollowUpCommentTargetType.RECORD, List.of("owner"));
        verifyNoMoreInteractions(noticeSender);
    }

    @Test
    void replyRecipientWithoutMentionReceivesOrdinaryNotice() {
        request.setReplyToUserId("replied-user");
        request.setMentionedUserIds(List.of("mentioned"));

        service.add(request, "operator", "org-a");

        verifyNotice(addedEvent, FollowUpCommentTargetType.RECORD, List.of("owner", "replied-user"));
        verifyNotice(mentionedEvent, FollowUpCommentTargetType.RECORD, List.of("mentioned"));
        verifyNoMoreInteractions(noticeSender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingConfigurationDoesNotSuppressOtherGroup(boolean missingAdded) {
        request.setMentionedUserIds(List.of("mentioned"));
        when(messageTaskMapper.getMessageByEvent(missingAdded ? addedEvent : mentionedEvent, "org-a"))
                .thenReturn(null);

        service.add(request, "operator", "org-a");

        verifyNotice(missingAdded ? mentionedEvent : addedEvent, FollowUpCommentTargetType.RECORD,
                List.of(missingAdded ? "mentioned" : "owner"));
        verifyNoMoreInteractions(noticeSender);
    }

    @Test
    void editingCommentKeepsOwnerAndMentionedUsersSeparate() {
        Comment comment = new FollowUpRecordComment();
        comment.setId("comment-a");
        comment.setResourceId("resource-a");
        comment.setOrganizationId("org-a");
        comment.setCreateUser("operator");
        comment.setContent("原评论");
        when(commentMapper.selectByPrimaryKey("comment-a")).thenReturn(comment);
        CommentUpdateRequest update = new CommentUpdateRequest();
        update.setId("comment-a");
        update.setContent("修改后评论");
        update.setMentionedUserIds(List.of("mentioned"));

        service.update(update, "operator", "org-a");

        verifyNotice(addedEvent, FollowUpCommentTargetType.RECORD, List.of("owner"));
        verifyNotice(mentionedEvent, FollowUpCommentTargetType.RECORD, List.of("mentioned"));
        verifyNoMoreInteractions(noticeSender);
    }

    @SuppressWarnings("unchecked")
    private void configureTarget(FollowUpCommentTargetType targetType) {
        if (targetType == FollowUpCommentTargetType.PLAN) {
            service = new FollowUpPlanCommentService();
            BaseMapper<FollowUpPlan> resourceMapper = mock(BaseMapper.class);
            FollowUpPlan plan = new FollowUpPlan();
            plan.setOrganizationId("org-a");
            plan.setType("CUSTOMER");
            plan.setOwner("owner");
            plan.setCustomerId("customer-a");
            when(resourceMapper.selectByPrimaryKey("resource-a")).thenReturn(plan);
            ReflectionTestUtils.setField(service, "planMapper", resourceMapper);
            ReflectionTestUtils.setField(service, "extPlanMapper", mock(ExtFollowUpPlanMapper.class));
            addedEvent = NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_ADDED;
            mentionedEvent = NotificationConstants.Event.CUSTOMER_FOLLOW_UP_PLAN_COMMENT_MENTIONED;
        } else {
            service = new FollowUpRecordCommentService();
            BaseMapper<FollowUpRecord> resourceMapper = mock(BaseMapper.class);
            FollowUpRecord record = new FollowUpRecord();
            record.setOrganizationId("org-a");
            record.setType("CUSTOMER");
            record.setOwner("owner");
            record.setCustomerId("customer-a");
            when(resourceMapper.selectByPrimaryKey("resource-a")).thenReturn(record);
            ReflectionTestUtils.setField(service, "recordMapper", resourceMapper);
            ReflectionTestUtils.setField(service, "extRecordMapper", mock(ExtFollowUpRecordMapper.class));
            addedEvent = NotificationConstants.Event.CUSTOMER_FOLLOW_UP_RECORD_COMMENT_ADDED;
            mentionedEvent = NotificationConstants.Event.CUSTOMER_FOLLOW_UP_RECORD_COMMENT_MENTIONED;
        }
        BaseMapper<Customer> customerMapper = mock(BaseMapper.class);
        Customer customer = new Customer();
        customer.setName("客户甲");
        when(customerMapper.selectByPrimaryKey("customer-a")).thenReturn(customer);
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(service, "extCommentMapper", mock(ExtCommentMapper.class));
        ReflectionTestUtils.setField(service, "extMessageTaskMapper", messageTaskMapper);
        ReflectionTestUtils.setField(service, "commonNoticeSendService", noticeSender);
        MessageTask messageTask = new MessageTask();
        messageTask.setTaskType("CUSTOMER");
        when(messageTaskMapper.getMessageByEvent(addedEvent, "org-a")).thenReturn(messageTask);
        when(messageTaskMapper.getMessageByEvent(mentionedEvent, "org-a")).thenReturn(messageTask);
    }

    private void verifyNotice(String event, FollowUpCommentTargetType targetType, List<String> users) {
        verify(noticeSender).sendNotice(eq("CUSTOMER"), eq(event),
                eq(Map.of("name", "客户甲", "resourceId", "resource-a", "targetType", targetType.name())),
                eq("operator"), eq("org-a"), eq(users), eq(true));
    }
}
