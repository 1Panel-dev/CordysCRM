package cn.cordys.crm.clue.controller;

import cn.cordys.common.util.Translator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.clue.domain.ClueCapacity;
import cn.cordys.crm.system.dto.request.CapacityAddRequest;
import cn.cordys.crm.system.dto.request.CapacityUpdateRequest;
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
public class ClueCapacityControllerTests extends BaseTest {

	private static String CAPACITY_ID;

	@Test
	@Order(1)
	void add() throws Exception {
		CapacityAddRequest request = new CapacityAddRequest();
		request.setScopeIds(List.of("admin"));
		request.setCapacity(10);
		this.requestPostWithOk("/lead-capacity/add", request);
		MvcResult mvcResult = this.requestPost("/lead-capacity/add", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("capacity.scope.duplicate"));
	}

	@Test
	@Order(2)
	void page() throws Exception {
		MvcResult mvcResult = this.requestGetWithOkAndReturn("/lead-capacity/get");
		List<ClueCapacity> result = getResultDataArray(mvcResult, ClueCapacity.class);
		assert result.size() == 1;
		CAPACITY_ID = result.getFirst().getId();
	}

	@Test
	@Order(3)
	void update() throws Exception {
		CapacityUpdateRequest request = new CapacityUpdateRequest();
		request.setId("not-exist");
		request.setScopeIds(List.of("admin"));
		request.setCapacity(100);
		MvcResult mvcResult = this.requestPost("/lead-capacity/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("capacity.not.exist"));
		request.setId(CAPACITY_ID);
		this.requestPost("/lead-capacity/update", request);
	}

	@Test
	@Order(4)
	void delete() throws Exception {
		this.requestGetWithOk("/lead-capacity/delete/" + CAPACITY_ID);
	}
}
