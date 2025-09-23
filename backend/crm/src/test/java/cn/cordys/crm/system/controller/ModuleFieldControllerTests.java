package cn.cordys.crm.system.controller;

import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.request.ProductPageRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleFieldControllerTests extends BaseTest {

    public static final String BASE_PATH = "/field";
    public static final String DEPT_TREE = "/dept/tree";
    public static final String USER_DEPT_TREE = "/user/dept/tree";
    public static final String CLUE_SOURCE_DATA = "/source/lead";
    public static final String CUSTOMER_SOURCE_DATA = "/source/account";
    public static final String CONTACT_SOURCE_DATA = "/source/contact";
    public static final String OPPORTUNITY_SOURCE_DATA = "/source/opportunity";
    public static final String PRODUCT_SOURCE_DATA = "/source/product";
    public static String FIELD_ID = "field-test-id";
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void initData() {
        ModuleField field = new ModuleField();
        field.setId(FIELD_ID);
        field.setFormId("form-test-1");
        field.setName("dep-test-1");
        field.setMobile(false);
        field.setType(FieldType.DEPARTMENT.name());
        field.setPos(1L);
        field.setCreateUser("admin");
        field.setCreateTime(System.currentTimeMillis());
        field.setUpdateUser("admin");
        field.setUpdateTime(System.currentTimeMillis());
        moduleFieldMapper.insert(field);
    }

    @Test
    @Order(2)
    void testGetDeptTree() throws Exception {
        this.requestGetWithOk(DEPT_TREE);
    }

    @Test
    @Order(3)
    void testGetUserDeptTree() throws Exception {
        this.requestGetWithOk(USER_DEPT_TREE);
    }

    @Test
    @Order(4)
    void resetFieldData() {
        ModuleField field = new ModuleField();
        field.setId(FIELD_ID);
        field.setType(FieldType.DATA_SOURCE.name());
        moduleFieldMapper.updateById(field);
    }

    @Test
    @Order(5)
    void testListClueSourceData() throws Exception {
        CluePageRequest cluePageRequest = new CluePageRequest();
        cluePageRequest.setCurrent(1);
        cluePageRequest.setPageSize(10);
        this.requestPostWithOk(CLUE_SOURCE_DATA, cluePageRequest);
    }

    @Test
    @Order(6)
    void testListCustomerSourceData() throws Exception {
        CustomerPageRequest customerPageRequest = new CustomerPageRequest();
        customerPageRequest.setCurrent(1);
        customerPageRequest.setPageSize(10);
        this.requestPostWithOk(CUSTOMER_SOURCE_DATA, customerPageRequest);
    }

    @Test
    @Order(7)
    void testListContactSourceData() throws Exception {
        CustomerContactPageRequest contactPageRequest = new CustomerContactPageRequest();
        contactPageRequest.setCurrent(1);
        contactPageRequest.setPageSize(10);
        this.requestPostWithOk(CONTACT_SOURCE_DATA, contactPageRequest);
    }

    @Test
    @Order(8)
    void testListOpportunitySourceData() throws Exception {
        OpportunityPageRequest opportunityPageRequest = new OpportunityPageRequest();
        opportunityPageRequest.setCurrent(1);
        opportunityPageRequest.setPageSize(10);
        this.requestPostWithOk(OPPORTUNITY_SOURCE_DATA, opportunityPageRequest);
    }

    @Test
    @Order(9)
    void testListProductSourceData() throws Exception {
        ProductPageRequest productPageRequest = new ProductPageRequest();
        productPageRequest.setCurrent(1);
        productPageRequest.setPageSize(10);
        this.requestPostWithOk(PRODUCT_SOURCE_DATA, productPageRequest);
    }
}
