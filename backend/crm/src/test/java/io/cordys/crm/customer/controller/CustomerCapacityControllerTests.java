package io.cordys.crm.customer.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.dto.request.CustomerCapacityPageRequest;
import io.cordys.crm.customer.dto.request.CustomerCapacitySaveRequest;
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

	private static CustomerCapacity testData;

	@Test
	@Order(1)
	void add() throws Exception {
		CustomerCapacitySaveRequest request = CustomerCapacitySaveRequest.builder().scopeId("default-scope").capacity(100).build();
		this.requestPostWithOk("/customer-capacity/add", request);
	}

	@Test
	@Order(2)
	void page() throws Exception {
		CustomerCapacityPageRequest request = new CustomerCapacityPageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/customer-capacity/page", request);
		Pager<List<CustomerCapacity>> result = getPageResult(mvcResult, CustomerCapacity.class);
		assert result.getList().size() == 1;
		testData = result.getList().getFirst();
		request.setSort(Map.of("id", "desc"));
		this.requestPostWithOk("/customer-capacity/page", request);
	}

	@Test
	@Order(3)
	void update() throws Exception {
		CustomerCapacitySaveRequest request = CustomerCapacitySaveRequest.builder().scopeId("default-scope,default-user").capacity(200).build();
		request.setId(testData.getId());
		this.requestPostWithOk("/customer-capacity/update", request);
		request.setId("not-exist");
		MvcResult mvcResult = this.requestPost("/customer-capacity/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer_capacity_not_exist"));
	}

	@Test
	@Order(4)
	void delete() throws Exception {
		this.requestGetWithOk("/customer-capacity/delete/" + testData.getId());
	}
}
