package cn.cordys.crm.system.notice;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.NoticeSendContext;
import cn.cordys.crm.system.notice.message.MessageDetailService;
import cn.cordys.crm.system.notice.sender.insite.InSiteNoticeSender;
import cn.cordys.crm.system.notice.sender.mail.MailNoticeSender;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoticeLocaleTest {

    private static final String MODULE = NotificationConstants.Module.CUSTOMER;
    private static final String EVENT = NotificationConstants.Event.CUSTOMER_ADD;
    private static final String TEMPLATE = "message.customer_add_text";

    private final LocaleContext requestLocale = new SimpleTimeZoneAwareLocaleContext(
            Locale.SIMPLIFIED_CHINESE, TimeZone.getTimeZone("Asia/Shanghai"));
    private LocaleContext previousLocale;
    private MessageSource previousMessages;

    @BeforeEach
    void setUp() {
        previousLocale = LocaleContextHolder.getLocaleContext();
        previousMessages = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(TEMPLATE, Locale.US, "New account ${name}");
        messages.addMessage(TEMPLATE, Locale.SIMPLIFIED_CHINESE, "新客户 ${name}");
        new Translator().setMessageSource(messages);
        LocaleContextHolder.setLocaleContext(requestLocale);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.setLocaleContext(previousLocale);
        new Translator().setMessageSource(previousMessages);
    }

    @Test
    void templateNoticeRestoresCallingThreadContext() {
        MessageDetailService details = mock(MessageDetailService.class);
        NoticeSendService service = new NoticeSendService(
                mock(MailNoticeSender.class), mock(InSiteNoticeSender.class), details);

        service.sendOther(MODULE, NoticeModel.builder()
                .event(EVENT)
                .paramMap(Map.of("Language", "en-US", "organizationId", "org-a",
                        "useTemplate", "true", "templateKey", TEMPLATE))
                .build(), false);

        verify(details).searchMessageByTypeAndOrgId(MODULE, true, "New account ${name}", "org-a");
        assertSame(requestLocale, LocaleContextHolder.getLocaleContext());
    }

    @Test
    void resolvedNoticeClearsLocaleOnWorkerWithoutPreviousContext() {
        LocaleContextHolder.resetLocaleContext();
        InSiteNoticeSender inSite = mock(InSiteNoticeSender.class);
        NoticeSendService service = new NoticeSendService(
                mock(MailNoticeSender.class), inSite, mock(MessageDetailService.class));
        MessageDetailDTO detail = new MessageDetailDTO();
        detail.setSysEnable(true);
        NoticeModel notice = NoticeModel.builder().paramMap(Map.of("Language", "en-US")).build();
        doAnswer(invocation -> {
            assertEquals(Locale.US, LocaleContextHolder.getLocale());
            return null;
        }).when(inSite).send(detail, notice);

        service.sendNotificationAsync(detail, notice);

        verify(inSite).send(detail, notice);
        assertNull(LocaleContextHolder.getLocaleContext());
    }
}
