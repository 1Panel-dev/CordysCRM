package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.dto.request.ModuleRequest;
import io.cordys.crm.system.dto.request.ModuleSortRequest;
import io.cordys.crm.system.dto.response.ModuleDTO;
import io.cordys.crm.system.service.ModuleService;
import io.cordys.mybatis.BaseMapper;
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
public class ModuleControllerTests extends BaseTest {

	private static final String LIST_ALL = "/module/list";
	private static final String SWITCH = "/module/switch/";
	private static final String SORT = "/module/sort";

	@Resource
	private ModuleService moduleService;
	@Resource
	private BaseMapper<Module> moduleMapper;

	@Test
	@Order(1)
	void testInitModuleList() {
		moduleService.initModule("default");
		List<Module> modules = moduleMapper.selectAll("pos");
		assert !modules.isEmpty();
	}

	@Test
	@Order(2)
	void testGetModuleListAndSwitch() throws Exception {
		ModuleRequest request = new ModuleRequest();
		request.setOrganizationId("default");
		MvcResult mvcResult = this.requestPostWithOkAndReturn(LIST_ALL, request);
		List<ModuleDTO> modules = getResultDataArray(mvcResult, ModuleDTO.class);
		assert !modules.isEmpty();
		String param = modules.getFirst().getId();
		this.requestGetWithOk(SWITCH + param);
		// permission check
		requestGetPermissionTest(PermissionConstants.MODULE_SETTING_UPDATE, SWITCH + param);
		// switch not exist module
		MvcResult mvcResult1 = this.requestGet(SWITCH + "none").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("module.not_exist"));
		ModuleSortRequest sortRequest = new ModuleSortRequest();
		sortRequest.setDragModuleId(param);
		sortRequest.setStart(1L);
		sortRequest.setEnd(3L);
		this.requestPostWithOk(SORT, sortRequest);
		sortRequest.setStart(3L);
		sortRequest.setEnd(1L);
		this.requestPostWithOk(SORT, sortRequest);
		// permission check
		requestPostPermissionTest(PermissionConstants.MODULE_SETTING_UPDATE, SORT, sortRequest);
	}
}
