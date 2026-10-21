package cn.cordys.crm.approval.service;

import cn.cordys.common.dto.SortRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.impl.DefaultUidGenerator;
import cn.cordys.common.uid.worker.WorkerIdAssigner;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalCommentMention;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.request.ApprovalCommentAddRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentPageRequest;
import cn.cordys.crm.approval.dto.request.ApprovalCommentUpdateRequest;
import cn.cordys.crm.approval.dto.response.ApprovalCommentMentionUserResponse;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.mapper.ExtApprovalCommentMapper;
import cn.cordys.crm.follow.mapper.ExtCommentMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ApprovalCommentServiceTest {

    private static final String ORG = "org-a";
    private static final String USER = "user-a";
    private static final String INSTANCE = "instance-a";
    private static ApplicationContext previousContext;
    private static MessageSource previousMessageSource;
    private static DefaultUidGenerator uidGenerator;

    private ApprovalCommentService service;
    private BaseMapper<ApprovalComment> commentMapper;
    private BaseMapper<ApprovalCommentMention> mentionMapper;
    private ExtApprovalCommentMapper extCommentMapper;
    private ApprovalResourceService approvalResourceService;
    private ApprovalCommentNoticeService approvalCommentNoticeService;
    private ApprovalInstance instance;

    @BeforeAll
    static void initCommonUtilities() {
        previousContext = (ApplicationContext) ReflectionTestUtils.getField(CommonBeanFactory.class, "context");
        previousMessageSource = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        ReflectionTestUtils.setField(Translator.class, "messageSource", new StaticMessageSource());
        uidGenerator = new DefaultUidGenerator();
        ReflectionTestUtils.setField(uidGenerator, "workerIdAssigner", (WorkerIdAssigner) () -> 1L);
        uidGenerator.init();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(DefaultUidGenerator.class)).thenReturn(uidGenerator);
        new CommonBeanFactory().setApplicationContext(context);
    }

    @AfterAll
    static void restoreCommonUtilities() {
        new CommonBeanFactory().setApplicationContext(previousContext);
        ReflectionTestUtils.setField(Translator.class, "messageSource", previousMessageSource);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ApprovalCommentService();
        commentMapper = mock(BaseMapper.class);
        mentionMapper = mock(BaseMapper.class);
        extCommentMapper = mock(ExtApprovalCommentMapper.class);
        approvalResourceService = mock(ApprovalResourceService.class);
        approvalCommentNoticeService = mock(ApprovalCommentNoticeService.class);
        ReflectionTestUtils.setField(service, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(service, "mentionMapper", mentionMapper);
        ReflectionTestUtils.setField(service, "extCommentMapper", extCommentMapper);
        ReflectionTestUtils.setField(service, "approvalResourceService", approvalResourceService);
        ReflectionTestUtils.setField(service, "approvalCommentNoticeService", approvalCommentNoticeService);
        instance = new ApprovalInstance();
        instance.setId(INSTANCE);
        when(extCommentMapper.selectInstance(INSTANCE, ORG)).thenReturn(instance);
    }

    @AfterEach
    void clearPage() {
        PageHelper.clearPage();
    }

    @Test
    void injectsApprovalMapperWhenFollowCommentMapperIsAlsoRegistered() {
        ExtCommentMapper followCommentMapper = mock(ExtCommentMapper.class);
        when(approvalResourceService.hasInstanceViewPermission(instance, USER, ORG)).thenReturn(true);
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("extCommentMapper", followCommentMapper);
            context.getBeanFactory().registerSingleton("extApprovalCommentMapper", extCommentMapper);
            context.getBeanFactory().registerSingleton("commentMapper", commentMapper);
            context.getBeanFactory().registerSingleton("mentionMapper", mentionMapper);
            context.getBeanFactory().registerSingleton("approvalResourceService", approvalResourceService);
            context.getBeanFactory().registerSingleton("approvalCommentNoticeService", approvalCommentNoticeService);
            context.registerBean(ApprovalCommentService.class);
            context.refresh();

            assertTrue(context.getBean(ApprovalCommentService.class).hasCommentPermission(INSTANCE, USER, ORG));

            verify(extCommentMapper).selectInstance(INSTANCE, ORG);
            verifyNoInteractions(followCommentMapper);
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, false, false", "true, false, true", "false, true, true" })
    void returnsCommentPermissionFromBusinessOrEffectiveMentions(boolean businessOrTaskPermission,
            boolean mentioned, boolean expected) {
        when(approvalResourceService.hasInstanceViewPermission(instance, USER, ORG))
                .thenReturn(businessOrTaskPermission);
        when(extCommentMapper.existsMention(INSTANCE, USER, ORG)).thenReturn(mentioned);

        assertEquals(expected, service.hasCommentPermission(INSTANCE, USER, ORG));

        verify(approvalResourceService).hasInstanceViewPermission(instance, USER, ORG);
        if (!businessOrTaskPermission) {
            verify(extCommentMapper).existsMention(INSTANCE, USER, ORG);
        }
    }

    @ParameterizedTest
    @CsvSource({ "missing-instance, org-a", "instance-a, other-org" })
    void returnsNoCommentPermissionForUnknownInstanceOrAnotherOrganization(String instanceId, String orgId) {
        assertFalse(service.hasCommentPermission(instanceId, USER, orgId));

        verify(extCommentMapper).selectInstance(instanceId, orgId);
        verifyNoInteractions(approvalResourceService, commentMapper, mentionMapper);
        verify(extCommentMapper, never()).existsMention(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectsUnknownInstanceOrMissingAllPermissions(boolean knownInstance) {
        if (!knownInstance) {
            when(extCommentMapper.selectInstance(INSTANCE, ORG)).thenReturn(null);
        }

        assertThrows(GenericException.class, () -> service.add(addRequest(), USER, ORG));

        verify(extCommentMapper).selectInstance(INSTANCE, ORG);
        verifyNoInteractions(commentMapper, mentionMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void allowsBusinessOrTaskPermissionAndEffectiveMention(boolean businessOrTaskPermission) {
        ApprovalCommentResponse detail = detail("comment-a");
        when(extCommentMapper.selectPage(INSTANCE, ORG, false)).thenReturn(List.of(detail));
        when(approvalResourceService.hasInstanceViewPermission(instance, USER, ORG))
                .thenReturn(businessOrTaskPermission);
        when(extCommentMapper.existsMention(INSTANCE, USER, ORG)).thenReturn(!businessOrTaskPermission);

        assertEquals(List.of(detail), service.page(pageRequest(), USER, ORG).getList());

        assertTrue(detail.isEditable());
        verify(approvalResourceService).hasInstanceViewPermission(instance, USER, ORG);
        if (!businessOrTaskPermission) {
            verify(extCommentMapper).existsMention(INSTANCE, USER, ORG);
        }
    }

    @Test
    void pagesRootCommentsWithCompleteRepliesAndDisplayInformation() {
        allowExistingPermission();
        ApprovalCommentResponse root = detail("comment-a");
        root.setContent("一级评论");
        root.setCreateUserName("评论甲");
        root.setCreateUserAvatar("avatar-a");
        root.setCreateTime(100L);
        root.setUpdateTime(110L);
        root.setReplyCount(99L);
        ApprovalCommentResponse reply = detail("reply-a");
        reply.setParentId(root.getId());
        reply.setCreateUser("other-user");
        reply.setCreateUserName("评论乙");
        reply.setCreateUserAvatar("avatar-b");
        reply.setContent("二级回复");
        reply.setCreateTime(200L);
        reply.setUpdateTime(210L);
        reply.setReplyToUserId(USER);
        reply.setReplyToUserName(root.getCreateUserName());
        ApprovalCommentResponse anotherReply = detail("reply-b");
        anotherReply.setParentId(root.getId());
        ApprovalCommentMentionUserResponse rootMention = new ApprovalCommentMentionUserResponse();
        rootMention.setCommentId(root.getId());
        rootMention.setId("member-a");
        ApprovalCommentMentionUserResponse replyMention = new ApprovalCommentMentionUserResponse();
        replyMention.setCommentId(reply.getId());
        replyMention.setId("member-b");
        ApprovalCommentPageRequest request = pageRequest();
        request.setCurrent(2);
        request.setPageSize(1);
        when(extCommentMapper.selectPage(INSTANCE, ORG, false)).thenAnswer(invocation -> {
            Page<?> page = PageHelper.getLocalPage();
            assertEquals(2, page.getPageNum());
            assertEquals(1, page.getPageSize());
            page.setTotal(9);
            // 模拟真实分页拦截器在一级查询结束后清理上下文。
            PageHelper.clearPage();
            return List.of(root);
        });
        when(extCommentMapper.selectChildren(INSTANCE, List.of(root.getId()), ORG, false))
                .thenAnswer(invocation -> {
                    assertNull(PageHelper.getLocalPage());
                    return List.of(reply, anotherReply);
                });
        when(extCommentMapper.selectMentionUsers(List.of(root.getId(), reply.getId(), anotherReply.getId()), ORG))
                .thenReturn(List.of(replyMention, rootMention));

        Pager<List<ApprovalCommentResponse>> result = service.page(request, USER, ORG);
        List<ApprovalCommentResponse> roots = result.getList();

        assertEquals(9L, result.getTotal());
        assertEquals(2L, result.getCurrent());
        assertEquals(1L, result.getPageSize());
        assertNull(PageHelper.getLocalPage());
        assertEquals(List.of(root), roots);
        assertEquals("一级评论", roots.getFirst().getContent());
        assertEquals("评论甲", roots.getFirst().getCreateUserName());
        assertEquals("avatar-a", roots.getFirst().getCreateUserAvatar());
        assertEquals(100L, roots.getFirst().getCreateTime());
        assertEquals(110L, roots.getFirst().getUpdateTime());
        assertEquals(2L, roots.getFirst().getReplyCount());
        assertEquals(List.of(rootMention), roots.getFirst().getMentionUsers());
        assertTrue(roots.getFirst().isEditable());
        List<ApprovalCommentResponse> replies = roots.getFirst().getReplies();

        assertEquals(List.of(reply, anotherReply), replies);
        assertEquals("二级回复", replies.getFirst().getContent());
        assertEquals("评论乙", replies.getFirst().getCreateUserName());
        assertEquals("avatar-b", replies.getFirst().getCreateUserAvatar());
        assertEquals(200L, replies.getFirst().getCreateTime());
        assertEquals(210L, replies.getFirst().getUpdateTime());
        assertEquals(USER, replies.getFirst().getReplyToUserId());
        assertEquals("评论甲", replies.getFirst().getReplyToUserName());
        assertEquals(List.of(replyMention), replies.getFirst().getMentionUsers());
        assertFalse(replies.getFirst().isEditable());
        assertTrue(replies.getFirst().getReplies().isEmpty());
        assertEquals(0L, replies.getFirst().getReplyCount());
        verify(extCommentMapper).selectChildren(INSTANCE, List.of(root.getId()), ORG, false);
        verify(extCommentMapper).selectMentionUsers(List.of(root.getId(), reply.getId(), anotherReply.getId()), ORG);
    }

    @Test
    void groupsRepliesByParentAndKeepsRootsWithoutReplies() {
        allowExistingPermission();
        ApprovalCommentResponse first = detail("comment-a");
        ApprovalCommentResponse second = detail("comment-b");
        ApprovalCommentResponse noReply = detail("comment-c");
        ApprovalCommentResponse firstReply = detail("reply-a1");
        firstReply.setParentId(first.getId());
        ApprovalCommentResponse nextReply = detail("reply-a2");
        nextReply.setParentId(first.getId());
        ApprovalCommentResponse secondReply = detail("reply-b");
        secondReply.setParentId(second.getId());
        List<String> parentIds = List.of(first.getId(), second.getId(), noReply.getId());
        when(extCommentMapper.selectPage(INSTANCE, ORG, false)).thenReturn(List.of(first, second, noReply));
        when(extCommentMapper.selectChildren(INSTANCE, parentIds, ORG, false))
                .thenReturn(List.of(secondReply, firstReply, nextReply));

        List<ApprovalCommentResponse> comments = service.page(pageRequest(), USER, ORG).getList();

        assertEquals(List.of(first, second, noReply), comments);
        assertEquals(List.of(firstReply, nextReply), first.getReplies());
        assertEquals(2L, first.getReplyCount());
        assertEquals(List.of(secondReply), second.getReplies());
        assertEquals(1L, second.getReplyCount());
        assertTrue(noReply.getReplies().isEmpty());
        assertEquals(0L, noReply.getReplyCount());
        verify(extCommentMapper).selectChildren(INSTANCE, parentIds, ORG, false);
    }

    @Test
    void emptyPageKeepsRootTotalAndSkipsChildrenAndMentionLookup() {
        allowExistingPermission();
        ApprovalCommentPageRequest request = pageRequest();
        request.setCurrent(4);
        request.setPageSize(5);
        when(extCommentMapper.selectPage(INSTANCE, ORG, false)).thenAnswer(invocation -> {
            Page<?> page = PageHelper.getLocalPage();
            page.setTotal(12);
            return List.of();
        });

        Pager<List<ApprovalCommentResponse>> result = service.page(request, USER, ORG);

        assertTrue(result.getList().isEmpty());
        assertEquals(12L, result.getTotal());
        assertEquals(4L, result.getCurrent());
        assertEquals(5L, result.getPageSize());
        assertNull(PageHelper.getLocalPage());
        verify(extCommentMapper, never()).selectChildren(anyString(), any(), anyString(), anyBoolean());
        verify(extCommentMapper, never()).selectMentionUsers(any(), anyString());
    }

    @ParameterizedTest
    @CsvSource(value = { "asc,true", "ASC,true", "desc,false", "invalid,false", "NULL,false" }, nullValues = "NULL")
    void passesCreationTimeDirectionToRootAndReplyQueries(String direction, boolean asc) {
        allowExistingPermission();
        ApprovalCommentPageRequest request = pageRequest();
        request.setSort(new SortRequest("createTime", direction));
        ApprovalCommentResponse root = detail("comment-a");
        when(extCommentMapper.selectPage(INSTANCE, ORG, asc)).thenReturn(List.of(root));

        service.page(request, USER, ORG);

        verify(extCommentMapper).selectPage(INSTANCE, ORG, asc);
        verify(extCommentMapper).selectChildren(INSTANCE, List.of(root.getId()), ORG, asc);
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void deniedPageDoesNotStartPaginationOrReadComments() {
        when(approvalResourceService.hasInstanceViewPermission(instance, USER, ORG)).thenAnswer(invocation -> {
            assertNull(PageHelper.getLocalPage());
            return false;
        });

        assertThrows(GenericException.class, () -> service.page(pageRequest(), USER, ORG));

        assertNull(PageHelper.getLocalPage());
        verify(extCommentMapper, never()).selectPage(anyString(), anyString(), anyBoolean());
        verify(extCommentMapper, never()).selectChildren(anyString(), any(), anyString(), anyBoolean());
    }

    @Test
    void rootQueryFailureClearsPaginationContext() {
        allowExistingPermission();
        when(extCommentMapper.selectPage(INSTANCE, ORG, false)).thenAnswer(invocation -> {
            assertNotNull(PageHelper.getLocalPage());
            throw new IllegalStateException("查询失败");
        });

        assertThrows(IllegalStateException.class, () -> service.page(pageRequest(), USER, ORG));

        assertNull(PageHelper.getLocalPage());
        verify(extCommentMapper, never()).selectChildren(anyString(), any(), anyString(), anyBoolean());
    }

    @Test
    void rejectsReplyToAnotherApprovalInstance() {
        allowExistingPermission();
        ApprovalComment parent = comment("parent-a", "other-user");
        parent.setInstanceId("instance-b");
        when(extCommentMapper.selectCommentForUpdate("parent-a", ORG)).thenReturn(parent);
        ApprovalCommentAddRequest request = addRequest();
        request.setParentId("parent-a");

        assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        verifyNoInteractions(commentMapper, mentionMapper);
    }

    @Test
    void rejectsThirdCommentLevel() {
        allowExistingPermission();
        ApprovalComment parent = comment("reply-a", "other-user");
        parent.setParentId("parent-a");
        when(extCommentMapper.selectCommentForUpdate("reply-a", ORG)).thenReturn(parent);
        ApprovalCommentAddRequest request = addRequest();
        request.setParentId("reply-a");

        assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        verifyNoInteractions(commentMapper, mentionMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectsEditingOrDeletingAnotherUsersComment(boolean update) {
        allowExistingPermission();
        when(extCommentMapper.selectCommentForUpdate("comment-a", ORG)).thenReturn(comment("comment-a", "other-user"));

        assertThrows(GenericException.class, () -> {
            if (update) {
                service.update(updateRequest(), USER, ORG);
            } else {
                service.delete("comment-a", USER, ORG);
            }
        });

        verifyNoInteractions(commentMapper, mentionMapper);
        verify(extCommentMapper, never()).deleteMentions(any(), anyString());
        verify(extCommentMapper, never()).deleteComments(any(), anyString(), anyString());
    }

    @Test
    void rejectsMentioningUserOutsideEnabledOrganizationMembers() {
        allowExistingPermission();
        ApprovalCommentAddRequest request = addRequest();
        request.setMentionedUserIds(List.of("member-a", "foreign-user"));
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a", "foreign-user"), ORG))
                .thenReturn(List.of("member-a"));

        assertThrows(GenericException.class, () -> service.add(request, USER, ORG));

        verifyNoInteractions(commentMapper, mentionMapper);
    }

    @Test
    void allowsFinishingOwnEditWhenItRemovesTheLastMentionGrant() {
        when(extCommentMapper.selectCommentForUpdate("comment-a", ORG)).thenReturn(comment("comment-a", USER));
        when(extCommentMapper.existsMention(INSTANCE, USER, ORG)).thenReturn(true, false);
        when(mentionMapper.select(any())).thenReturn(List.of(mention("mention-a", "comment-a", USER)));
        ApprovalCommentResponse detail = detail("comment-a");
        when(extCommentMapper.selectDetail("comment-a", ORG)).thenReturn(detail);

        assertSame(detail, service.update(updateRequest(), USER, ORG));

        ArgumentCaptor<ApprovalComment> updated = ArgumentCaptor.forClass(ApprovalComment.class);
        verify(commentMapper).update(updated.capture());
        assertEquals("修改后的评论", updated.getValue().getContent());
        assertEquals(USER, updated.getValue().getUpdateUser());
        assertFalse(service.hasCommentPermission(INSTANCE, USER, ORG));
        verify(extCommentMapper, times(2)).existsMention(INSTANCE, USER, ORG);
        verify(mentionMapper).deleteByIds(List.of("mention-a"));
        verifyNoInteractions(approvalCommentNoticeService);
    }

    @Test
    void deletesRootAndRepliesTogetherWithTheirMentionGrants() {
        allowExistingPermission();
        when(extCommentMapper.selectCommentForUpdate("comment-a", ORG)).thenReturn(comment("comment-a", USER));
        ApprovalComment reply = comment("reply-a", "other-user");
        reply.setParentId("comment-a");
        when(commentMapper.selectListByLambda(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<ApprovalComment> query = invocation.getArgument(0);
            assertTrue(query.getParams().containsValue(INSTANCE));
            assertTrue(query.getParams().containsValue(ORG));
            assertTrue(query.getParams().containsValue("comment-a"));
            return List.of(reply);
        });

        service.delete("comment-a", USER, ORG);

        verify(extCommentMapper).deleteMentions(List.of("comment-a", "reply-a"), ORG);
        verify(extCommentMapper).deleteComments(List.of("comment-a", "reply-a"), INSTANCE, ORG);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void deduplicatesMentionUsersAndDefaultsReplyRecipientToRootAuthor() {
        allowExistingPermission();
        when(extCommentMapper.selectCommentForUpdate("parent-a", ORG)).thenReturn(comment("parent-a", "root-author"));
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a"), ORG)).thenReturn(List.of("member-a"));
        when(extCommentMapper.selectDetail(anyString(), eq(ORG)))
                .thenAnswer(invocation -> detail(invocation.getArgument(0)));
        ApprovalCommentAddRequest request = addRequest();
        request.setParentId("parent-a");
        request.setMentionedUserIds(List.of("member-a", "member-a"));

        service.add(request, USER, ORG);

        ArgumentCaptor<ApprovalComment> commentCaptor = ArgumentCaptor.forClass(ApprovalComment.class);
        verify(commentMapper).insert(commentCaptor.capture());
        assertEquals("parent-a", commentCaptor.getValue().getParentId());
        assertEquals("root-author", commentCaptor.getValue().getReplyToUserId());
        ArgumentCaptor<List<ApprovalCommentMention>> mentionCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(mentionMapper).batchInsert(mentionCaptor.capture());
        assertEquals(1, mentionCaptor.getValue().size());
        assertEquals("member-a", mentionCaptor.getValue().getFirst().getUserId());
        assertEquals(commentCaptor.getValue().getId(), mentionCaptor.getValue().getFirst().getCommentId());
        assertEquals(commentCaptor.getValue().getCreateTime(), mentionCaptor.getValue().getFirst().getCreateTime());
        verify(approvalCommentNoticeService).sendMentionNotice(commentCaptor.getValue(), List.of("member-a"));
    }

    @Test
    void unchangedMentionKeepsTodoIdentityAndTimeWithoutAnotherNotice() {
        prepareOwnEdit();
        ApprovalCommentMention existing = mention("mention-kept", "comment-a", "member-a");
        when(mentionMapper.select(any())).thenReturn(List.of(existing));
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a"), ORG)).thenReturn(List.of("member-a"));
        ApprovalCommentUpdateRequest request = updateRequest();
        request.setMentionedUserIds(List.of("member-a"));

        service.update(request, USER, ORG);

        assertEquals("mention-kept", existing.getId());
        assertEquals(100L, existing.getCreateTime());
        verify(mentionMapper, never()).deleteByIds(any());
        verify(mentionMapper, never()).batchInsert(any());
        verifyNoInteractions(approvalCommentNoticeService);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void editingMentionsOnlyCreatesAndNotifiesNewRecipients() {
        prepareOwnEdit();
        ApprovalCommentMention kept = mention("mention-kept", "comment-a", "member-a");
        ApprovalCommentMention removed = mention("mention-removed", "comment-a", "member-b");
        when(mentionMapper.select(any())).thenReturn(List.of(kept, removed));
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a", "member-c"), ORG)).thenReturn(List.of("member-a", "member-c"));
        ApprovalCommentUpdateRequest request = updateRequest();
        request.setMentionedUserIds(List.of("member-a", "member-c"));

        service.update(request, USER, ORG);

        verify(mentionMapper).deleteByIds(List.of(removed.getId()));
        ArgumentCaptor<List<ApprovalCommentMention>> created = ArgumentCaptor.forClass((Class) List.class);
        verify(mentionMapper).batchInsert(created.capture());
        assertEquals(List.of("member-c"), created.getValue().stream().map(ApprovalCommentMention::getUserId).toList());
        assertEquals("comment-a", created.getValue().getFirst().getCommentId());
        assertEquals("mention-kept", kept.getId());
        assertEquals(100L, kept.getCreateTime());
        ArgumentCaptor<ApprovalComment> notifiedComment = ArgumentCaptor.forClass(ApprovalComment.class);
        verify(approvalCommentNoticeService).sendMentionNotice(notifiedComment.capture(), eq(List.of("member-c")));
        assertEquals(notifiedComment.getValue().getUpdateTime(), created.getValue().getFirst().getCreateTime());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void removedRecipientCanReceiveANewTodoAndNoticeWhenMentionedAgain() {
        prepareOwnEdit();
        ApprovalCommentMention removed = mention("mention-old", "comment-a", "member-a");
        when(mentionMapper.select(any())).thenReturn(List.of(removed), List.of());
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a"), ORG)).thenReturn(List.of("member-a"));
        ApprovalCommentUpdateRequest request = updateRequest();

        service.update(request, USER, ORG);
        verifyNoInteractions(approvalCommentNoticeService);
        request.setMentionedUserIds(List.of("member-a"));
        service.update(request, USER, ORG);

        verify(mentionMapper).deleteByIds(List.of(removed.getId()));
        ArgumentCaptor<List<ApprovalCommentMention>> created = ArgumentCaptor.forClass((Class) List.class);
        verify(mentionMapper).batchInsert(created.capture());
        assertNotEquals(removed.getId(), created.getValue().getFirst().getId());
        assertTrue(created.getValue().getFirst().getCreateTime() > removed.getCreateTime());
        verify(approvalCommentNoticeService).sendMentionNotice(any(), eq(List.of("member-a")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings({"rawtypes", "unchecked"})
    void eachCommentNotifiesOnlyAfterCommitAndRollbackDoesNotNotify(boolean commit) {
        allowExistingPermission();
        when(extCommentMapper.selectEnabledUserIds(List.of("member-a"), ORG)).thenReturn(List.of("member-a"));
        when(extCommentMapper.selectDetail(anyString(), eq(ORG))).thenAnswer(invocation -> detail(invocation.getArgument(0)));
        ApprovalCommentAddRequest request = addRequest();
        request.setMentionedUserIds(List.of("member-a"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.add(request, USER, ORG);
            service.add(request, USER, ORG);

            verifyNoInteractions(approvalCommentNoticeService);
            ArgumentCaptor<ApprovalComment> comments = ArgumentCaptor.forClass(ApprovalComment.class);
            verify(commentMapper, times(2)).insert(comments.capture());
            assertNotEquals(comments.getAllValues().getFirst().getId(), comments.getAllValues().getLast().getId());
            assertTrue(comments.getAllValues().stream().allMatch(comment -> INSTANCE.equals(comment.getInstanceId())));
            ArgumentCaptor<List<ApprovalCommentMention>> mentions = ArgumentCaptor.forClass((Class) List.class);
            verify(mentionMapper, times(2)).batchInsert(mentions.capture());
            for (int i = 0; i < 2; i++) {
                assertEquals(comments.getAllValues().get(i).getId(), mentions.getAllValues().get(i).getFirst().getCommentId());
                assertEquals(comments.getAllValues().get(i).getCreateTime(), mentions.getAllValues().get(i).getFirst().getCreateTime());
            }
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            if (commit) {
                synchronizations.forEach(TransactionSynchronization::afterCommit);
                verify(approvalCommentNoticeService, times(2)).sendMentionNotice(any(), eq(List.of("member-a")));
            } else {
                synchronizations.forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
                verifyNoInteractions(approvalCommentNoticeService);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void mentionContextReturnsTheOriginalApprovalInstance() {
        instance.setResourceId("resource-a");
        when(extCommentMapper.selectMentionInstance("mention-a", USER, ORG)).thenReturn(instance);

        assertSame(instance, service.getMentionInstance("mention-a", "resource-a", USER, ORG));

        verify(extCommentMapper).selectMentionInstance("mention-a", USER, ORG);
        verifyNoInteractions(approvalResourceService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidMentionContextOrMismatchedResourceIsRejected(boolean existingMention) {
        if (existingMention) {
            instance.setResourceId("another-resource");
            when(extCommentMapper.selectMentionInstance("mention-a", USER, ORG)).thenReturn(instance);
        }

        assertThrows(GenericException.class, () -> service.getMentionInstance("mention-a", "resource-a", USER, ORG));

        verify(extCommentMapper).selectMentionInstance("mention-a", USER, ORG);
        verifyNoInteractions(approvalResourceService);
    }

    private void prepareOwnEdit() {
        allowExistingPermission();
        when(extCommentMapper.selectCommentForUpdate("comment-a", ORG)).thenReturn(comment("comment-a", USER));
        when(extCommentMapper.selectDetail("comment-a", ORG)).thenReturn(detail("comment-a"));
    }

    private ApprovalCommentMention mention(String id, String commentId, String userId) {
        ApprovalCommentMention mention = new ApprovalCommentMention();
        mention.setId(id);
        mention.setCommentId(commentId);
        mention.setUserId(userId);
        mention.setCreateTime(100L);
        return mention;
    }

    private void allowExistingPermission() {
        when(approvalResourceService.hasInstanceViewPermission(instance, USER, ORG)).thenReturn(true);
    }

    private ApprovalCommentPageRequest pageRequest() {
        ApprovalCommentPageRequest request = new ApprovalCommentPageRequest();
        request.setInstanceId(INSTANCE);
        request.setCurrent(1);
        request.setPageSize(10);
        return request;
    }

    private ApprovalCommentAddRequest addRequest() {
        ApprovalCommentAddRequest request = new ApprovalCommentAddRequest();
        request.setInstanceId(INSTANCE);
        request.setContent("评论内容");
        return request;
    }

    private ApprovalCommentUpdateRequest updateRequest() {
        ApprovalCommentUpdateRequest request = new ApprovalCommentUpdateRequest();
        request.setId("comment-a");
        request.setContent("修改后的评论");
        request.setMentionedUserIds(List.of());
        return request;
    }

    private ApprovalComment comment(String id, String author) {
        ApprovalComment comment = new ApprovalComment();
        comment.setId(id);
        comment.setInstanceId(INSTANCE);
        comment.setOrganizationId(ORG);
        comment.setCreateUser(author);
        return comment;
    }

    private ApprovalCommentResponse detail(String id) {
        ApprovalCommentResponse response = new ApprovalCommentResponse();
        response.setId(id);
        response.setInstanceId(INSTANCE);
        response.setCreateUser(USER);
        return response;
    }
}
