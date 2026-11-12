package cn.cordys.crm.system.notice;

import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.common.constants.TopicConstants;
import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.domain.Announcement;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.dto.convert.AnnouncementContentDTO;
import cn.cordys.crm.system.dto.request.AnnouncementRequest;
import cn.cordys.crm.system.dto.response.NotificationDTO;
import cn.cordys.crm.system.mapper.ExtNotificationMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.dto.NoticeRedisMessage;
import cn.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import cn.cordys.crm.system.service.AnnouncementService;
import cn.cordys.crm.system.service.NotificationService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("unchecked")
class NoticeTransactionTest {

    private final Connection connection = mock(Connection.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ZSetOperations<String, String> sorted = mock(ZSetOperations.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final MessagePublisher publisher = mock(MessagePublisher.class);
    private final ExtNotificationMapper notificationMapper = mock(ExtNotificationMapper.class);
    private final BaseMapper<Notification> notificationBaseMapper = mock(BaseMapper.class);
    private final BaseMapper<Announcement> announcementMapper = mock(BaseMapper.class);
    private NotificationService notifications;
    private AnnouncementService announcements;
    private InSiteNoticeSender sender;
    private DataSourceTransactionManager transactionManager;
    private TransactionTemplate transactions;
    private MockedStatic<IDGenerator> ids;

    enum Operation {
        READ, READ_ALL, ANNOUNCEMENT, IN_SITE
    }

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        transactionManager = new DataSourceTransactionManager(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(redis.opsForValue()).thenReturn(values);
        when(notificationMapper.updateByIdAndReceiver(any())).thenReturn(1);
        when(notificationMapper.updateByReceiver(any())).thenReturn(2);

        NotificationService notificationTarget = new NotificationService();
        injectRedis(notificationTarget);
        ReflectionTestUtils.setField(notificationTarget, "extNotificationMapper", notificationMapper);
        notifications = transactionalProxy(notificationTarget);
        AnnouncementService announcementTarget = new AnnouncementService();
        injectRedis(announcementTarget);
        ReflectionTestUtils.setField(announcementTarget, "notificationBaseMapper", notificationBaseMapper);
        ReflectionTestUtils.setField(announcementTarget, "announcementMapper", announcementMapper);
        ReflectionTestUtils.setField(announcementTarget, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(announcementTarget, "userMapper", mock(ExtUserMapper.class));
        announcements = transactionalProxy(announcementTarget);
        sender = new InSiteNoticeSender();
        injectRedis(sender);
        ReflectionTestUtils.setField(sender, "notificationBaseMapper", notificationBaseMapper);

        ids = mockStatic(IDGenerator.class, withSettings().mockMaker(MockMakers.INLINE));
        AtomicInteger sequence = new AtomicInteger();
        ids.when(IDGenerator::nextStr).thenAnswer(invocation -> "notice-" + sequence.incrementAndGet());
        OperationLogContext.putEmptySpan();
    }

    @AfterEach
    void tearDown() {
        ids.close();
        OperationLogContext.clear();
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void updatesCacheAndBroadcastsOnlyAfterOuterCommit(Operation operation) throws Exception {
        transactions.executeWithoutResult(status -> {
            perform(operation);
            verifyNoInteractions(redis, publisher);
        });

        var order = inOrder(connection, values, publisher);
        order.verify(connection).commit();
        String expectedRead = operation == Operation.READ || operation == Operation.READ_ALL ? "True" : "False";
        order.verify(values).set("user_read:receiver", expectedRead);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        order.verify(publisher).publish(eq(TopicConstants.SSE_TOPIC), message.capture());
        NoticeRedisMessage notice = JSON.parseObject(message.getValue(), NoticeRedisMessage.class);
        assertEquals("receiver", notice.getMessage());
        assertEquals(switch (operation) {
            case READ, READ_ALL -> "READ";
            case ANNOUNCEMENT -> "ANNOUNCEMENT_NOTICE";
            case IN_SITE -> "SYSTEM_NOTICE";
        }, notice.getNoticeType());
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void rollbackLeavesRedisAndClientsUntouched(Operation operation) throws Exception {
        transactions.executeWithoutResult(status -> {
            perform(operation);
            status.setRollbackOnly();
        });

        verify(connection).rollback();
        verify(connection, never()).commit();
        verifyNoInteractions(redis, publisher);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void redisFailureDoesNotReportCommittedOperationAsFailed(Operation operation) throws Exception {
        when(redis.opsForZSet()).thenThrow(new IllegalStateException("Redis unavailable"));

        assertDoesNotThrow(() -> transactions.executeWithoutResult(status -> perform(operation)));

        verify(connection).commit();
        verify(connection, never()).rollback();
        verifyNoInteractions(publisher);
    }

    @Test
    void failedDatabaseCommitDoesNotTouchRedis() throws Exception {
        doThrow(new SQLException("Commit failed")).when(connection).commit();

        assertThrows(RuntimeException.class, () -> notifications.readAll("org-a", "receiver"));

        verifyNoInteractions(redis, publisher);
    }

    @Test
    void unauthorizedReadDoesNotInvalidateCacheOrBroadcast() {
        when(notificationMapper.updateByIdAndReceiver(any())).thenReturn(0);

        assertEquals(0, notifications.read("notice-1", "receiver", "org-a"));

        ArgumentCaptor<Notification> record = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateByIdAndReceiver(record.capture());
        assertEquals("receiver", record.getValue().getReceiver());
        assertEquals("org-a", record.getValue().getOrganizationId());
        verifyNoInteractions(redis, publisher);
    }

    @Test
    void remainingUnreadMessagesKeepUnreadFlag() {
        when(notificationMapper.countByReceiver(any())).thenReturn(1);

        assertEquals(1, notifications.read("notice-1", "receiver", "org-a"));

        verify(redis).delete("msg_content:notice-1");
        verifyNoInteractions(values, publisher);
    }

    @Test
    void readAllClearsExistingCachedMessagesAfterCommit() throws Exception {
        when(sorted.range("announce_user:receiver", 0, -1)).thenReturn(Set.of("announcement-notice"));
        when(sorted.range("msg_user:receiver", 0, -1)).thenReturn(Set.of("system-notice"));

        assertEquals(2, notifications.readAll("org-a", "receiver"));

        var order = inOrder(connection, redis);
        order.verify(connection).commit();
        order.verify(redis).delete("announce_content:announcement-notice");
        order.verify(redis).delete("msg_content:system-notice");
        ArgumentCaptor<Notification> record = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).updateByReceiver(record.capture());
        assertEquals("receiver", record.getValue().getReceiver());
        assertEquals("org-a", record.getValue().getOrganizationId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"公告内容", "<p>中文🙂\"引号\"\\路径\n换行</p>", ""})
    void scheduledAnnouncementEntryHasItsOwnTransactionAndPreservesPayload(String content) throws Exception {
        when(notificationBaseMapper.batchInsert(anyList())).thenAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            verifyNoInteractions(redis, publisher);
            return 1;
        });

        Announcement announcement = announcement();
        announcement.setContent(content.getBytes(StandardCharsets.UTF_8));
        announcement.setUrl("https://example.com/?name=公告&source=通知");
        announcement.setRenameUrl(content.isEmpty() ? null : "查看\"公告\"🙂");
        announcements.convertNotification("author", announcement, List.of("receiver"));

        var order = inOrder(connection, values, publisher);
        order.verify(connection).commit();
        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        order.verify(values).set(eq("announce_content:notice-1"), cached.capture());
        order.verify(publisher).publish(eq(TopicConstants.SSE_TOPIC), anyString());
        NotificationDTO notification = JSON.parseObject(cached.getValue(), NotificationDTO.class);
        assertEquals("receiver", notification.getReceiver());
        assertEquals("org-a", notification.getOrganizationId());
        AnnouncementContentDTO actual = JSON.parseObject(notification.getContentText(), AnnouncementContentDTO.class);
        assertEquals(content, actual.getContent());
        assertEquals(announcement.getUrl(), actual.getUrl());
        assertEquals(announcement.getRenameUrl(), actual.getRenameUrl());
        assertEquals(new String(notification.getContent(), StandardCharsets.UTF_8), notification.getContentText());
    }

    @Test
    void laterAnnouncementBatchFailureSuppressesEarlierBatchBroadcasts() throws Exception {
        when(notificationBaseMapper.batchInsert(anyList())).thenReturn(50)
                .thenThrow(new IllegalStateException("Second batch failed"));
        List<String> receivers = IntStream.range(0, 51).mapToObj(i -> "receiver-" + i).toList();

        assertThrows(IllegalStateException.class,
                () -> announcements.convertNotification("author", announcement(), receivers));

        verify(notificationBaseMapper, times(2)).batchInsert(anyList());
        verify(connection).rollback();
        verifyNoInteractions(redis, publisher);
    }

    @Test
    void announcementInsertFailureRollsBackPendingNotifications() throws Exception {
        when(announcementMapper.insert(any())).thenThrow(new IllegalStateException("Announcement insert failed"));

        assertThrows(IllegalStateException.class, () -> announcements.add(announcementRequest(), "author"));

        verify(notificationBaseMapper).batchInsert(anyList());
        verify(connection).rollback();
        verifyNoInteractions(redis, publisher);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void announcementDeletionInvalidatesCacheOnlyOnCommit(boolean commit) throws Exception {
        when(announcementMapper.selectByPrimaryKey("announcement")).thenReturn(announcement());
        NotificationDTO old = new NotificationDTO();
        old.setId("old-notice");
        old.setReceiver("receiver");
        when(notificationMapper.selectByAnyOne(any())).thenReturn(List.of(old));

        transactions.executeWithoutResult(status -> {
            announcements.delete("announcement");
            verifyNoInteractions(redis, publisher);
            if (!commit) {
                status.setRollbackOnly();
            }
        });

        if (commit) {
            var order = inOrder(connection, redis);
            order.verify(connection).commit();
            order.verify(redis).delete("announce_content:old-notice");
        } else {
            verify(connection).rollback();
            verifyNoInteractions(redis);
        }
        verifyNoInteractions(publisher);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void announcementEditRemovesOldCacheBeforePublishingReplacementOnlyOnCommit(boolean commit) throws Exception {
        Announcement original = announcement();
        original.setNotice(true);
        original.setReceiveType("{}".getBytes(StandardCharsets.UTF_8));
        when(announcementMapper.selectByPrimaryKey("announcement")).thenReturn(original);
        NotificationDTO old = new NotificationDTO();
        old.setId("old-notice");
        old.setReceiver("receiver");
        when(notificationMapper.selectByAnyOne(any())).thenReturn(List.of(old));
        AnnouncementRequest request = announcementRequest();
        request.setId("announcement");

        transactions.executeWithoutResult(status -> {
            announcements.update(request, "author");
            verifyNoInteractions(redis, publisher);
            if (!commit) {
                status.setRollbackOnly();
            }
        });

        if (commit) {
            var order = inOrder(connection, redis, values, publisher);
            order.verify(connection).commit();
            order.verify(redis).delete("announce_content:old-notice");
            order.verify(values).set(eq("announce_content:notice-1"), anyString());
            order.verify(publisher).publish(eq(TopicConstants.SSE_TOPIC), anyString());
        } else {
            verify(connection).rollback();
            verifyNoInteractions(redis, publisher);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void inactiveAnnouncementDoesNotBroadcast(boolean expired) throws Exception {
        Announcement announcement = announcement();
        if (expired) {
            announcement.setEndTime(0L);
        } else {
            announcement.setStartTime(Long.MAX_VALUE);
        }

        announcements.convertNotification("author", announcement, List.of("receiver"));

        verify(notificationBaseMapper).batchInsert(anyList());
        verify(connection).commit();
        verifyNoInteractions(redis, publisher);
    }

    @Test
    void failedRecipientDoesNotSuppressFollowingCommittedRecipient() {
        when(redis.opsForZSet()).thenThrow(new IllegalStateException("Temporary Redis failure")).thenReturn(sorted);

        announcements.convertNotification("author", announcement(), List.of("first", "receiver"));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(eq(TopicConstants.SSE_TOPIC), message.capture());
        assertEquals("receiver", JSON.parseObject(message.getValue(), NoticeRedisMessage.class).getMessage());
    }

    @Test
    void nonTransactionalInSiteSendStillSavesBeforePublishing() {
        perform(Operation.IN_SITE);

        var order = inOrder(notificationBaseMapper, sorted, publisher);
        order.verify(notificationBaseMapper).insert(any());
        order.verify(sorted).add(eq("msg_user:receiver"), eq("notice-1"), anyDouble());
        order.verify(publisher).publish(eq(TopicConstants.SSE_TOPIC), anyString());
    }

    private void perform(Operation operation) {
        switch (operation) {
            case READ -> notifications.read("notice-1", "receiver", "org-a");
            case READ_ALL -> notifications.readAll("org-a", "receiver");
            case ANNOUNCEMENT -> announcements.convertNotification("author", announcement(), List.of("receiver"));
            case IN_SITE -> {
                MessageDetailDTO detail = new MessageDetailDTO();
                detail.setOrganizationId("org-a");
                detail.setTaskType("APPROVAL");
                NoticeModel notice = NoticeModel.builder().operator("author").event("APPROVAL_COMMENT_MENTIONED")
                        .paramMap(Map.of("resourceId", "comment-a"))
                        .receivers(List.of(new Receiver("receiver", "SYSTEM_NOTICE"))).build();
                sender.sendAnnouncement(detail, notice, "审批评论中@了您", "审批评论@提醒");
            }
        }
    }

    private Announcement announcement() {
        Announcement announcement = new Announcement();
        announcement.setId("announcement");
        announcement.setOrganizationId("org-a");
        announcement.setSubject("公告标题");
        announcement.setContent("公告内容".getBytes(StandardCharsets.UTF_8));
        announcement.setStartTime(0L);
        announcement.setEndTime(Long.MAX_VALUE);
        return announcement;
    }

    private AnnouncementRequest announcementRequest() {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setOrganizationId("org-a");
        request.setSubject("公告标题");
        request.setContent("公告内容");
        request.setStartTime(0L);
        request.setEndTime(Long.MAX_VALUE);
        request.setUserIds(List.of("receiver"));
        return request;
    }

    private void injectRedis(Object target) {
        ReflectionTestUtils.setField(target, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(target, "messagePublisher", publisher);
    }

    private <T> T transactionalProxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
}
