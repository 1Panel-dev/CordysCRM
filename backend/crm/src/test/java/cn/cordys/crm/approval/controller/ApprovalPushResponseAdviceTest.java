package cn.cordys.crm.approval.controller;

import cn.cordys.common.response.handler.ResultResponseBodyAdvice;
import cn.cordys.crm.approval.dto.response.ApprovalPushResponse;
import cn.cordys.crm.form.domain.CustomFormData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApprovalPushResponseAdviceTest {

    @ParameterizedTest
    @CsvSource({ "false,false", "true,false", "true,true" })
    void updateResultPreservesBusinessFieldsAndOnlyAddsInvalidResubmitFlag(boolean hasPushResult, boolean invalid)
            throws Exception {
        ApprovalPushResponseAdvice advice = new ApprovalPushResponseAdvice();
        ReflectionTestUtils.setField(advice, "objectMapper", new JsonMapper());
        var mvc = MockMvcBuilders.standaloneSetup(new UpdateResultController())
                .setControllerAdvice(new ResultResponseBodyAdvice(), advice).build();
        var request = get("/approval-push-response-test");
        if (hasPushResult) {
            ApprovalPushResponse result = new ApprovalPushResponse();
            result.setResubmitNodeInvalid(invalid);
            request.requestAttr(ApprovalPushResponse.class.getName(), result);
        }

        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("record-1"))
                .andExpect(jsonPath("$.data.name").value("编辑后的记录"))
                .andExpect(invalid ? jsonPath("$.data.resubmitNodeInvalid").value(true)
                        : jsonPath("$.data.resubmitNodeInvalid").doesNotExist());
    }

    @RestController
    static class UpdateResultController {

        @GetMapping("/approval-push-response-test")
        public CustomFormData updateResult() {
            CustomFormData data = new CustomFormData();
            data.setId("record-1");
            data.setName("编辑后的记录");
            return data;
        }
    }
}
