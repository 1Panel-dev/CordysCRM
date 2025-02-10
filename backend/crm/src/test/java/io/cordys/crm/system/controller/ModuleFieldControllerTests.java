package io.cordys.crm.system.controller;

import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleFieldOption;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleFieldSaveRequest;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
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
public class ModuleFieldControllerTests extends BaseTest{

	@Test
	@Order(1)
	void testSaveFields() throws Exception {
		// empty param
		ModuleFieldSaveRequest request = new ModuleFieldSaveRequest();
		request.setModuleId("default-module");
		request.setFields(List.of());
		request.setDeleteFieldIds(List.of());
		this.requestPostWithOk("/module/field/save", request);
		request.setDeleteFieldIds(List.of("default-delete-id"));
		request.setFields(List.of(buildField()));
		ModuleForm form = buildForm();
		request.setForm(form);
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/field/save", request);
		List<ModuleFieldDTO> fields = getResultDataArray(mvcResult, ModuleFieldDTO.class);
		assert fields.size() == 1;
		ModuleFieldDTO saveField = fields.getFirst();
		saveField.setName("default-name-update");
		ModuleFieldOption option = ModuleFieldOption.builder().fieldKey("default-key").fieldValue("default-value").build();
		saveField.setOptions(List.of(option));
		request.setFields(List.of(saveField));
		form.setId("default-form-id");
		request.setForm(form);
		this.requestPostWithOk("/module/field/save", request);
	}

	@Test
	@Order(2)
	void testGetFieldList() throws Exception {
		ModuleFieldRequest request = new ModuleFieldRequest();
		request.setModuleId("default-module");
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/field/list", request);
		List<ModuleFieldDTO> fields = getResultDataArray(mvcResult, ModuleFieldDTO.class);
		assert fields.size() == 1;
	}

	private ModuleFieldDTO buildField() {
		ModuleField field = ModuleField.builder()
				.moduleId("default-module").name("default-name").type("select").pos(1L)
				.tooltip("default-tooltip").required(true).unique(true).fieldWidth("half")
				.defaultValue("default-value")
				.build();
		ModuleFieldDTO dto = new ModuleFieldDTO();
		BeanUtils.copyBean(dto, field);
		return dto;
	}

	private ModuleForm buildForm() {
		return ModuleForm.builder()
				.moduleId("default-module").frontCache(true).layout("layout")
				.labelPos("middle").labelWidth("default")
				.labelAlignment("left").showDesc(true)
				.inputWidth("default").optBtnPos("middle")
				.saveBtn(true).saveContinueBtn(true).cancelBtn(true)
				.build();
	}
}
