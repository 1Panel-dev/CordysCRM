package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.dto.request.ModuleFormRequest;
import io.cordys.crm.system.dto.request.ModuleFormSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.crm.system.dto.response.ModuleFieldOptionDTO;
import io.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import io.cordys.crm.system.dto.response.ModuleFormDTO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleFormControllerTests extends BaseTest{

	@Test
	@Order(1)
	void testSaveFields() throws Exception {
		// empty param
		ModuleFormSaveRequest request = new ModuleFormSaveRequest();
		ModuleFormDTO form = buildForm();
		request.setFormKey("lead");
		request.setOrganizationId("default-org");
		request.setFields(List.of());
		request.setDeleteFieldIds(List.of());
		request.setForm(form);
		this.requestPostWithOk("/module/form/save", request);
		request.setDeleteFieldIds(List.of("default-delete-id"));
		request.setFields(List.of(buildField()));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/form/save", request);
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() == 1;
		ModuleFieldDTO saveField = formConfig.getFields().getFirst();
		saveField.setName("default-name-update");
		ModuleFieldOptionDTO option = new ModuleFieldOptionDTO();
		option.setId("default-key");
		option.setLabel("default-label");
		saveField.setOptions(List.of(option));
		request.setFields(List.of(saveField));
		this.requestPostWithOk("/module/form/save", request);
	}

	@Test
	@Order(2)
	void testGetFieldList() throws Exception {
		ModuleFormRequest request = new ModuleFormRequest();
		request.setFormKey("lead");
		request.setOrganizationId("default-org");
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/form/config", request);
		ModuleFormConfigDTO formConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
		assert formConfig.getFields().size() == 1;
	}

	private ModuleFieldDTO buildField() {
		return ModuleFieldDTO.builder()
				.name("default-name").type("select")
				.showLabel(true).readable(true).editable(true).tooltip("default-tooltip")
				.fieldWidth("half").defaultValue("default-value").pos(1L)
				.build();
	}

	private ModuleFormDTO buildForm() {
		return ModuleFormDTO.builder()
				.frontCache(true).layout("layout")
				.labelPos("middle").labelWidth("default")
				.labelAlignment("left").showDesc(true)
				.inputWidth("default").optBtnPos("middle")
				.saveBtn(true).saveContinueBtn(true).cancelBtn(true)
				.build();
	}
}
