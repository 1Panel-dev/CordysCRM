package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.InternalDetailTab;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.dto.response.RelatedFormDTO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleFormControllerTests extends BaseTest {

    public static List<BaseField> fields;

    @Test
    @Order(1)
    void testGetFieldList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn("/module/form/config/" + FormKey.CLUE.getKey());
        ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
        assert formConfig.getFields().size() > 1;
        fields = formConfig.getFields();
    }

    @Test
    @Order(2)
    void testGetRelatedFormsAndDetailTabOptions() throws Exception {
        MvcResult relatedResult = this.requestGetWithOkAndReturn("/module/form/related/" + FormKey.CUSTOMER.getKey());
        List<RelatedFormDTO> relatedForms = getResultDataArray(relatedResult, RelatedFormDTO.class);
        assertTrue(relatedForms.stream().allMatch(form -> form.getName() != null && !form.getName().isBlank()));
        assertTrue(relatedForms.stream().anyMatch(option -> FormKey.OPPORTUNITY.getKey().equals(option.getId())));
        assertTrue(relatedForms.stream().anyMatch(option -> FormKey.CONTACT.getKey().equals(option.getId())));
        assertTrue(relatedForms.stream().anyMatch(form ->
                InternalDetailTab.CUSTOMER_OWNER_RECORD.name().equals(form.getInternalKey())
                        && form.getInternalKey().equals(form.getId()) && form.getSourceTypeFields().isEmpty()));
        assertTrue(relatedForms.stream().anyMatch(form ->
                InternalDetailTab.CUSTOMER_INVOICE.name().equals(form.getInternalKey())
                        && FormKey.INVOICE.getKey().equals(form.getId()) && form.getSourceTypeFields().isEmpty()));

        MvcResult configResult = this.requestGetWithOkAndReturn("/module/form/config/" + FormKey.CUSTOMER.getKey());
        ModuleFormConfigDTO formConfig = getResultData(configResult, ModuleFormConfigDTO.class);
        assertNotNull(formConfig.getFormProp().getDetailTabs());
        assertFalse(formConfig.getFormProp().getDetailTabs().isEmpty());
        assertTrue(formConfig.getFormProp().getDetailTabs().stream().allMatch(tab ->
                tab.getName() != null
                        && (tab.getRelatedForm() == null || tab.getRelatedForm().getName() != null)
                        && (tab.getRelatedField() == null || tab.getRelatedField().getName() != null)));
        assertTrue(formConfig.getFormProp().getDetailTabs().stream().anyMatch(tab ->
                InternalDetailTab.CUSTOMER_INVOICE.name().equals(tab.getInternalKey())
                        && tab.getRelatedForm() != null && tab.getRelatedField() == null));
        assertTrue(formConfig.getFormProp().getDetailTabs().stream().anyMatch(tab ->
                InternalDetailTab.CUSTOMER_OWNER_RECORD.name().equals(tab.getInternalKey())
                        && tab.getRelatedForm() == null && tab.getRelatedField() == null));

        MvcResult clueConfigResult = this.requestGetWithOkAndReturn("/module/form/config/" + FormKey.CLUE.getKey());
        ModuleFormConfigDTO clueConfig = getResultData(clueConfigResult, ModuleFormConfigDTO.class);
        assertTrue(clueConfig.getFormProp().getDetailTabs().stream().anyMatch(tab ->
                InternalDetailTab.CLUE_OWNER_RECORD.name().equals(tab.getInternalKey())));

        MvcResult opportunityConfigResult = this.requestGetWithOkAndReturn(
                "/module/form/config/" + FormKey.OPPORTUNITY.getKey());
        ModuleFormConfigDTO opportunityConfig = getResultData(opportunityConfigResult, ModuleFormConfigDTO.class);
        assertTrue(opportunityConfig.getFormProp().getDetailTabs().stream().anyMatch(tab ->
                InternalDetailTab.OPPORTUNITY_CONTACT.name().equals(tab.getInternalKey())
                        && tab.getRelatedForm() != null && tab.getRelatedField() == null));
    }

    @Test
    @Order(3)
    void testSaveFields() throws Exception {
        ModuleFormSaveRequest request = new ModuleFormSaveRequest();
        request.setFormKey("none-key");
        request.setFields(List.of());
        request.setFormProp(new FormProp());
        MvcResult mvcResult = this.requestPost("/module/form/save", request).andExpect(status().is4xxClientError()).andReturn();
        assert mvcResult.getResponse().getContentAsString().contains(Translator.get("http_result_not_found"));
        request.setFormKey(FormKey.CLUE.getKey());
        request.setFields(fields);
        this.requestPostWithOk("/module/form/save", request);
        BaseField field = new SelectField();
        field.setId("select-id");
        field.setType(FieldType.SELECT.name());
        request.setFields(List.of(field));
        MvcResult mvcResult1 = this.requestPost("/module/form/save", request).andExpect(status().is5xxServerError()).andReturn();
        assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("module.form.business_field.deleted"));
    }
}
