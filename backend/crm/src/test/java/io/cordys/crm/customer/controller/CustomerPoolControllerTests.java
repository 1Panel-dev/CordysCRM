package io.cordys.crm.customer.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.domain.CustomerPoolRecycleRule;
import io.cordys.crm.customer.domain.CustomerPoolRelation;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolSaveRequest;
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
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerPoolControllerTests extends BaseTest {

	private static CustomerPoolDTO testCustomerPool;
	@Resource
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<CustomerPoolRelation> customerPoolRelationMapper;

	@Test
	@Order(1)
	void emptyPage() throws Exception {
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/customer-pool/page", createPageRequest());
		Pager<List<CustomerPoolDTO>> result = getPageResult(mvcResult, CustomerPoolDTO.class);
		assert result.getList().isEmpty();
	}

	@Test
	@Order(2)
	void add() throws Exception {
		CustomerPool customerPool = createCustomerPool();
		CustomerPoolSaveRequest request = new CustomerPoolSaveRequest();
		BeanUtils.copyBean(request, customerPool);
		request.setPickRule(createPickRule());
		request.setRecycleRule(createRecycleRule());
		this.requestPostWithOk("/customer-pool/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		CustomerPoolPageRequest pageRequest = createPageRequest();
		pageRequest.setSort(Map.of("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/customer-pool/page", pageRequest);
		Pager<List<CustomerPoolDTO>> result = getPageResult(mvcResult, CustomerPoolDTO.class);
		assert result.getList().size() == 1;
		testCustomerPool = result.getList().getFirst();
	}

	@Test
	@Order(4)
	void update() throws Exception {
		CustomerPool customerPool = createCustomerPool();
		customerPool.setId(testCustomerPool.getId());
		CustomerPoolSaveRequest request = new CustomerPoolSaveRequest();
		BeanUtils.copyBean(request, customerPool);
		CustomerPoolPickRule pickRule = createPickRule();
		pickRule.setId("default-pick-rule");
		request.setPickRule(pickRule);
		CustomerPoolRecycleRule recycleRule = createRecycleRule();
		recycleRule.setId("default-recycle-rule");
		request.setRecycleRule(recycleRule);
		MvcResult mvcResult = this.requestPost("/customer-pool/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer_pool_access_fail"));
		// update owner id by sql
		customerPool.setOwnerId("admin");
		customerPoolMapper.updateById(customerPool);
		request.setOwnerId("admin");
		this.requestPostWithOk("/customer-pool/update", request);
	}

	@Test
	@Order(5)
	void switchStatus() throws Exception {
		MvcResult mvcResult = this.requestGet("/customer-pool/switch/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer_pool_not_exist"));
		this.requestGetWithOk("/customer-pool/switch/" + testCustomerPool.getId());
	}

	@Test
	@Order(6)
	void delete() throws Exception {
		MvcResult mvcResult = this.requestGet("/customer-pool/delete/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer_pool_not_exist"));
		// insert free customer on the pool, then delete it
		CustomerPoolRelation customerPoolRelation = createCustomerPoolRelation();
		customerPoolRelation.setPoolId(testCustomerPool.getId());
		customerPoolRelationMapper.insert(customerPoolRelation);
		MvcResult mvcResult1 = this.requestGet("/customer-pool/delete/" + testCustomerPool.getId()).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("customer_pool_related"));
		// pick customer, delete the pool
		customerPoolRelationMapper.deleteByPrimaryKey(customerPoolRelation.getId());
		this.requestGetWithOk("/customer-pool/delete/" + testCustomerPool.getId());
	}

	private CustomerPoolPageRequest createPageRequest() {
		CustomerPoolPageRequest request = new CustomerPoolPageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		return request;
	}

	private CustomerPool createCustomerPool() {
		CustomerPool customerPool = new CustomerPool();
		customerPool.setName("default-ct-pool");
		customerPool.setScopeId("default-dp");
		customerPool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		customerPool.setOwnerId("default-owner");
		customerPool.setEnable(true);
		return customerPool;
	}

	private CustomerPoolPickRule createPickRule() {
		CustomerPoolPickRule pickRule = new CustomerPoolPickRule();
		pickRule.setPickNumber(1);
		pickRule.setLimitOnNumber(true);
		pickRule.setLimitPreOwner(true);
		pickRule.setPickIntervalDays(1);
		return pickRule;
	}

	private CustomerPoolRecycleRule createRecycleRule() {
		CustomerPoolRecycleRule recycleRule = new CustomerPoolRecycleRule();
		recycleRule.setExpireNotice(true);
		recycleRule.setNoticeDays(10);
		return recycleRule;
	}

	private CustomerPoolRelation createCustomerPoolRelation() {
		CustomerPoolRelation customerPoolRelation = new CustomerPoolRelation();
		customerPoolRelation.setId("default-ct-pool-relation");
		customerPoolRelation.setCustomerId("default-custom");
		customerPoolRelation.setCreateTime(System.currentTimeMillis());
		customerPoolRelation.setCreateUser("default-user");
		customerPoolRelation.setUpdateTime(System.currentTimeMillis());
		customerPoolRelation.setUpdateUser("default-user");
		return customerPoolRelation;
	}
}
