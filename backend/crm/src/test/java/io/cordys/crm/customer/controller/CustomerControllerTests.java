package io.cordys.crm.customer.controller;

import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.system.dto.response.ModuleFieldDTO;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerControllerTests extends BaseTest {
    private static final String BASE_PATH = "/customer/";

    protected static final String MODULE_FIELD = "module/field";

    private static Customer addCustomer;

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(MODULE_FIELD);
        List<ModuleFieldDTO> moduleFields = getResultDataArray(mvcResult, ModuleFieldDTO.class);
        moduleFields.forEach(moduleField -> {
            Assertions.assertEquals(moduleField.getModuleId(), ModuleKey.CUSTOMER);
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, MODULE_FIELD);
    }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomerListResponse>> pageResult = getPageResult(mvcResult, CustomerListResponse.class);
        List<CustomerListResponse> customerList = pageResult.getList();
        Assertions.assertTrue(CollectionUtils.isEmpty(customerList));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("test");
        request.setDealStatus("test");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Customer resultData = getResultData(mvcResult, Customer.class);
        Customer customer = customerMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addCustomer = customer;
        Assertions.assertEquals(request.getName(), customer.getName());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setId(addCustomer.getId());
        request.setName("test update");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Customer userCustomerResult = customerMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), userCustomerResult.getName());

        // 不修改信息
        CustomerUpdateRequest emptyRequest = new CustomerUpdateRequest();
        emptyRequest.setId(addCustomer.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testPage() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomerListResponse>> pageResult = getPageResult(mvcResult, CustomerListResponse.class);
        List<CustomerListResponse> customerList = pageResult.getList();

        Customer example = new Customer();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setInSharedPool(false);
        Map<String, Customer> customerMap = customerMapper.select(example)
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));

        customerList.forEach(customerListResponse -> {
            Customer customer = customerMap.get(customerListResponse.getId());
            Customer responseCustomer = BeanUtils.copyBean(new Customer(), customerListResponse);
            responseCustomer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            responseCustomer.setInSharedPool(false);
            Assertions.assertEquals(customer, responseCustomer);
        });


        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addCustomer.getId());
        // todo
        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DEFAULT_DELETE, addCustomer.getId());
    }
}