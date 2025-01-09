package io.cordys.crm.lead.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.lead.dto.request.LeadCapacityPageRequest;
import io.cordys.crm.lead.dto.request.LeadCapacitySaveRequest;
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
public class LeadCapacityControllerTests extends BaseTest {

	private static LeadCapacity testData;

	@Test
	@Order(1)
	void add() throws Exception {
		LeadCapacitySaveRequest request = LeadCapacitySaveRequest.builder()
				.organizationId("default-org").scopeId("default-scope").capacity(100).build();
		this.requestPostWithOk("/lead-capacity/add", request);
	}

	@Test
	@Order(2)
	void page() throws Exception {
		LeadCapacityPageRequest request = new LeadCapacityPageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		request.setOrganizationId("default-org");
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/lead-capacity/page", request);
		Pager<List<LeadCapacity>> result = getPageResult(mvcResult, LeadCapacity.class);
		assert result.getList().size() == 1;
		testData = result.getList().getFirst();
		request.setSort(Map.of("id", "desc"));
		this.requestPostWithOk("/lead-capacity/page", request);
	}

	@Test
	@Order(3)
	void update() throws Exception {
		LeadCapacitySaveRequest request = LeadCapacitySaveRequest.builder()
				.organizationId("default-org").scopeId("default-scope,default-user").capacity(200).build();
		request.setId(testData.getId());
		this.requestPostWithOk("/lead-capacity/update", request);
		request.setId("not-exist");
		MvcResult mvcResult = this.requestPost("/lead-capacity/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("lead_capacity_not_exist"));
	}

	@Test
	@Order(4)
	void delete() throws Exception {
		this.requestGetWithOk("/lead-capacity/delete/" + testData.getId());
	}
}
