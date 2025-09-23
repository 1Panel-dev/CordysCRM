package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

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
    void testSaveFields() throws Exception {
        ModuleFormSaveRequest request = new ModuleFormSaveRequest();
        request.setFormKey("none-key");
        request.setFields(List.of());
        request.setFormProp(new FormProp());
        MvcResult mvcResult = this.requestPost("/module/form/save", request).andExpect(status().is5xxServerError()).andReturn();
        assert mvcResult.getResponse().getContentAsString().contains(Translator.get("module.form.not_exist"));
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
