package io.cordys.crm.customer.controller;

import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.domain.CustomerField;
import io.cordys.crm.customer.dto.request.CustomerAddRequest;
import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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

    protected static final String BATCH_TRANSFER = "batch/transfer";
    protected static final String BATCH_TO_POOL = "batch/to-pool";
    protected static final String OPTION = "option";

    private static Customer addCustomer;
    private static Customer anotherCustomer;
    private static List<String> batchIds = new ArrayList<>();

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
         requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_POOL_READ), MODULE_FORM);
     }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);

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
                .filter(field -> StringUtils.equals(field.getInternalKey(), "customerLevel"))
                .findFirst().orElse(null);

        // 请求成功
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("aa");
        request.setOwner("bb");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleField.getId(), "1")));

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Customer resultData = getResultData(mvcResult, Customer.class);
        Customer customer = customerMapper.selectByPrimaryKey(resultData.getId());
        Assertions.assertEquals(request.getName(), customer.getName());
        Assertions.assertEquals(request.getOwner(), customer.getOwner());

        // 校验字段
        List<BaseModuleFieldValue> fieldValues = getCustomerFields(customer.getId())
                .stream().map(customerField -> {
                    BaseModuleFieldValue baseModuleFieldValue = BeanUtils.copyBean(new BaseModuleFieldValue(), customerField);
                    baseModuleFieldValue.setFieldValue(customerField.getFieldValue().toString());
                    return baseModuleFieldValue;
                })
                .toList();
        Assertions.assertEquals(request.getModuleFields(), fieldValues);

        // 校验重名异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), CustomerResultCode.CUSTOMER_EXIST);

        // 创建另一个客户
        request.setName("another");
        request.setOwner(InternalUser.ADMIN.getValue());
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
        request.setOwner(InternalUser.ADMIN.getValue());
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

        addCustomer = customerMapper.selectByPrimaryKey(addCustomer.getId());

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
        Assertions.assertNotNull(getResponse.getOwnerName());
        if (!getResponse.getOwner().equals(InternalUser.ADMIN.getValue())) {
            Assertions.assertNotNull(getResponse.getDepartmentId());
            Assertions.assertNotNull(getResponse.getDepartmentName());
        }

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DEFAULT_GET, addCustomer.getId());
    }


    @Test
    @Order(3)
    void testPage() throws Exception {
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        request.setSearchType(BusinessSearchType.ALL.name());
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
            Assertions.assertNotNull(customerListResponse.getOwnerName());
            if (!customerListResponse.getOwner().equals(InternalUser.ADMIN.getValue())) {
                Assertions.assertNotNull(customerListResponse.getDepartmentId());
                Assertions.assertNotNull(customerListResponse.getDepartmentName());
            }
        });

        request.setSearchType(BusinessSearchType.SELF.name());
        this.requestPostWithOk(DEFAULT_PAGE, request);

        request.setSearchType(BusinessSearchType.DEPARTMENT.name());
        this.requestPostWithOk(DEFAULT_PAGE, request);

        request.setSearchType(BusinessSearchType.VISIBLE.name());
        this.requestPostWithOk(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(4)
    void testTransfer() throws Exception {
        CustomerBatchTransferRequest request = new CustomerBatchTransferRequest();
        request.setIds(List.of(addCustomer.getId()));
        request.setOwner(PERMISSION_USER_NAME);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(BATCH_TRANSFER, request);

        getResultData(mvcResult, CustomerGetResponse.class);

        // 校验请求成功数据
        Customer customer = customerMapper.selectByPrimaryKey(addCustomer.getId());
        Assertions.assertEquals(PERMISSION_USER_NAME, customer.getOwner());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, BATCH_TRANSFER, request);
    }

    @Test
    @Order(4)
    void testOption() throws Exception {
        BasePageRequest request = new BasePageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(OPTION, request);

        List<OptionDTO> options = getPageResult(mvcResult, OptionDTO.class).getList();

        Assertions.assertTrue(CollectionUtils.isNotEmpty(options));

        request.setKeyword(addCustomer.getName());
        mvcResult = this.requestPostWithOkAndReturn(OPTION, request);
        options = getPageResult(mvcResult, OptionDTO.class).getList();

        Assertions.assertTrue(options.size() == 1);
        Assertions.assertEquals(addCustomer.getId(), options.getFirst().getId());
        Assertions.assertEquals(addCustomer.getName(), options.getFirst().getName());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, OPTION, request);
    }

    @Test
    @Order(10)
    void testDelete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addCustomer.getId());
        Assertions.assertNull(customerMapper.selectByPrimaryKey(addCustomer.getId()));

        List<CustomerField> fields = getCustomerFields(addCustomer.getId());
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DEFAULT_DELETE, addCustomer.getId());
    }

    @Test
    @Order(11)
    void testBatchDelete() throws Exception {
        this.requestPostWithOk(DEFAULT_BATCH_DELETE, List.of(anotherCustomer.getId()));
        Assertions.assertNull(customerMapper.selectByPrimaryKey(anotherCustomer.getId()));

        List<CustomerField> fields = getCustomerFields(anotherCustomer.getId());
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_DELETE, DEFAULT_BATCH_DELETE, List.of(anotherCustomer.getId()));
    }

    @Test
    @Order(12)
    @Sql(scripts = {"/dml/init_customer_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = {"/dml/cleanup_customer_test.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void testPageReservedDay() throws Exception {
        CustomerAddRequest addRequest = new CustomerAddRequest();
        addRequest.setName("aa");
        addRequest.setOwner("admin");
        this.requestPostWithOk(DEFAULT_ADD, addRequest);
        CustomerPageRequest request = new CustomerPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);
        request.setSearchType(BusinessSearchType.ALL.name());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomerListResponse>> pageResult = getPageResult(mvcResult, CustomerListResponse.class);
        List<CustomerListResponse> customerList = pageResult.getList();
        customerList.forEach(customerListResponse -> {
            batchIds.add(customerListResponse.getId());
        });
        this.requestPostWithOk(BATCH_TO_POOL, batchIds);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_RECYCLE, BATCH_TO_POOL, batchIds);
    }

    @Test
    @Order(13)
    void testBatchToPool() throws Exception {
        this.requestPostWithOk(BATCH_TO_POOL, batchIds);
    }

    private List<CustomerField> getCustomerFields(String customerId) {
        CustomerField example = new CustomerField();
        example.setResourceId(customerId);
        return customerFieldMapper.select(example);
    }
}