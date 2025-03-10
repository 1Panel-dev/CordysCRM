package io.cordys.crm.system.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.SelectField;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.dto.form.FormProp;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
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
public class ModuleFormControllerTests extends BaseTest{

	@Test
	@Order(1)
	void testSaveFields() throws Exception {
		ModuleFormSaveRequest request = new ModuleFormSaveRequest();
		request.setFormKey("none-key");
		request.setFields(List.of());
		request.setFormProp(new FormProp());
		this.requestPost("/module/form/save", request).andExpect(status().is5xxServerError());
		request.setFormKey(FormKey.CLUE.getKey());
		BaseField field = new SelectField();
		field.setId("select-id");
		field.setType(FieldType.SELECT.name());
		request.setFields(List.of(field));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/form/save", request);
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() == 1;
		BaseField saveField = formConfig.getFields().getFirst();
		field.setType(FieldType.SELECT.name());
		request.setFields(List.of(saveField));
		this.requestPostWithOk("/module/form/save", request);
	}

	@Test
	@Order(2)
	void testGetFieldList() throws Exception {
		MvcResult mvcResult = this.requestGetWithOkAndReturn("/module/form/config/" + FormKey.CLUE.getKey());
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() == 1;
	}
}
