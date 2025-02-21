package io.cordys.crm.lead.controller;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.system.dto.request.CapacityRequest;
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
public class LeadCapacityControllerTests extends BaseTest {

	@Test
	@Order(1)
	void add() throws Exception {
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(10);
		this.requestPostWithOk("/lead-capacity/save", List.of(capacity));
	}

	@Test
	@Order(2)
	void page() throws Exception {
		MvcResult mvcResult = this.requestGetWithOkAndReturn("/lead-capacity/list");
		List<LeadCapacity> result = getResultDataArray(mvcResult, LeadCapacity.class);
		assert result.size() == 1;
	}

	@Test
	@Order(3)
	void update() throws Exception {
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(100);
		this.requestPostWithOk("/lead-capacity/save", List.of(capacity));
	}
}
