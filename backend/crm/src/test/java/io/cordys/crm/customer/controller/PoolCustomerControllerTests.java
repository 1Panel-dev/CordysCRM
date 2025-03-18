package io.cordys.crm.customer.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.domain.CustomerOwner;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
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
public class PoolCustomerControllerTests extends BaseTest {

	public static final String BASE_PATH = "/pool/customer";
	public static final String GET_OPTIONS = "/options";
	public static final String PAGE = "/page";
	public static final String PICK = "/pick";
	public static final String ASSIGN = "/assign";
	public static final String DELETE = "/delete/";
	public static final String BATCH_PICK = "/batch-pick";
	public static final String BATCH_ASSIGN = "/batch-assign";
	public static final String BATCH_DELETE = "/batch-delete";

	public static String testDataId;

	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private BaseMapper<CustomerOwner> customerOwnerMapper;
	@Resource
	private BaseMapper<CustomerCapacity> customerCapacityMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> customerPoolPickRuleMapper;

	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void prepareTestData() {
		Customer customer = createCustomer();
		Customer ownCustomer = createCustomer();
		CustomerCapacity capacity = createCapacity();
		ownCustomer.setInSharedPool(false);
		ownCustomer.setOwner("admin");
		customerMapper.batchInsert(List.of(ownCustomer, customer));
		customerCapacityMapper.insert(capacity);
	}

	@Test
	@Order(2)
	void getOptions() throws Exception {
		this.requestGetWithOk(GET_OPTIONS);
		requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, GET_OPTIONS);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		CustomerPageRequest request = new CustomerPageRequest();
		request.setPoolId("test-pool-id");
		request.setCurrent(1);
		request.setPageSize(10);
		MvcResult mvcResult = this.requestPostWithOkAndReturn(PAGE, request);
		Pager<List<CustomerListResponse>> pageResult = getPageResult(mvcResult, CustomerListResponse.class);
		assert pageResult.getTotal() == 1;
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, PAGE, request);
	}

	@Test
	@Order(4)
	void pickFailWithOverCapacity() throws Exception {
		PoolCustomerPickRequest request = new PoolCustomerPickRequest();
		request.setCustomerId(testDataId);
		request.setPoolId("test-pool-id");
		MvcResult mvcResult = this.requestPost(PICK, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer.capacity.over"));
		customerCapacityMapper.deleteByLambda(new LambdaQueryWrapper<>());
		CustomerPoolPickRule pickRule = createPickRule();
		pickRule.setLimitOnNumber(false);
		pickRule.setPoolId("test-pool-id");
		customerPoolPickRuleMapper.insert(pickRule);
		this.requestPostWithOk(PICK, request);
		customerPoolPickRuleMapper.deleteByLambda(new LambdaQueryWrapper<>());
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_PICK, PICK, request);
	}

	@Test
	@Order(5)
	void assignSuccess() throws Exception {
		PoolCustomerAssignRequest request = new PoolCustomerAssignRequest();
		request.setCustomerId(testDataId);
		request.setAssignUserId("aa");
		this.requestPostWithOk(ASSIGN, request);
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ASSIGN, ASSIGN, request);
	}

	@Test
	@Order(6)
	void deleteSuccess() throws Exception {
		this.requestGetWithOk(DELETE + testDataId);
		requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DELETE + testDataId);
	}

	@Test
	@Order(7)
	void batchPickFailWithOverDailyOrPreOwnerLimit() throws Exception {
		Customer customer = createCustomer();
		customer.setOwner("admin");
		customer.setInSharedPool(false);
		customerMapper.insert(customer);
		CustomerPoolPickRule rule = createPickRule();
		rule.setLimitOnNumber(true);
		rule.setPickNumber(1);
		customerPoolPickRuleMapper.insert(rule);
		customerCapacityMapper.deleteByLambda(new LambdaQueryWrapper<>());
		PoolCustomerBatchPickRequest request = new PoolCustomerBatchPickRequest();
		request.setBatchIds(List.of(testDataId));
		request.setPoolId("test-pool-id");
		MvcResult mvcResult = this.requestPost(BATCH_PICK, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer.daily.pick.over"));
		rule.setLimitOnNumber(false);
		rule.setLimitPreOwner(true);
		rule.setPickIntervalDays(1);
		customerPoolPickRuleMapper.updateById(rule);
		insertOwnerHis();
		MvcResult mvcResult1 = this.requestPost(BATCH_PICK, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("customer.pre_owner.pick.limit"));
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_PICK, BATCH_PICK, request);
	}

	@Test
	@Order(8)
	void batchAssignFailWithNotExit() throws Exception {
		PoolCustomerBatchAssignRequest request = new PoolCustomerBatchAssignRequest();
		request.setBatchIds(List.of("aaa"));
		request.setAssignUserId("cc");
		MvcResult mvcResult = this.requestPost(BATCH_ASSIGN, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer.not.exist"));
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ASSIGN, BATCH_ASSIGN, request);
	}

	@Test
	@Order(9)
	void batchDeleteSuccess() throws Exception {
		PoolCustomerBatchRequest request = new PoolCustomerBatchRequest();
		request.setBatchIds(List.of(testDataId));
		this.requestPostWithOk(BATCH_DELETE, request);
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, BATCH_DELETE, request);
	}

	private Customer createCustomer() {
		Customer customer = new Customer();
		customer.setId(IDGenerator.nextStr());
		testDataId = customer.getId();
		customer.setName("ct");
		customer.setOwner("cc");
		customer.setCollectionTime(System.currentTimeMillis());
		customer.setPoolId("test-pool-id");
		customer.setInSharedPool(true);
		customer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		customer.setCreateTime(System.currentTimeMillis());
		customer.setCreateUser("admin");
		customer.setUpdateTime(System.currentTimeMillis());
		customer.setUpdateUser("admin");
		return customer;
	}

	private CustomerCapacity createCapacity() {
		CustomerCapacity capacity = new CustomerCapacity();
		capacity.setId(IDGenerator.nextStr());
		capacity.setScopeId("admin");
		capacity.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		capacity.setCapacity(1);
		capacity.setCreateTime(System.currentTimeMillis());
		capacity.setCreateUser("admin");
		capacity.setUpdateTime(System.currentTimeMillis());
		capacity.setUpdateUser("admin");
		return capacity;
	}

	private CustomerPoolPickRule createPickRule() {
		CustomerPoolPickRule rule = new CustomerPoolPickRule();
		rule.setId(IDGenerator.nextStr());
		rule.setPoolId("test-pool-id");
		rule.setLimitOnNumber(false);
		rule.setLimitPreOwner(false);
		rule.setCreateTime(System.currentTimeMillis());
		rule.setCreateUser("admin");
		rule.setUpdateTime(System.currentTimeMillis());
		rule.setUpdateUser("admin");
		return rule;
	}

	private void insertOwnerHis() {
		CustomerOwner customerOwner = new CustomerOwner();
		customerOwner.setId(IDGenerator.nextStr());
		customerOwner.setCustomerId(testDataId);
		customerOwner.setOwner("admin");
		customerOwner.setCollectionTime(System.currentTimeMillis());
		customerOwner.setOperator("admin");
		customerOwner.setEndTime(System.currentTimeMillis());
		customerOwnerMapper.insert(customerOwner);
	}
}
