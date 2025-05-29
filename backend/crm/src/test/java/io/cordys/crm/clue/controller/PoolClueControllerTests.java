package io.cordys.crm.clue.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.ClueCapacity;
import io.cordys.crm.clue.domain.ClueOwner;
import io.cordys.crm.clue.domain.CluePoolPickRule;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.request.PoolClueAssignRequest;
import io.cordys.crm.clue.dto.request.PoolCluePickRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import io.cordys.crm.system.dto.request.PoolBatchPickRequest;
import io.cordys.crm.system.dto.request.PoolBatchRequest;
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
public class PoolClueControllerTests extends BaseTest {

	public static final String BASE_PATH = "/pool/clue";
	public static final String GET_OPTIONS = "/options";
	public static final String PAGE = "/page";
	public static final String PICK = "/pick";
	public static final String ASSIGN = "/assign";
	public static final String DELETE = "/delete/";
	public static final String GET_DETAIL = "/get/";
	public static final String BATCH_PICK = "/batch-pick";
	public static final String BATCH_ASSIGN = "/batch-assign";
	public static final String BATCH_DELETE = "/batch-delete";

	public static String testDataId;

	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private BaseMapper<ClueOwner> clueOwnerMapper;
	@Resource
	private BaseMapper<ClueCapacity> clueCapacityMapper;
	@Resource
	private BaseMapper<CluePoolPickRule> cluePoolPickRuleMapper;

	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void prepareTestData() {
		Clue clue = createClue();
		Clue ownClue = createClue();
		ClueCapacity capacity = createCapacity();
		ownClue.setInSharedPool(false);
		ownClue.setOwner("admin");
		clueMapper.batchInsert(List.of(ownClue, clue));
		clueCapacityMapper.insert(capacity);
	}

	@Test
	@Order(2)
	void getOptions() throws Exception {
		this.requestGetWithOk(GET_OPTIONS);
		requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_READ, GET_OPTIONS);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		CluePageRequest request = new CluePageRequest();
		request.setPoolId("test-pool-id");
		request.setCurrent(1);
		request.setPageSize(10);
		MvcResult mvcResult = this.requestPostWithOkAndReturn(PAGE, request);
		Pager<List<ClueListResponse>> pageResult = getPageResult(mvcResult, ClueListResponse.class);
		assert pageResult.getTotal() == 1;
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_READ, PAGE, request);
	}

	@Test
	@Order(4)
	void pickFailWithOverCapacity() throws Exception {
		PoolCluePickRequest request = new PoolCluePickRequest();
		request.setClueId(testDataId);
		request.setPoolId("test-pool-id");
		MvcResult mvcResult = this.requestPost(PICK, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.getWithArgs("customer.capacity.over", 0));
		clueCapacityMapper.deleteByLambda(new LambdaQueryWrapper<>());
		CluePoolPickRule pickRule = createPickRule();
		pickRule.setLimitOnNumber(false);
		pickRule.setPoolId("test-pool-id");
		cluePoolPickRuleMapper.insert(pickRule);
		this.requestPost(PICK, request);
		cluePoolPickRuleMapper.deleteByLambda(new LambdaQueryWrapper<>());
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_PICK, PICK, request);
	}

	@Test
	@Order(5)
	void assignSuccess() throws Exception {
		PoolClueAssignRequest request = new PoolClueAssignRequest();
		request.setClueId(testDataId);
		request.setAssignUserId("aa");
		this.requestPostWithOk(ASSIGN, request);
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_ASSIGN, ASSIGN, request);
	}

	@Test
	@Order(6)
	void getDetail() throws Exception {
		this.requestGetWithOk(GET_DETAIL + testDataId);
		requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_READ, GET_DETAIL + testDataId);
	}

	@Test
	@Order(7)
	void deleteSuccess() throws Exception {
		this.requestGetWithOk(DELETE + testDataId);
		requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_DELETE, DELETE + testDataId);
	}

	@Test
	@Order(8)
	void batchPickFailWithOverDailyOrPreOwnerLimit() throws Exception {
		Clue clue = createClue();
		clue.setOwner("admin");
		clue.setInSharedPool(false);
		clueMapper.insert(clue);
		CluePoolPickRule rule = createPickRule();
		rule.setLimitOnNumber(true);
		rule.setPickNumber(1);
		cluePoolPickRuleMapper.insert(rule);
		clueCapacityMapper.deleteByLambda(new LambdaQueryWrapper<>());
		PoolBatchPickRequest request = new PoolBatchPickRequest();
		request.setBatchIds(List.of(testDataId));
		request.setPoolId("test-pool-id");
		this.requestPost(BATCH_PICK, request);
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_PICK, BATCH_PICK, request);
	}

	@Test
	@Order(9)
	void batchAssignFailWithNotExit() throws Exception {
		PoolBatchAssignRequest request = new PoolBatchAssignRequest();
		request.setBatchIds(List.of("aaa"));
		request.setAssignUserId("cc");
		MvcResult mvcResult = this.requestPost(BATCH_ASSIGN, request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("clue.not.exist"));
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_ASSIGN, BATCH_ASSIGN, request);
	}

	@Test
	@Order(10)
	void batchDeleteSuccess() throws Exception {
		this.requestPostWithOk(BATCH_DELETE, List.of(testDataId));
		requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_DELETE, BATCH_DELETE, List.of(testDataId));
	}

	private Clue createClue() {
		Clue clue = new Clue();
		clue.setId(IDGenerator.nextStr());
		testDataId = clue.getId();
		clue.setStage("test");
		clue.setName("ct");
		clue.setOwner("cc");
		clue.setProducts(List.of("cc"));
		clue.setCollectionTime(System.currentTimeMillis());
		clue.setPoolId("test-pool-id");
		clue.setInSharedPool(true);
		clue.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		clue.setCreateTime(System.currentTimeMillis());
		clue.setCreateUser("admin");
		clue.setUpdateTime(System.currentTimeMillis());
		clue.setUpdateUser("admin");
		return clue;
	}

	private ClueCapacity createCapacity() {
		ClueCapacity capacity = new ClueCapacity();
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

	private CluePoolPickRule createPickRule() {
		CluePoolPickRule rule = new CluePoolPickRule();
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
		ClueOwner clueOwner = new ClueOwner();
		clueOwner.setId(IDGenerator.nextStr());
		clueOwner.setClueId(testDataId);
		clueOwner.setOwner("admin");
		clueOwner.setCollectionTime(System.currentTimeMillis());
		clueOwner.setOperator("admin");
		clueOwner.setEndTime(System.currentTimeMillis());
		clueOwnerMapper.insert(clueOwner);
	}
}
