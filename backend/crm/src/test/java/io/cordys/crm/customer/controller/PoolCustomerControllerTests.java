package io.cordys.crm.customer.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.uid.IDGenerator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PoolCustomerControllerTests extends BaseTest {

	public static final String BASE_PATH = "/pool/customer";
	public static final String GET_OPTIONS = "/options";
	public static final String PAGE = "/page";
	public static final String DELETE = "/delete/";
	public static final String ASSIGN = "/assign";
	public static final String PICK = "/pick/";

	public static String testDataId;

	@Resource
	private BaseMapper<Customer> customerMapper;

	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void prepareData() {
		Customer customer = createCustomer();
		customerMapper.insert(customer);
	}

	@Test
	@Order(2)
	void getOptions() throws Exception {
		this.requestGetWithOk(GET_OPTIONS);
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
	}

	@Test
	@Order(4)
	void pick() throws Exception {
		this.requestGetWithOk(PICK + testDataId);
	}

	@Test
	@Order(5)
	void assign() throws Exception {
		PoolCustomerAssignRequest request = new PoolCustomerAssignRequest();
		request.setCustomerId(testDataId);
		request.setAssignUserId("aa");
		this.requestPostWithOk(ASSIGN, request);
	}

	@Test
	@Order(6)
	void delete() throws Exception {
		this.requestGetWithOk(DELETE + testDataId);
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
}
