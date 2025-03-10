package io.cordys.crm.clue.controller;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.clue.constants.ClueResultCode;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.ClueField;
import io.cordys.crm.clue.dto.request.ClueAddRequest;
import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.request.ClueUpdateRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

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

    protected static final String BATCH_TRANSFER = "batch/transfer";

    private static Clue addClue;
    private static Clue anotherClue;

    @Resource
    private BaseMapper<Clue> clueMapper;

    @Resource
    private BaseMapper<ClueField> clueFieldMapper;

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
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
         requestGetPermissionTest(PermissionConstants.CLUE_MANAGEMENT_READ, MODULE_FORM);
     }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CluePageRequest request = new CluePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ClueListResponse>> pageResult = getPageResult(mvcResult, ClueListResponse.class);
        List<ClueListResponse> clueList = pageResult.getList();
        Assertions.assertTrue(CollectionUtils.isEmpty(clueList));

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
        request.setContact("test");
        request.setPhone("18750920048");
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

        // 校验重名异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), ClueResultCode.CLUE_EXIST);

        // 创建另一个客户
        request.setName("another");
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Clue.class);
        anotherClue = clueMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addClue = clue;

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
        request.setOwner("bb22");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Clue clueResult = clueMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), clueResult.getName());
        Assertions.assertEquals(request.getOwner(), clueResult.getOwner());

        // 不修改信息
        ClueUpdateRequest emptyRequest = new ClueUpdateRequest();
        emptyRequest.setId(addClue.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验重名异常
        request.setId(addClue.getId());
        request.setName(anotherClue.getName());
        assertErrorCode(this.requestPost(DEFAULT_UPDATE, request), ClueResultCode.CLUE_EXIST);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CLUE_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addClue.getId());
        ClueGetResponse getResponse = getResultData(mvcResult, ClueGetResponse.class);

        // 校验请求成功数据
        Clue clue = clueMapper.selectByPrimaryKey(addClue.getId());
        Clue responseClue = BeanUtils.copyBean(new Clue(), getResponse);
        responseClue.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        responseClue.setInSharedPool(false);
        responseClue.setCollectionTime(addClue.getCollectionTime());
        Assertions.assertEquals(responseClue, clue);

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

    private List<ClueField> getClueFields(String clueId) {
        ClueField example = new ClueField();
        example.setResourceId(clueId);
        return clueFieldMapper.select(example);
    }
}