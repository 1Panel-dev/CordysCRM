package cn.cordys.common.util;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.handler.ListTypeHandler;
import cn.cordys.common.response.handler.ResultResponseBodyAdvice;
import cn.cordys.config.RequestParamTrimConfig;
import cn.cordys.crm.opportunity.dto.request.OpportunityRuleAddRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JsonCompatibilityTest {

    @Test
    void sharedJsonPreservesDecimalsCollectionsAndStoredDates() {
        Map<String, Object> values = JSON.parseToMap("""
                {/* legacy comment */ "amount":12345678901234567890.12345678901234567890}
                """);
        assertEquals(new BigDecimal("12345678901234567890.12345678901234567890"), values.get("amount"));
        assertEquals(List.of("one"), JSON.parseArray("\"one\"", String.class));
        assertEquals(List.of("one"), ListTypeHandler.parseArray("\"one\"", String.class));
        assertEquals("[\"one\",\"two\"]", ListTypeHandler.toJSONString(List.of("one", "two")));
        assertEquals("[2026,10,21]", JSON.toJSONString(LocalDate.of(2026, 10, 21)));
        assertEquals(LocalDate.of(2026, 10, 21), JSON.parseObject("[2026,10,21]", LocalDate.class));
        assertEquals("JSON 反序列化失败", assertThrows(RuntimeException.class,
                () -> JSON.parseObject("{broken", Map.class)).getMessage());
    }

    @Test
    void jsonDiffStillFindsChangedAndAddedFields() throws Exception {
        var differences = JsonDifferenceUtils.compareJson("{\"amount\":10}", "{\"amount\":20,\"name\":\"new\"}",
                new ArrayList<>());
        assertEquals(List.of("modified", "add"), differences.stream().map(JsonDifferenceDTO::getType).toList());
    }

    @Test
    void jackson3KeepsResponseEnvelopeTrimXssAndMoneyFormatting() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(RequestParamTrimConfig.class)
                .withPropertyValues("spring.jackson.use-jackson2-defaults=true", "xss.protection.url.list=/compat/escape")
                .run(context -> {
                    var rule = context.getBean(JsonMapper.class).readValue("""
                            {"name":"  rule  ","scopeIds":["cc"],"ownerIds":["cc"],"enable":true,"auto":false}
                            """, OpportunityRuleAddRequest.class);
                    assertEquals("rule", rule.getName());
                    assertEquals(List.of("cc"), rule.getScopeIds());
                    var mvc = MockMvcBuilders.standaloneSetup(new EchoController())
                            .setMessageConverters(new JacksonJsonHttpMessageConverter(context.getBean(JsonMapper.class)))
                            .setControllerAdvice(new ResultResponseBodyAdvice())
                            .build();
                    mvc.perform(post("/compat/plain").contentType(MediaType.APPLICATION_JSON).content("""
                            {"text":"  hello  ","amount":123.4500,"date":"2026-10-21","unknown":true}
                            """))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data.text").value("hello"))
                            .andExpect(jsonPath("$.data.amount").value(123.45))
                            .andExpect(jsonPath("$.data.date").value("2026-10-21"))
                            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"amount\":123.45")));
                    mvc.perform(post("/compat/escape").contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"text\":\"  <b>hello</b>  \"}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data.text").value("&lt;b&gt;hello&lt;/b&gt;"));
                    mvc.perform(post("/compat/plain").contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"text\":null}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.data.text").doesNotExist());
                });
    }

    @RestController
    static class EchoController {
        @PostMapping({ "/compat/plain", "/compat/escape" })
        Payload echo(@RequestBody Payload payload) {
            return payload;
        }
    }

    record Payload(String text, @JsonSerialize(using = BigDecimalNoTrailingZeroSerializer.class) BigDecimal amount,
            LocalDate date) {
    }
}
