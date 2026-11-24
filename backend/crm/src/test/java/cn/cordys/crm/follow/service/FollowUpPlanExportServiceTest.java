package cn.cordys.crm.follow.service;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FollowUpPlanExportServiceTest {

    @ParameterizedTest
    @CsvSource(value = { "zh-CN,true,是", "zh-CN,false,否", "en-US,true,Yes", "en-US,false,No", "en-US,null,No" },
            nullValues = "null")
    void exportsConvertedAsLocalizedText(String languageTag, Boolean converted, String expected) {
        MessageSource original = (MessageSource) ReflectionTestUtils.getField(Translator.class, "messageSource");
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/cordys-crm");
        messages.setDefaultEncoding("UTF-8");
        ReflectionTestUtils.setField(Translator.class, "messageSource", messages);
        try {
            FollowUpPlanListResponse plan = new FollowUpPlanListResponse();
            plan.setConverted(converted);
            Map<String, Object> fields = ReflectionTestUtils.invokeMethod(new FollowUpPlanExportService(),
                    "getSystemFieldMap", plan, Locale.forLanguageTag(languageTag));
            assertEquals(expected, fields.get("converted"));
        } finally {
            ReflectionTestUtils.setField(Translator.class, "messageSource", original);
        }
    }
}
