package io.cordys.crm.system.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.dto.field.SelectFieldProp;
import io.cordys.crm.system.dto.form.FormProp;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.service.ModuleFormService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleFormControllerTests extends BaseTest{

	@Resource
	private ModuleFormService moduleFormService;

	@Test
	@Order(0)
	void init() {
		moduleFormService.initForm();
	}

	@Test
	@Order(1)
	void testSaveFields() throws Exception {
		ModuleFormSaveRequest request = new ModuleFormSaveRequest();
		request.setFormKey("none-key");
		request.setFields(List.of());
		request.setDeleteFieldIds(List.of());
		request.setFormProp(new FormProp());
		this.requestPost("/module/form/save", request).andExpect(status().is5xxServerError());
		request.setFormKey(FormKey.CUSTOMER.getKey());
		request.setDeleteFieldIds(List.of("default-delete-id"));
		ModuleFieldDTO field = new ModuleFieldDTO();
		SelectFieldProp selectFieldProp = new SelectFieldProp();
		selectFieldProp.setType(FieldType.SELECT.name());
		field.setFieldProp(selectFieldProp);
		request.setFields(List.of(field));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/form/save", request);
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() > 1;
		ModuleFieldDTO saveField = formConfig.getFields().getFirst();
		saveField.setFieldProp(selectFieldProp);
		request.setFields(List.of(saveField));
		this.requestPostWithOk("/module/form/save", request);
	}

	@Test
	@Order(2)
	void testGetFieldList() throws Exception {
		MvcResult mvcResult = this.requestGetWithOkAndReturn("/module/form/config/" + FormKey.CUSTOMER.getKey());
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() > 1;
	}
}
