package io.cordys.crm.customer.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerField;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
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

    protected static final String MODULE_FORM = "module/form";

    private static Customer addCustomer;

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

     @Test
     @Order(0)
     void testModuleField() throws Exception {
         this.requestGetWithOkAndReturn(MODULE_FORM);

         // 校验权限
         requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, MODULE_FORM);
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
        request.setTags(List.of("tag1"));
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Customer resultData = getResultData(mvcResult, Customer.class);
        Customer customer = customerMapper.selectByPrimaryKey(resultData.getId());
        Assertions.assertEquals(request.getTags(), customer.getTags());

        // 校验请求成功数据
        this.addCustomer = customer;

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setId(addCustomer.getId());
        request.setTags(List.of("tag2"));
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Customer customerResult = customerMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getTags(), customerResult.getTags());

        // 不修改信息
        CustomerUpdateRequest emptyRequest = new CustomerUpdateRequest();
        emptyRequest.setId(addCustomer.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addCustomer.getId());
        CustomerGetResponse getResponse = getResultData(mvcResult, CustomerGetResponse.class);

        // 校验请求成功数据
        Customer customer = customerMapper.selectByPrimaryKey(addCustomer.getId());
        Customer responseCustomer = BeanUtils.copyBean(new Customer(), getResponse);
        responseCustomer.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        responseCustomer.setInSharedPool(false);
        Assertions.assertEquals(responseCustomer, customer);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DEFAULT_GET, addCustomer.getId());
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
        Assertions.assertNull(customerMapper.selectByPrimaryKey(addCustomer.getId()));

        CustomerField example = new CustomerField();
        example.setCustomerId(addCustomer.getId());
        List<CustomerField> fields = customerFieldMapper.select(example);
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DEFAULT_DELETE, addCustomer.getId());
    }
}