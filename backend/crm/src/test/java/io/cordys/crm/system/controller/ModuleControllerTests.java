package io.cordys.crm.system.controller;

import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.response.ModuleDTO;
import io.cordys.crm.system.service.ModuleService;
import jakarta.annotation.Resource;
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
public class ModuleControllerTests extends BaseTest{

	@Resource
	private ModuleService moduleService;

	@Test
	@Order(1)
	public void testInitModuleList() {
		moduleService.initModule("default");
	}

	@Test
	@Order(2)
	public void testGetModuleListAndSwitch() throws Exception {
		ModuleRequest request = new ModuleRequest();
		request.setOrganizationId("default");
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/module/list", request);
		List<ModuleDTO> modules = getResultDataArray(mvcResult, ModuleDTO.class);
		assert !modules.isEmpty();
		String param = modules.getFirst().getId();
		this.requestGetWithOk("/module/switch/" + param);
		// switch not exist module
		MvcResult mvcResult1 = this.requestGet("/module/switch/" + "none").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("module.not_exist"));
	}
}
