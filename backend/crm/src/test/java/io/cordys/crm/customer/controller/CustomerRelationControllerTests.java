package io.cordys.crm.customer.controller;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.constants.CustomerRelationType;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import io.cordys.crm.customer.dto.response.CustomerRelationListResponse;
import io.cordys.crm.customer.service.CustomerRelationService;
import io.cordys.crm.customer.service.CustomerService;
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

    private static Customer addCustomer;
    private static Customer anotherAddCustomer;

    @Resource
    private CustomerRelationService customerRelationService;

    @Resource
    private CustomerService customerService;

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

        List<CustomerRelationSaveRequest> requests = List.of(new CustomerRelationSaveRequest(anotherAddCustomer.getId(), CustomerRelationType.SUBSIDIARY.name()));
        requestPostWithOk(SAVE, requests, addCustomer.getId());
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
    @Order(10)
    void batchDelete() {
        customerRelationService.deleteByCustomerId(addCustomer.getId());
        List<CustomerRelationListResponse> list = customerRelationService.list(addCustomer.getId());
        Assertions.assertTrue(list.isEmpty());
    }
}