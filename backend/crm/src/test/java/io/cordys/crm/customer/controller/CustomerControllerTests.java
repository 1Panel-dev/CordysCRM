package io.cordys.crm.customer.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerField;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.request.CustomerUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerGetResponse;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
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
class CustomerControllerTests extends BaseTest {
    private static final String BASE_PATH = "/customer/";

    protected static final String MODULE_FORM = "module/form";

    private static Customer addCustomer;
    private static Customer anotherCustomer;

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;

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
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
        ModuleField moduleField = moduleFieldMapper.select(example)
                .stream()
                .filter(field -> StringUtils.equals(field.getInternalKey(), "customerTag"))
                .findFirst().orElse(null);

        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("aa");
        request.setOwner("bb");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), List.of("value"))));

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Customer resultData = getResultData(mvcResult, Customer.class);
        Customer customer = customerMapper.selectByPrimaryKey(resultData.getId());
        Assertions.assertEquals(request.getName(), customer.getName());
        Assertions.assertEquals(request.getOwner(), customer.getOwner());

        // 校验字段
        List<BaseModuleFieldValue> fieldValues = getCustomerFields(customer.getId())
                .stream().map(customerField -> {
                    BaseModuleFieldValue baseModuleFieldValue = BeanUtils.copyBean(new BaseModuleFieldValue(), customerField);
                    baseModuleFieldValue.setFieldValue(JSON.parseArray(customerField.getFieldValue().toString(), String.class));
                    return baseModuleFieldValue;
                })
                .toList();
        Assertions.assertEquals(request.getModuleFields(), fieldValues);

        // 校验重名异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), CustomerResultCode.CUSTOMER_EXIST);

        // 创建另一个客户
        request.setName("another");
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Customer.class);
        anotherCustomer = customerMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addCustomer = customer;

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_ADD, DEFAULT_ADD, request);
    }

    private ModuleForm getModuleForm() {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setFormKey(FormKey.CUSTOMER.getKey());
        return moduleFormMapper.selectOne(example);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setId(addCustomer.getId());
        request.setName("aa11");
        request.setOwner("bb22");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Customer customerResult = customerMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), customerResult.getName());
        Assertions.assertEquals(request.getOwner(), customerResult.getOwner());

        // 不修改信息
        CustomerUpdateRequest emptyRequest = new CustomerUpdateRequest();
        emptyRequest.setId(addCustomer.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验重名异常
        request.setId(addCustomer.getId());
        request.setName(anotherCustomer.getName());
        assertErrorCode(this.requestPost(DEFAULT_UPDATE, request), CustomerResultCode.CUSTOMER_EXIST);

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
        responseCustomer.setCollectionTime(addCustomer.getCollectionTime());
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

        List<CustomerField> fields = getCustomerFields(addCustomer.getId());
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DEFAULT_DELETE, addCustomer.getId());
    }

    private List<CustomerField> getCustomerFields(String customerId) {
        CustomerField example = new CustomerField();
        example.setCustomerId(customerId);
        return customerFieldMapper.select(example);
    }
}