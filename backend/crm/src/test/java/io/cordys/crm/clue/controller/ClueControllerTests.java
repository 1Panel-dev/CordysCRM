package io.cordys.crm.clue.controller;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.dto.ResourceTabEnableDTO;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.clue.constants.ClueResultCode;
import io.cordys.crm.clue.constants.ClueStatus;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.ClueField;
import io.cordys.crm.clue.dto.request.*;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClueControllerTests extends BaseTest {
    private static final String BASE_PATH = "/clue/";
    protected static final String MODULE_FORM = "module/form";
    protected static final String STATUS_UPDATE = "status/update";
    protected static final String BATCH_TRANSFER = "batch/transfer";
    protected static final String TAB = "tab";
    protected static final String BATCH_TO_POOL = "batch/to-pool";
    protected static final String TRANSITION_CUSTOMER = "transition/customer";
    protected static final String EXPORT = "export";
    protected static final String EXPORT_SELECT = "export-select";

    private static Clue addClue;
    private static Clue anotherClue;
    private static final List<String> batchIds = new ArrayList<>();

    @Resource
    private BaseMapper<Clue> clueMapper;

    @Resource
    private BaseMapper<ClueField> clueFieldMapper;

    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

     @Test
     @Order(0)
     void testModuleField() throws Exception {
         this.requestGetWithOkAndReturn(MODULE_FORM);

         // 校验权限
         requestGetPermissionsTest(List.of(PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ), MODULE_FORM);
     }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CluePageRequest request = new CluePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
//        ModuleField moduleField = moduleFieldMapper.select(example)
//                .stream()
//                .filter(field -> StringUtils.equals(field.getInternalKey(), "clueSource"))
//                .findFirst().orElse(null);

        // 请求成功
        ClueAddRequest request = new ClueAddRequest();
        request.setName("aa");
        request.setOwner("bb");
        request.setContact("test111");
        request.setPhone("18750920048");
        request.setProducts(List.of("cc"));
//        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), "1")));

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Clue resultData = getResultData(mvcResult, Clue.class);
        Clue clue = clueMapper.selectByPrimaryKey(resultData.getId());
        Assertions.assertEquals(request.getName(), clue.getName());
        Assertions.assertEquals(request.getOwner(), clue.getOwner());

        // 校验字段
//        List<BaseModuleFieldValue> fieldValues = getClueFields(clue.getId())
//                .stream().map(clueField -> {
//                    BaseModuleFieldValue baseModuleFieldValue = BeanUtils.copyBean(new BaseModuleFieldValue(), clueField);
//                    baseModuleFieldValue.setFieldValue(clueField.getFieldValue());
//                    return baseModuleFieldValue;
//                })
//                .toList();
//        Assertions.assertEquals(request.getModuleFields(), fieldValues);


        // 创建另一个客户
        request.setName("another");
        request.setOwner(InternalUser.ADMIN.getValue());
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Clue.class);
        anotherClue = clueMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        addClue = clue;

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_ADD, DEFAULT_ADD, request);
    }

    private ModuleForm getModuleForm() {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setFormKey(FormKey.CLUE.getKey());
        return moduleFormMapper.selectOne(example);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        ClueUpdateRequest request = new ClueUpdateRequest();
        request.setId(addClue.getId());
        request.setName("aa11");
        request.setOwner(InternalUser.ADMIN.getValue());
        request.setProducts(List.of("cc", "dd"));
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Clue clueResult = clueMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), clueResult.getName());
        Assertions.assertEquals(request.getOwner(), clueResult.getOwner());

        // 不修改信息
        ClueUpdateRequest emptyRequest = new ClueUpdateRequest();
        emptyRequest.setId(addClue.getId());
        emptyRequest.setProducts(List.of("cc", "dd"));
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(2)
    void testUpdateStatus() throws Exception {
        // 请求成功
        ClueStatusUpdateRequest request = new ClueStatusUpdateRequest();
        request.setId(addClue.getId());
        request.setStage(ClueStatus.INTERESTED.name());
        this.requestPostWithOk(STATUS_UPDATE, request);
        // 校验请求成功数据
        Clue clueResult = clueMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getStage(), clueResult.getStage());
        Assertions.assertEquals(addClue.getStage(), clueResult.getLastStage());
        addClue.setStage(request.getStage());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_UPDATE, STATUS_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addClue.getId());
        ClueGetResponse getResponse = getResultData(mvcResult, ClueGetResponse.class);

        // 校验请求成功数据
        Assertions.assertNotNull(getResponse.getOwnerName());
        if (!getResponse.getOwner().equals(InternalUser.ADMIN.getValue())) {
            Assertions.assertNotNull(getResponse.getDepartmentId());
            Assertions.assertNotNull(getResponse.getDepartmentName());
        }
        Assertions.assertNotNull(getResponse.getOptionMap().get("owner"));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, DEFAULT_GET, addClue.getId());
    }


    @Test
    @Order(3)
    void testPage() throws Exception {
        CluePageRequest request = new CluePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        request.setSearchType(BusinessSearchType.ALL.name());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ClueListResponse>> pageResult = getPageResult(mvcResult, ClueListResponse.class);
        List<ClueListResponse> clueList = pageResult.getList();

        Clue example = new Clue();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setInSharedPool(false);
        Map<String, Clue> clueMap = clueMapper.select(example)
                .stream()
                .collect(Collectors.toMap(Clue::getId, Function.identity()));

        clueList.forEach(clueListResponse -> {
            Clue clue = clueMap.get(clueListResponse.getId());
            Clue responseClue = BeanUtils.copyBean(new Clue(), clueListResponse);
            responseClue.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            responseClue.setInSharedPool(false);
            Assertions.assertEquals(clue, responseClue);
            Assertions.assertNotNull(clueListResponse.getOwnerName());
            if (!responseClue.getOwner().equals(InternalUser.ADMIN.getValue())) {
                Assertions.assertNotNull(clueListResponse.getDepartmentId());
                Assertions.assertNotNull(clueListResponse.getDepartmentName());
            }
        });

        request.setSearchType(BusinessSearchType.SELF.name());
        this.requestPostWithOk(DEFAULT_PAGE, request);

        request.setSearchType(BusinessSearchType.DEPARTMENT.name());
        this.requestPostWithOk(DEFAULT_PAGE, request);


        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(4)
    void testTransfer() throws Exception {
        ClueBatchTransferRequest request = new ClueBatchTransferRequest();
        request.setIds(List.of(addClue.getId()));
        request.setOwner(PERMISSION_USER_NAME);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(BATCH_TRANSFER, request);

        getResultData(mvcResult, ClueGetResponse.class);

        // 校验请求成功数据
        Clue clue = clueMapper.selectByPrimaryKey(addClue.getId());
        Assertions.assertEquals(PERMISSION_USER_NAME, clue.getOwner());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_UPDATE, BATCH_TRANSFER, request);
    }

    @Test
    @Order(4)
    void testTab() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(TAB);
        ResourceTabEnableDTO resultData = getResultData(mvcResult, ResourceTabEnableDTO.class);
        // 校验请求成功数据
        Assertions.assertTrue(resultData.getAll());
        Assertions.assertTrue(resultData.getDept());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, TAB);
    }
    @Test
    @Order(5)
    void transitionCustomer() throws Exception {
        ClueTransitionCustomerRequest request = new ClueTransitionCustomerRequest();
        request.setClueId(addClue.getId());
        request.setName("cc");
        request.setOwner("admin");
        this.requestPostWithOk(TRANSITION_CUSTOMER, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ADD, TRANSITION_CUSTOMER, request);
    }

    @Test
    @Order(10)
    void testDelete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addClue.getId());
        Assertions.assertNull(clueMapper.selectByPrimaryKey(addClue.getId()));

        List<ClueField> fields = getClueFields(addClue.getId());
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_DELETE, DEFAULT_DELETE, addClue.getId());
    }

    @Test
    @Order(11)
    void testBatchDelete() throws Exception {
        this.requestPostWithOk(DEFAULT_BATCH_DELETE, List.of(anotherClue.getId()));
        Assertions.assertNull(clueMapper.selectByPrimaryKey(anotherClue.getId()));

        List<ClueField> fields = getClueFields(anotherClue.getId());
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_DELETE, DEFAULT_BATCH_DELETE, List.of(anotherClue.getId()));
    }

    @Test
    @Order(12)
    @Sql(scripts = {"/dml/init_clue_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/dml/cleanup_clue_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageReservedDay() throws Exception {
        ClueAddRequest addRequest = new ClueAddRequest();
        addRequest.setName("aa");
        addRequest.setOwner("admin");
        addRequest.setContact("test");
        addRequest.setPhone("11111111111");
        addRequest.setProducts(List.of("cc"));
        this.requestPostWithOk(DEFAULT_ADD, addRequest);
        CluePageRequest request = new CluePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setSearchType(BusinessSearchType.ALL.name());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ClueListResponse>> pageResult = getPageResult(mvcResult, ClueListResponse.class);
        List<ClueListResponse> clueList = pageResult.getList();
        clueList.forEach(clue -> {
            batchIds.add(clue.getId());
        });
        this.requestPostWithOk(BATCH_TO_POOL, batchIds);
    }

    @Test
    @Order(13)
    void testBatchToPool() throws Exception {
        this.requestPostWithOk(BATCH_TO_POOL, batchIds);
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_RECYCLE, BATCH_TO_POOL, batchIds);
    }

    @Test
    @Order(14)
    void testExport() throws Exception {
        ClueExportRequest request = new ClueExportRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setFileName("export_test");
        ExportHeadDTO col = new ExportHeadDTO();
        col.setKey("name");
        col.setTitle("线索名称");
        request.setHeadList(List.of(col));
        this.requestPostWithOk(EXPORT, request);
    }

    @Test
    @Order(15)
    void testExportSelect() throws Exception {
        ExportSelectRequest request = new ExportSelectRequest();
        request.setFileName("export_select_test");
        request.setIds(List.of("test_id"));
        ExportHeadDTO col = new ExportHeadDTO();
        col.setKey("name");
        col.setTitle("线索名称");
        request.setHeadList(List.of(col));
        this.requestPostWithOk(EXPORT_SELECT, request);
    }

    private List<ClueField> getClueFields(String clueId) {
        ClueField example = new ClueField();
        example.setResourceId(clueId);
        return clueFieldMapper.select(example);
    }
}