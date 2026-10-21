package cn.cordys.crm.system.notice.sender.insite;

import cn.cordys.common.redis.MessagePublisher;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.system.domain.Notification;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class InSiteNoticeSenderTest {

    @ParameterizedTest
    @CsvSource(value = {"CONTRACT,CONTRACT", "NULL,APPROVAL"}, nullValues = "NULL")
    @SuppressWarnings("unchecked")
    void savesExplicitMessageCategoryOrFallsBackToConfigurationModule(String resourceType, String expectedType) {
        InSiteNoticeSender sender = new InSiteNoticeSender();
        BaseMapper<Notification> mapper = mock(BaseMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> sorted = mock(ZSetOperations.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForZSet()).thenReturn(sorted);
        when(redis.opsForValue()).thenReturn(values);
        ReflectionTestUtils.setField(sender, "notificationBaseMapper", mapper);
        ReflectionTestUtils.setField(sender, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(sender, "messagePublisher", mock(MessagePublisher.class));
        MessageDetailDTO detail = new MessageDetailDTO();
        detail.setTaskType("APPROVAL");
        detail.setOrganizationId("org-a");
        Map<String, Object> params = new HashMap<>();
        params.put("resourceId", "comment-a");
        if (resourceType != null) {
            params.put("resourceType", resourceType);
        }
        NoticeModel notice = NoticeModel.builder()
                .operator("author")
                .event("APPROVAL_COMMENT_MENTIONED")
                .paramMap(params)
                .receivers(List.of(new Receiver("receiver", "SYSTEM_NOTICE")))
                .build();

        try (var ids = mockStatic(IDGenerator.class, withSettings().mockMaker(MockMakers.INLINE))) {
            ids.when(IDGenerator::nextStr).thenReturn("notice-a");

            sender.sendAnnouncement(detail, notice, "审批评论中@了您", "审批评论@提醒");
        }

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(mapper).insert(saved.capture());
        assertEquals(expectedType, saved.getValue().getResourceType());
        assertEquals("UNREAD", saved.getValue().getStatus());
        assertEquals("receiver", saved.getValue().getReceiver());
        assertEquals("org-a", saved.getValue().getOrganizationId());
        assertEquals("comment-a", saved.getValue().getResourceId());
        assertEquals("APPROVAL", detail.getTaskType());
    }
}
