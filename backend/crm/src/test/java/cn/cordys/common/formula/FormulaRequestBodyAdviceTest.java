package cn.cordys.common.formula;

import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
class FormulaRequestBodyAdviceTest {

    private final RecordingCompletionService completionService = new RecordingCompletionService();
    private final FormulaRequestBodyAdvice advice = new FormulaRequestBodyAdvice(completionService);

    @Test
    void completesQuotationAddAfterDeserialization() {
        Object body = new Object();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/opportunity/quotation/add");

        Object result = advice.afterBodyRead(body, new ServletServerHttpRequest(request),
                null, Object.class, null);

        assertSame(body, result);
        assertSame(body, completionService.body);
        org.junit.jupiter.api.Assertions.assertEquals("quotation", completionService.formKey);
        assertTrue(completionService.createMode);
    }

    @Test
    void completesQuotationUpdateInUpdateMode() {
        Object body = new Object();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/opportunity/quotation/update");

        Object result = advice.afterBodyRead(body, new ServletServerHttpRequest(request),
                null, Object.class, null);

        assertSame(body, result);
        assertSame(body, completionService.body);
        org.junit.jupiter.api.Assertions.assertEquals("quotation", completionService.formKey);
        assertFalse(completionService.createMode);
    }

    @Test
    void ignoresCustomFormDataAddBecauseDynamicFormsAreCompletedInService() {
        CustomFormDataAddRequest body = new CustomFormDataAddRequest();
        body.setCustomFormId("custom-form-1");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/custom-form/data/add");

        Object result = advice.afterBodyRead(body, new ServletServerHttpRequest(request),
                null, CustomFormDataAddRequest.class, null);

        assertSame(body, result);
        org.junit.jupiter.api.Assertions.assertNull(completionService.body);
    }

    @Test
    void ignoresCustomFormDataUpdateBecauseDynamicFormsAreCompletedInService() {
        CustomFormDataUpdateRequest body = new CustomFormDataUpdateRequest();
        body.setCustomFormId("custom-form-1");
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/custom-form/data/update");

        Object result = advice.afterBodyRead(body, new ServletServerHttpRequest(request),
                null, CustomFormDataUpdateRequest.class, null);

        assertSame(body, result);
        org.junit.jupiter.api.Assertions.assertNull(completionService.body);
    }

    @Test
    void ignoresNonFormUpdateRequests() {
        Object body = new Object();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/system/organization/update");

        Object result = advice.afterBodyRead(body, new ServletServerHttpRequest(request),
                null, Object.class, null);

        assertSame(body, result);
        org.junit.jupiter.api.Assertions.assertNull(completionService.body);
    }

    private static class RecordingCompletionService extends FormulaRequestCompletionService {

        private String formKey;
        private Object body;
        private boolean createMode;

        RecordingCompletionService() {
            super(null, null);
        }

        @Override
        public void completeAuthoritative(
                String formKey,
                Object request,
                boolean createMode
        ) {
            this.formKey = formKey;
            this.body = request;
            this.createMode = createMode;
        }
    }
}
