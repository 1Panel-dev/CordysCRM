package io.cordys.crm.opportunity.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.opportunity.domain.Opportunity;
import io.cordys.crm.opportunity.dto.request.OpportunityAddRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityUpdateRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpportunityControllerTests extends BaseTest {

    private static final String BASE_PATH = "/opportunity/";
    protected static final String MODULE_FORM = "module/form";

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
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Opportunity resultData = getResultData(mvcResult, Opportunity.class);
        Opportunity opportunity = opportunityMapper.selectByPrimaryKey(resultData.getId());
        this.addOpportunity = opportunity;

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
        request.setModuleFields(List.of(new BaseModuleFieldValue("id", "value")));
        this.requestPost(DEFAULT_UPDATE, request);

        request.setId(addOpportunity.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
    }

}
