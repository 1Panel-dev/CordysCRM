package io.cordys.crm.customer.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.dto.request.CustomerCapacitySaveRequest;
import io.cordys.crm.system.dto.request.CapacityRequest;
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
public class CustomerCapacityControllerTests extends BaseTest {

	@Test
	@Order(1)
	void add() throws Exception {
		CustomerCapacitySaveRequest request = new CustomerCapacitySaveRequest();
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(10);
		request.setCapacities(List.of(capacity));
		this.requestPostWithOk("/customer-capacity/save", request);
	}

	@Test
	@Order(2)
	void page() throws Exception {
		BasePageRequest request = new BasePageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/customer-capacity/page", request);
		Pager<List<CustomerCapacity>> result = getPageResult(mvcResult, CustomerCapacity.class);
		assert result.getList().size() == 1;
		request.setSort(Map.of("id", "desc"));
		this.requestPostWithOk("/customer-capacity/page", request);
	}

	@Test
	@Order(3)
	void update() throws Exception {
		CustomerCapacitySaveRequest request = new CustomerCapacitySaveRequest();
		CapacityRequest capacity = new CapacityRequest();
		capacity.setScopeIds(List.of("cc"));
		capacity.setCapacity(100);
		request.setCapacities(List.of(capacity));
		this.requestPostWithOk("/customer-capacity/save", request);
	}
}
