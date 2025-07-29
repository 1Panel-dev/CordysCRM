package io.cordys.crm.customer.controller;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.SortRequest;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerPool;
import io.cordys.crm.customer.dto.CustomerPoolDTO;
import io.cordys.crm.customer.dto.CustomerPoolPickRuleDTO;
import io.cordys.crm.customer.dto.CustomerPoolRecycleRuleDTO;
import io.cordys.crm.customer.dto.request.CustomerPoolAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPoolUpdateRequest;
import io.cordys.crm.system.dto.RuleConditionDTO;
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
import java.util.Set;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerPoolControllerTests extends BaseTest {

	private static CustomerPoolDTO testCustomerPool;
	@Resource
	private BaseMapper<CustomerPool> customerPoolMapper;
	@Resource
	private BaseMapper<Customer> customerMapper;

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
		CustomerPoolAddRequest request = new CustomerPoolAddRequest();
		BeanUtils.copyBean(request, customerPool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		CustomerPoolPickRuleDTO pickRule = CustomerPoolPickRuleDTO.builder()
				.limitOnNumber(true).limitPreOwner(true).limitNew(false).pickNumber(1).pickIntervalDays(1).build();
		request.setPickRule(pickRule);
		RuleConditionDTO condition = new RuleConditionDTO();
		condition.setColumn("name");
		condition.setOperator("=");
		condition.setValue("cc");
		CustomerPoolRecycleRuleDTO recycleRule = CustomerPoolRecycleRuleDTO.builder()
				.conditions(List.of(condition)).build();
		request.setRecycleRule(recycleRule);
		request.setHiddenFieldIds(Set.of(BusinessModuleField.CUSTOMER_NAME.getKey()));
		this.requestPostWithOk("/customer-pool/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		BasePageRequest request = createPageRequest();
		request.setSort(new SortRequest("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/customer-pool/page", request);
		Pager<List<CustomerPoolDTO>> result = getPageResult(mvcResult, CustomerPoolDTO.class);
		assert result.getList().size() == 1;
		testCustomerPool = result.getList().getFirst();
	}

	@Test
	@Order(4)
	void update() throws Exception {
		CustomerPool customerPool = createCustomerPool();
		customerPool.setId(testCustomerPool.getId());
		CustomerPoolUpdateRequest request = new CustomerPoolUpdateRequest();
		BeanUtils.copyBean(request, customerPool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		CustomerPoolPickRuleDTO pickRule = CustomerPoolPickRuleDTO.builder()
				.limitOnNumber(true).limitPreOwner(true).pickNumber(1).pickIntervalDays(1).build();
		request.setPickRule(pickRule);
		CustomerPoolRecycleRuleDTO recycleRule = CustomerPoolRecycleRuleDTO.builder()
				.build();
		request.setRecycleRule(recycleRule);
		request.setHiddenFieldIds(Set.of(BusinessModuleField.CUSTOMER_OWNER.getKey()));
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
		Customer freeCustomer = createFreeCustomer();
		freeCustomer.setPoolId(testCustomerPool.getId());
		customerMapper.insert(freeCustomer);
		this.requestGet("/customer-pool/no-pick/" + testCustomerPool.getId());
		// pick customer, delete the pool
		customerMapper.deleteByPrimaryKey(freeCustomer.getId());
		this.requestGetWithOk("/customer-pool/delete/" + testCustomerPool.getId());
	}

	private BasePageRequest createPageRequest() {
		BasePageRequest request = new BasePageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		return request;
	}

	private CustomerPool createCustomerPool() {
		CustomerPool customerPool = new CustomerPool();
		customerPool.setName("default-ct-pool");
		customerPool.setScopeId(JSON.toJSONString(List.of("default-dp")));
		customerPool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		customerPool.setEnable(true);
		customerPool.setAuto(true);
		return customerPool;
	}

	private Customer createFreeCustomer() {
		Customer customer = new Customer();
		customer.setId("default-ct-id");
		customer.setName("default-ct");
		customer.setInSharedPool(true);
		customer.setOwner("admin");
		customer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		customer.setCollectionTime(System.currentTimeMillis());
		customer.setCreateTime(System.currentTimeMillis());
		customer.setCreateUser("default-user");
		customer.setUpdateTime(System.currentTimeMillis());
		customer.setUpdateUser("default-user");
		return customer;
	}
}
