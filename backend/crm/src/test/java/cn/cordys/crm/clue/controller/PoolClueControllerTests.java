package cn.cordys.crm.clue.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ExportHeadDTO;
import cn.cordys.common.dto.ExportSelectRequest;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.ClueCapacity;
import cn.cordys.crm.clue.domain.ClueOwner;
import cn.cordys.crm.clue.domain.CluePoolPickRule;
import cn.cordys.crm.clue.dto.request.ClueExportRequest;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.request.PoolClueAssignRequest;
import cn.cordys.crm.clue.dto.request.PoolCluePickRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.system.domain.ExportTask;
import cn.cordys.crm.system.dto.request.PoolBatchAssignRequest;
import cn.cordys.crm.system.dto.request.PoolBatchPickRequest;
import cn.cordys.crm.system.service.ExportTaskCenterService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    protected static final String EXPORT_ALL = "/export-all";
    protected static final String EXPORT_SELECT = "/export-select";

	public static String testDataId;

	@Resource
	private BaseMapper<Clue> clueMapper;
	@Resource
	private BaseMapper<ClueOwner> clueOwnerMapper;
	@Resource
	private BaseMapper<ClueCapacity> clueCapacityMapper;
	@Resource
	private BaseMapper<CluePoolPickRule> cluePoolPickRuleMapper;
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


    // 因为要在删除之前测试能否导出，此时优先级和getDetail可以看作是平级的，所以都用同一个Order
    @Test
    @Order(6)
    void testExport() throws Exception {
        ClueExportRequest request = new ClueExportRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setFileName("export_test_clue_pool");
        ExportHeadDTO col = new ExportHeadDTO();
        col.setKey("name");
        col.setTitle("线索池名称");
        request.setHeadList(List.of(col));

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
        this.requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_EXPORT, EXPORT_ALL, request);
    }

    @Test
    @Order(6)
    void testExportSelect() throws Exception {

        CluePageRequest request = new CluePageRequest();
        request.setPoolId("test-pool-id");
        request.setCurrent(1);
        request.setPageSize(10);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(PAGE, request);
        Pager<List<ClueListResponse>> pageResult = getPageResult(mvcResult, ClueListResponse.class);
        assert pageResult.getTotal() == 1;
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_READ, PAGE, request);
        List<ClueListResponse> customerList = pageResult.getList();

        ExportSelectRequest exportRequest = new ExportSelectRequest();
        exportRequest.setFileName("export_test_clue_pool_select");
        exportRequest.setIds(List.of(customerList.getFirst().getId()));
        ExportHeadDTO col = new ExportHeadDTO();
        col.setKey("name");
        col.setTitle("线索名称");
        exportRequest.setHeadList(List.of(col));
        this.requestPostWithOk(EXPORT_SELECT, exportRequest);

        this.requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_POOL_EXPORT, EXPORT_SELECT, exportRequest);
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
		rule.setLimitNew(false);
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
