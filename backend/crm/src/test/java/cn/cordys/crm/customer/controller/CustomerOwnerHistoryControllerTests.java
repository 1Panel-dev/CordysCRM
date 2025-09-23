package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerOwner;
import cn.cordys.crm.customer.dto.request.CustomerAddRequest;
import cn.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import cn.cordys.crm.customer.dto.response.CustomerOwnerListResponse;
import cn.cordys.crm.customer.service.CustomerOwnerHistoryService;
import cn.cordys.crm.customer.service.CustomerService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerOwnerHistoryControllerTests extends BaseTest {
    private static final String BASE_PATH = "/account/owner/history/";
    protected static final String LIST = "list/{0}";
    private static Customer addCustomer;

    @Resource
    private CustomerOwnerHistoryService customerOwnerHistoryService;

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
        List<CustomerOwnerListResponse> list = getResultDataArray(mvcResult, CustomerOwnerListResponse.class);
        Assertions.assertTrue(CollectionUtils.isEmpty(list));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, "111");
    }

    @Test
    @Order(1)
    void testAdd() {
        CustomerAddRequest customerAddRequest = new CustomerAddRequest();
        customerAddRequest.setName(UUID.randomUUID().toString());
        customerAddRequest.setOwner(InternalUser.ADMIN.getValue());
        addCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);

        CustomerOwner customerOwner = customerOwnerHistoryService.add(addCustomer, InternalUser.ADMIN.getValue(), false);
        // 校验成功数据
        Assertions.assertEquals(customerOwner.getCustomerId(), addCustomer.getId());
        Assertions.assertEquals(customerOwner.getOperator(), InternalUser.ADMIN.getValue());
        Assertions.assertEquals(customerOwner.getCollectionTime(), addCustomer.getCollectionTime());
        Assertions.assertEquals(customerOwner.getOwner(), addCustomer.getOwner());
    }

    @Test
    @Order(2)
    void testBatchAdd() {
        CustomerBatchTransferRequest transferRequest = new CustomerBatchTransferRequest();
        transferRequest.setIds(List.of(addCustomer.getId()));
        transferRequest.setOwner(PERMISSION_USER_NAME);
        customerOwnerHistoryService.batchAdd(transferRequest, InternalUser.ADMIN.getValue());

        List<CustomerOwnerListResponse> list = customerOwnerHistoryService.list(addCustomer.getId(), DEFAULT_ORGANIZATION_ID);
        for (CustomerOwnerListResponse customerOwner : list) {
            // 校验成功数据
            Assertions.assertEquals(customerOwner.getCustomerId(), addCustomer.getId());
            Assertions.assertEquals(customerOwner.getOperator(), InternalUser.ADMIN.getValue());
            Assertions.assertEquals(customerOwner.getCollectionTime(), addCustomer.getCollectionTime());
            Assertions.assertEquals(customerOwner.getOwner(), addCustomer.getOwner());
        }
    }

    @Test
    @Order(3)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, addCustomer.getId());
        List<CustomerOwnerListResponse> list = getResultDataArray(mvcResult, CustomerOwnerListResponse.class);
        list.forEach(customerOwner -> {
            // 校验成功数据
            Assertions.assertEquals(customerOwner.getCustomerId(), addCustomer.getId());
            Assertions.assertEquals(customerOwner.getOperator(), InternalUser.ADMIN.getValue());
            Assertions.assertEquals(customerOwner.getCollectionTime(), addCustomer.getCollectionTime());
            Assertions.assertEquals(customerOwner.getOwner(), addCustomer.getOwner());
            Assertions.assertNotNull(customerOwner.getOwnerName());
            Assertions.assertNotNull(customerOwner.getOwnerName());
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, "111");
    }

    @Test
    @Order(10)
    void batchDelete() {
        customerOwnerHistoryService.deleteByCustomerIds(List.of(addCustomer.getId()));
        List<CustomerOwnerListResponse> list = customerOwnerHistoryService.list(addCustomer.getId(), DEFAULT_ORGANIZATION_ID);
        Assertions.assertTrue(list.isEmpty());
    }
}