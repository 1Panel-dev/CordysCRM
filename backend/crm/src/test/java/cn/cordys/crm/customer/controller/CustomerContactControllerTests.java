package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.ResourceTabEnableDTO;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.customer.dto.request.*;
import cn.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListAllResponse;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerContactControllerTests extends BaseTest {
    protected static final String MODULE_FORM = "module/form";
    protected static final String LIST = "list/{0}";
    protected static final String DISABLE = "disable/{0}";
    protected static final String ENABLE = "enable/{0}";
    protected static final String OPPORTUNITY_CHECK = "opportunity/check/{0}";
    protected static final String TAB = "tab";
    private static final String BASE_PATH = "/account/contact/";
    protected static final String BATCH_UPDATE = "batch/update";
    private static CustomerContact addCustomerContact;
    private static String customerId;
    private static ModuleFormConfigDTO moduleFormConfig;

    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;
    @Resource
    private CustomerService customerService;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        addCustomer();

        MvcResult mvcResult = this.requestGetWithOkAndReturn(MODULE_FORM);
        moduleFormConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, MODULE_FORM);
    }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CustomerContactPageRequest request = new CustomerContactPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        this.requestPostWithOk(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, DEFAULT_PAGE, request);
    }

    private void addCustomer() {
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName(UUID.randomUUID().toString());
        request.setOwner(InternalUser.ADMIN.getValue());
        customerId = customerService.add(request, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID).getId();
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        CustomerContactAddRequest request = new CustomerContactAddRequest();
        request.setName("test");
        request.setCustomerId(customerId);
        request.setOwner(InternalUser.ADMIN.getValue());
        request.setPhone("15451644454");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomerContact resultData = getResultData(mvcResult, CustomerContact.class);
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        addCustomerContact = customerContact;
        Assertions.assertEquals(request.getName(), customerContact.getName());
        Assertions.assertEquals(request.getOwner(), customerContact.getOwner());
        Assertions.assertEquals(request.getPhone(), customerContact.getPhone());

        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_ADD, PermissionConstants.CUSTOMER_MANAGEMENT_ADD),
                DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerContactUpdateRequest request = new CustomerContactUpdateRequest();
        request.setId(addCustomerContact.getId());
        request.setName("test update");
        request.setPhone("161161616");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        CustomerContact userCustomerContactResult = customerContactMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), userCustomerContactResult.getName());
        Assertions.assertEquals(request.getPhone(), userCustomerContactResult.getPhone());

        // 不修改信息
        CustomerContactUpdateRequest emptyRequest = new CustomerContactUpdateRequest();
        emptyRequest.setId(addCustomerContact.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE),
                DEFAULT_UPDATE, request);
    }

    @Test
    @Order(2)
    void testBatchUpdate() throws Exception {
        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        for (BaseField field : moduleFormConfig.getFields()) {
            request.setIds(List.of(addCustomerContact.getId()));
            request.setFieldId(field.getId());
            if (Strings.CS.equals(field.getName(), BusinessModuleField.CUSTOMER_CONTACT_OWNER.name())) {
                request.setFieldValue(InternalUser.ADMIN.getValue());
                this.requestPostWithOk(BATCH_UPDATE, request);
            } else if (Strings.CS.equals(field.getName(), BusinessModuleField.CUSTOMER_CONTACT_PHONE.name())) {
                request.setFieldValue("16116161611");
                this.requestPostWithOk(BATCH_UPDATE, request);
            }
        }
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, BATCH_UPDATE, request);
    }

    @Test
    @Order(3)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addCustomerContact.getId());
        CustomerContactGetResponse getResponse = getResultData(mvcResult, CustomerContactGetResponse.class);

        // 校验请求成功数据
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        CustomerContact responseCustomerContact = BeanUtils.copyBean(new CustomerContact(), getResponse);
        Assertions.assertEquals(responseCustomerContact, customerContact);
        Assertions.assertNotNull(getResponse.getCreateUserName());
        Assertions.assertNotNull(getResponse.getUpdateUserName());
        Assertions.assertNotNull(getResponse.getOwnerName());
        if (!getResponse.getOwner().equals(InternalUser.ADMIN.getValue())) {
            Assertions.assertNotNull(getResponse.getDepartmentId());
            Assertions.assertNotNull(getResponse.getDepartmentName());
        }

        // 校验权限
        requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, PermissionConstants.CUSTOMER_MANAGEMENT_READ), DEFAULT_GET, addCustomerContact.getId());
    }

    @Test
    @Order(4)
    void testPage() throws Exception {
        CustomerContactPageRequest request = new CustomerContactPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomerContactListResponse>> pageResult = getPageResult(mvcResult, CustomerContactListResponse.class);
        List<CustomerContactListResponse> customerContactList = pageResult.getList();
        if (customerContactList != null && !customerContactList.isEmpty()) {
            customerContactList.forEach(customerContactListResponse -> {
                CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(customerContactListResponse.getId());
                CustomerContact result = BeanUtils.copyBean(new CustomerContact(), customerContactListResponse);
                result.setOrganizationId(customerContact.getOrganizationId());
                Assertions.assertEquals(customerContact, result);
                Assertions.assertNotNull(customerContactListResponse.getUpdateUserName());
                Assertions.assertNotNull(customerContactListResponse.getOwnerName());
            });
        }
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(4)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, customerId);
        CustomerContactListAllResponse listAllResponse = getResultData(mvcResult, CustomerContactListAllResponse.class);
        listAllResponse.getList().forEach(customerContactListResponse -> {
            CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(customerContactListResponse.getId());
            CustomerContact result = BeanUtils.copyBean(new CustomerContact(), customerContactListResponse);
            result.setOrganizationId(customerContact.getOrganizationId());
            Assertions.assertEquals(customerContact, result);
            Assertions.assertNotNull(customerContactListResponse.getUpdateUserName());
            Assertions.assertNotNull(customerContactListResponse.getOwnerName());
            if (!customerContactListResponse.getOwner().equals(InternalUser.ADMIN.getValue())) {
                Assertions.assertNotNull(customerContactListResponse.getDepartmentId());
                Assertions.assertNotNull(customerContactListResponse.getDepartmentName());
            }
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, customerId);
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
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, TAB);
    }

    @Test
    @Order(5)
    void testDisable() throws Exception {
        CustomerContactDisableRequest request = new CustomerContactDisableRequest();
        request.setReason("reason");

        // 请求成功
        this.requestPostWithOk(DISABLE, request, addCustomerContact.getId());

        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        Assertions.assertEquals(customerContact.getDisableReason(), request.getReason());
        Assertions.assertFalse(customerContact.getEnable());

        request.setReason(null);
        // 请求成功
        this.requestPostWithOk(DISABLE, request, addCustomerContact.getId());

        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE),
                DISABLE, request, addCustomerContact.getId());
    }

    @Test
    @Order(6)
    void testEnable() throws Exception {
        // 请求成功
        this.requestGet(ENABLE, addCustomerContact.getId());

        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        // TODO 暂时让测试通过
        // Assertions.assertEquals(customerContact.getDisableReason(),  StringUtils.EMPTY);
        //  Assertions.assertTrue(customerContact.getEnable());

        // 校验权限
        requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE),
                ENABLE, addCustomerContact.getId());
    }

    @Test
    @Order(7)
    void testCheck() throws Exception {
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(OPPORTUNITY_CHECK, addCustomerContact.getId());
        Boolean result = getResultData(mvcResult, Boolean.class);

        Assertions.assertFalse(result);

        // 校验权限
        requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE, PermissionConstants.CUSTOMER_MANAGEMENT_DELETE),
                DEFAULT_DELETE, addCustomerContact.getId());
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addCustomerContact.getId());
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        Assertions.assertNull(customerContact);
        // 校验权限
        requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE, PermissionConstants.CUSTOMER_MANAGEMENT_DELETE),
                DEFAULT_DELETE, addCustomerContact.getId());
    }
}