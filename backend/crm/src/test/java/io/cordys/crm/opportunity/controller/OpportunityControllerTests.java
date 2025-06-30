package io.cordys.crm.opportunity.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.ExportHeadDTO;
import io.cordys.common.dto.ExportSelectRequest;
import io.cordys.common.dto.ResourceTabEnableDTO;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.dto.request.*;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpportunityControllerTests extends BaseTest {

    private static final String BASE_PATH = "/opportunity/";
    protected static final String MODULE_FORM = "module/form";
    protected static final String BATCH_TRANSFER = "batch/transfer";
    protected static final String TAB = "tab";
    protected static final String UPDATE_STAGE = "update/stage";
    protected static final String CONTACT_LIST = "contact/list/{0}";
    protected static final String EXPORT_ALL = "export-all";
    protected static final String EXPORT_SELECT = "export-select";

    private static Opportunity addOpportunity;
    @Resource
    private BaseMapper<Opportunity> opportunityMapper;


    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        this.requestGetWithOkAndReturn(MODULE_FORM);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, MODULE_FORM);
    }


    @Test
    @Order(2)
    void testPage() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
    }


    @Sql(scripts = {"/dml/init_opportunity_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Test
    @Order(1)
    void testAdd() throws Exception {
        OpportunityAddRequest request = new OpportunityAddRequest();
        request.setName("商机1");
        request.setCustomerId("123");
        request.setAmount(BigDecimal.valueOf(1.1));
        request.setProducts(List.of("11"));
        request.setPossible(BigDecimal.valueOf(1.2));
        request.setContactId("12345");
        request.setOwner("admin");
        request.setExpectedEndTime(System.currentTimeMillis());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Opportunity resultData = getResultData(mvcResult, Opportunity.class);
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(resultData.getId());
        addOpportunity = opportunity;

        request.setProducts(List.of("11", "22"));
        this.requestPost(DEFAULT_ADD, request);
    }


    @Test
    @Order(3)
    void testUpdate() throws Exception {
        OpportunityUpdateRequest request = new OpportunityUpdateRequest();
        request.setId("1234");
        request.setName("商机2");
        request.setCustomerId("123");
        request.setContactId("1234567");
        request.setOwner("admin");
        request.setProducts(List.of("22"));
        request.setExpectedEndTime(System.currentTimeMillis());
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId(addOpportunity.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    void testBatchTransfer() throws Exception {
        OpportunityTransferRequest request = new OpportunityTransferRequest();
        request.setIds(List.of("1234"));
        request.setOwner("12345");
        this.requestPostWithOk(BATCH_TRANSFER, request);
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
        requestGetPermissionTest(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ, TAB);
    }


    @Test
    @Order(5)
    void testDelete() throws Exception {
        this.requestGet(DEFAULT_DELETE, "123456");
        this.requestGetWithOk(DEFAULT_DELETE, addOpportunity.getId());
    }


    @Test
    @Order(6)
    void testBatchDelete() throws Exception {
        this.requestPostWithOk(DEFAULT_BATCH_DELETE, List.of("123"));
    }


    @Test
    @Order(2)
    void testGetDetail() throws Exception {
        this.requestGet(DEFAULT_GET, addOpportunity.getId());
    }

    @Test
    @Order(3)
    void testUpdateStage() throws Exception {
        OpportunityStageRequest request = new OpportunityStageRequest();
        request.setId(addOpportunity.getId());
        request.setStage("SUCCESS");
        this.requestPostWithOk(UPDATE_STAGE, request);
    }

    @Test
    @Order(7)
    @Sql(scripts = {"/dml/init_opportunity_rule_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/dml/cleanup_opportunity_rule_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageReservedDay() throws Exception {
        OpportunityAddRequest request = new OpportunityAddRequest();
        request.setName("商机1");
        request.setCustomerId("123");
        request.setAmount(BigDecimal.valueOf(1.1));
        request.setProducts(List.of("11"));
        request.setPossible(BigDecimal.valueOf(1.2));
        request.setContactId("12345");
        request.setOwner("admin");
        request.setExpectedEndTime(System.currentTimeMillis());
        this.requestPostWithOk(DEFAULT_ADD, request);
        OpportunityPageRequest pageRequest = new OpportunityPageRequest();
        pageRequest.setSearchType("ALL");
        pageRequest.setCurrent(1);
        pageRequest.setPageSize(10);
        this.requestPostWithOk(DEFAULT_PAGE, pageRequest);
    }


    @Test
    @Order(2)
    void testGetContactList() throws Exception {
        this.requestGet(CONTACT_LIST, addOpportunity.getId());
        this.requestGet(CONTACT_LIST, "1234567");
    }


    @Test
    @Order(4)
    void testExport() throws Exception {
        OpportunityExportRequest request = new OpportunityExportRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setFileName("测试导出");

        ExportHeadDTO exportHeadDTO = new ExportHeadDTO();
        exportHeadDTO.setKey("name");
        exportHeadDTO.setTitle("商机名称");
        List<ExportHeadDTO> list = new ArrayList<>();
        list.add(exportHeadDTO);
        request.setHeadList(list);

        this.requestPostWithOk(EXPORT_ALL, request);
    }


    @Test
    @Order(4)
    void testExportSelect() throws Exception {
        ExportSelectRequest request = new ExportSelectRequest();
        request.setFileName("测试导出选中");

        ExportHeadDTO exportHeadDTO = new ExportHeadDTO();
        exportHeadDTO.setKey("name");
        exportHeadDTO.setTitle("商机名称");
        List<ExportHeadDTO> list = new ArrayList<>();
        list.add(exportHeadDTO);
        request.setHeadList(list);

        request.setIds(List.of(addOpportunity.getId()));

        this.requestPostWithOk(EXPORT_SELECT, request);
    }
}
