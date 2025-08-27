package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.constants.CustomerRelationType;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerRelation;
import cn.cordys.crm.customer.dto.request.CustomerAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import cn.cordys.crm.customer.dto.request.CustomerRelationUpdateRequest;
import cn.cordys.crm.customer.dto.response.CustomerRelationListResponse;
import cn.cordys.crm.customer.service.CustomerRelationService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerRelationControllerTests extends BaseTest {
    private static final String BASE_PATH = "/customer/relation/";

    protected static final String LIST = "list/{0}";

    protected static final String SAVE = "save/{0}";
    protected static final String ADD = "add/{0}";
    protected static final String UPDATE = "update/{0}";
    protected static final String DELETE = "delete/{0}";

    private static Customer addCustomer;
    private static CustomerRelation addCustomerRelation;
    private static Customer anotherAddCustomer;

    @Resource
    private CustomerRelationService customerRelationService;

    @Resource
    private CustomerService customerService;

    @Resource
    private BaseMapper<CustomerRelation> customerRelationBaseMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testListEmpty() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, "111");
        List<CustomerRelationListResponse> list = getResultDataArray(mvcResult, CustomerRelationListResponse.class);
        Assertions.assertTrue(CollectionUtils.isEmpty(list));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, "111");
    }

    @Test
    @Order(1)
    void testSave() throws Exception {
        // 校验权限 放前面，否则会删掉创建的关系
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, SAVE, List.of(), "111");

        CustomerAddRequest customerAddRequest = new CustomerAddRequest();
        customerAddRequest.setName(UUID.randomUUID().toString());
        customerAddRequest.setOwner(InternalUser.ADMIN.getValue());
        addCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);

        customerAddRequest.setName(UUID.randomUUID().toString());
        anotherAddCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);

        List<CustomerRelationSaveRequest> requests = List.of(new CustomerRelationSaveRequest());
        requestPostWithOk(SAVE, requests, addCustomer.getId());

        requests = List.of(new CustomerRelationSaveRequest(anotherAddCustomer.getId(), CustomerRelationType.SUBSIDIARY.name()));
        requestPostWithOk(SAVE, requests, addCustomer.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, SAVE, requests, addCustomer.getId());
    }

    @Test
    @Order(3)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, addCustomer.getId());
        List<CustomerRelationListResponse> list = getResultDataArray(mvcResult, CustomerRelationListResponse.class);
        CustomerRelationListResponse response = list.getFirst();

        Assertions.assertEquals(response.getCustomerId(), anotherAddCustomer.getId());
        Assertions.assertEquals(response.getRelationType(), CustomerRelationType.SUBSIDIARY.name());
        Assertions.assertNotNull(response.getCustomerName());
    }

    @Test
    @Order(4)
    void testAdd() throws Exception {
        CustomerRelationSaveRequest customerRelationSaveRequest = new CustomerRelationSaveRequest(anotherAddCustomer.getId(), CustomerRelationType.SUBSIDIARY.name());

        MvcResult mvcResult = requestPostWithOkAndReturn(ADD, customerRelationSaveRequest, addCustomer.getId());
        addCustomerRelation = getResultData(mvcResult, CustomerRelation.class);
        addCustomerRelation = customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId());
        Assertions.assertEquals(addCustomerRelation.getSourceCustomerId(), addCustomer.getId());
        Assertions.assertEquals(addCustomerRelation.getTargetCustomerId(), anotherAddCustomer.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, ADD, customerRelationSaveRequest, addCustomer.getId());
    }

    @Test
    @Order(5)
    void testEdit() throws Exception {
        CustomerRelationUpdateRequest customerRelationUpdateRequest = new CustomerRelationUpdateRequest();
        customerRelationUpdateRequest.setId(addCustomerRelation.getId());
        customerRelationUpdateRequest.setCustomerId(anotherAddCustomer.getId());
        customerRelationUpdateRequest.setRelationType(CustomerRelationType.GROUP.name());
        requestPostWithOk(UPDATE, customerRelationUpdateRequest, addCustomer.getId());

        addCustomerRelation = customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId());
        Assertions.assertEquals(addCustomerRelation.getTargetCustomerId(), addCustomer.getId());
        Assertions.assertEquals(addCustomerRelation.getSourceCustomerId(), anotherAddCustomer.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, UPDATE, customerRelationUpdateRequest, addCustomer.getId());
    }

    @Test
    @Order(9)
    void delete() throws Exception {
        requestGetWithOk(DELETE, addCustomerRelation.getId());

        Assertions.assertNull(customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId()));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DELETE, addCustomerRelation.getId());
    }

    @Test
    @Order(10)
    void batchDelete() {
        customerRelationService.deleteByCustomerId(addCustomer.getId());
        List<CustomerRelationListResponse> list = customerRelationService.list(addCustomer.getId());
        Assertions.assertTrue(list.isEmpty());
    }
}