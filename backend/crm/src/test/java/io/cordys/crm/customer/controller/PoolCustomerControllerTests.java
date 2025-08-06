package io.cordys.crm.customer.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.pager.Pager;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerCapacity;
import io.cordys.crm.customer.domain.CustomerOwner;
import io.cordys.crm.customer.domain.CustomerPoolPickRule;
import io.cordys.crm.customer.dto.request.CustomerExportRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerAssignRequest;
import io.cordys.crm.customer.dto.request.PoolCustomerPickRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.system.domain.ExportTask;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.crm.system.dto.request.PoolBatchRequest;
import io.cordys.crm.system.service.ExportTaskCenterService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
	public static final String GET_DETAIL = "/get/";
	public static final String BATCH_PICK = "/batch-pick";
	public static final String BATCH_ASSIGN = "/batch-assign";
	public static final String BATCH_DELETE = "/batch-delete";
	protected static final String EXPORT_ALL = "/export-all";
	protected static final String EXPORT_SELECT = "/export-select";
	public static String testDataId;

	@Resource
	private BaseMapper<Customer> customerMapper;
	@Resource
	private BaseMapper<CustomerOwner> customerOwnerMapper;
	@Resource
	private BaseMapper<CustomerCapacity> customerCapacityMapper;
	@Resource
	private BaseMapper<CustomerPoolPickRule> customerPoolPickRuleMapper;
	@Resource
	private BaseMapper<ExportTask> exportTaskBaseMapper;
	@Resource
	private ExportTaskCenterService exportTaskCenterService;

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
		requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ, GET_OPTIONS);
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
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ, PAGE, request);
	}

	@Test
	@Order(4)
	void pickFailWithOverCapacity() throws Exception {
		PoolCustomerPickRequest request = new PoolCustomerPickRequest();
		request.setCustomerId(testDataId);
		request.setPoolId("test-pool-id");
		MvcResult mvcResult = this.requestPost(PICK, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.getWithArgs("customer.capacity.over", 0));
		customerCapacityMapper.deleteByLambda(new LambdaQueryWrapper<>());
		CustomerPoolPickRule pickRule = createPickRule();
		pickRule.setLimitOnNumber(false);
		pickRule.setPoolId("test-pool-id");
		customerPoolPickRuleMapper.insert(pickRule);
		this.requestPost(PICK, request);
		customerPoolPickRuleMapper.deleteByLambda(new LambdaQueryWrapper<>());
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK, PICK, request);
	}

	@Test
	@Order(5)
	void assignSuccess() throws Exception {
		PoolCustomerAssignRequest request = new PoolCustomerAssignRequest();
		request.setCustomerId(testDataId);
		request.setAssignUserId("aa");
		this.requestPostWithOk(ASSIGN, request);
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN, ASSIGN, request);
	}

	@Test
	@Order(6)
	void getDetail() throws Exception {
		this.requestGetWithOk(GET_DETAIL + testDataId);
		requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ, GET_DETAIL + testDataId);
	}


	@Test
	@Order(6)
	void testExport() throws Exception {
		CustomerExportRequest request = new CustomerExportRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		request.setPoolId("test-pool-id");
		request.setFileName("测试跨页导出公海");

		List<ExportHeadDTO> list = new ArrayList<>();
		list.add(new ExportHeadDTO("name", "客户名称"));

		request.setHeadList(list);
		MvcResult mvcResult = this.requestPostWithOkAndReturn(EXPORT_ALL, request);
		String resultData = getResultData(mvcResult, String.class);
		Thread.sleep(1500); // 等待导出任务完成
		ResponseEntity<org.springframework.core.io.Resource> resourceResponseEntity = exportTaskCenterService.download(resultData);
		Assertions.assertTrue(resourceResponseEntity.getBody().exists());
		ExportTask exportTask = new ExportTask();
		exportTask.setId(resultData);
		LocalDateTime oneDayBefore = LocalDateTime.now().minusDays(2);
		exportTask.setCreateTime(oneDayBefore.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
		exportTaskBaseMapper.updateById(exportTask);
		exportTaskCenterService.clean();
		System.out.println(resourceResponseEntity.getBody().exists());

		//权限校验
		this.requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT, EXPORT_ALL, request);
	}

	@Test
	@Order(5)
	void testExportSelect() throws Exception {
		CustomerPageRequest pageRequest = new CustomerPageRequest();
		pageRequest.setCurrent(1);
		pageRequest.setPageSize(10);
		pageRequest.setPoolId("test-pool-id");

		MvcResult mvcResult = this.requestPostWithOkAndReturn(PAGE, pageRequest);
		Pager<List<CustomerListResponse>> pageResult = getPageResult(mvcResult, CustomerListResponse.class);
		List<CustomerListResponse> customerList = pageResult.getList();

		ExportSelectRequest exportRequest = new ExportSelectRequest();
		exportRequest.setFileName("测试导出公海");

		ExportHeadDTO exportHeadDTO = new ExportHeadDTO();
		exportHeadDTO.setKey("name");
		exportHeadDTO.setTitle("客户选择名称");
		List<ExportHeadDTO> list = new ArrayList<>();
		list.add(exportHeadDTO);
		exportRequest.setHeadList(list);
		List<String> ids = customerList.stream().map(CustomerListResponse::getId).collect(Collectors.toList());
		exportRequest.setIds(ids);

		this.requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_EXPORT, EXPORT_SELECT, exportRequest);
	}

	@Test
	@Order(7)
	void deleteSuccess() throws Exception {
		this.requestGetWithOk(DELETE + testDataId);
		requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE, DELETE + testDataId);
	}

	@Test
	@Order(8)
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
		PoolBatchPickRequest request = new PoolBatchPickRequest();
		request.setBatchIds(List.of(testDataId));
		request.setPoolId("test-pool-id");
		this.requestPost(BATCH_PICK, request);
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_PICK, BATCH_PICK, request);
	}

	@Test
	@Order(9)
	void batchAssignFailWithNotExit() throws Exception {
		PoolBatchAssignRequest request = new PoolBatchAssignRequest();
		request.setBatchIds(List.of("aaa"));
		request.setAssignUserId("cc");
		MvcResult mvcResult = this.requestPost(BATCH_ASSIGN, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("customer.not.exist"));
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_ASSIGN, BATCH_ASSIGN, request);
	}

	@Test
	@Order(10)
	void batchDeleteSuccess() throws Exception {
		PoolBatchRequest request = new PoolBatchRequest();
		request.setBatchIds(List.of(testDataId));
		this.requestPostWithOk(BATCH_DELETE, request);
		requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_POOL_DELETE, BATCH_DELETE, request);
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
		rule.setLimitNew(false);
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
