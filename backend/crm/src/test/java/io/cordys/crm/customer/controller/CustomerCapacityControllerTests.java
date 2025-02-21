package io.cordys.crm.customer.controller;

import io.cordys.common.dto.SortRequest;
import io.cordys.common.pager.Pager;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.CustomerCapacity;
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
public class CustomerCapacityControllerTests extends BaseTest {

	@Test
	@Order(1)
	void add() throws Exception {
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(10);
		this.requestPostWithOk("/customer-capacity/save", List.of(capacity));
	}

	@Test
	@Order(2)
	void page() throws Exception {
		MvcResult mvcResult = this.requestGetWithOkAndReturn("/customer-capacity/get");
		List<CustomerCapacity> result = getResultDataArray(mvcResult, CustomerCapacity.class);
		assert result.size() == 1;
	}

	@Test
	@Order(3)
	void update() throws Exception {
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(100);
		this.requestPostWithOk("/customer-capacity/save", List.of(capacity));
	}
}
