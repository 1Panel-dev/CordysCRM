package io.cordys.crm.customer.controller;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.constants.CustomerResultCode;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerContactAddRequest;
import io.cordys.crm.customer.dto.request.CustomerContactDisableRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerContactGetResponse;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerContactControllerTests extends BaseTest {
    private static final String BASE_PATH = "/customer/contact/";
    protected static final String MODULE_FORM = "module/form";
    protected static final String DISABLE = "disable/{0}";
    protected static final String ENABLE = "enable/{0}";

    private static CustomerContact addCustomerContact;

    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        this.requestGetWithOkAndReturn(MODULE_FORM);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, MODULE_FORM);
    }

    @Test
    @Order(0)
    void testPageEmpty() throws Exception {
        CustomerContactPageRequest request = new CustomerContactPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomerContactListResponse>> pageResult = getPageResult(mvcResult, CustomerContactListResponse.class);
        List<CustomerContactListResponse> customerContactList = pageResult.getList();
        Assertions.assertTrue(CollectionUtils.isEmpty(customerContactList));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        CustomerContactAddRequest request = new CustomerContactAddRequest();
        request.setName("test");
        request.setCustomerId("customerId");
        request.setOwner(InternalUser.ADMIN.getValue());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomerContact resultData = getResultData(mvcResult, CustomerContact.class);
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addCustomerContact = customerContact;
        Assertions.assertEquals(request.getName(), customerContact.getName());
        Assertions.assertEquals(request.getOwner(), customerContact.getOwner());

        // 校验重名异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), CustomerResultCode.CUSTOMER_CONTACT_EXIST);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerContactUpdateRequest request = new CustomerContactUpdateRequest();
        request.setId(addCustomerContact.getId());
        request.setName("test update");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        CustomerContact userCustomerContactResult = customerContactMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), userCustomerContactResult.getName());

        // 不修改信息
        CustomerContactUpdateRequest emptyRequest = new CustomerContactUpdateRequest();
        emptyRequest.setId(addCustomerContact.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, DEFAULT_UPDATE, request);
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
        Assertions.assertNotNull(getResponse.getDepartmentName());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, DEFAULT_GET, addCustomerContact.getId());
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
        customerContactList.forEach(customerContactListResponse -> {
            CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(customerContactListResponse.getId());
            CustomerContact result = BeanUtils.copyBean(new CustomerContact(), customerContactListResponse);
            result.setOrganizationId(customerContact.getOrganizationId());
            Assertions.assertEquals(customerContact, result);
            Assertions.assertNotNull(customerContactListResponse.getUpdateUserName());
            Assertions.assertNotNull(customerContactListResponse.getDepartmentName());
        });
        
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_READ, DEFAULT_PAGE, request);
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
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, DISABLE, request, addCustomerContact.getId());
    }

    @Test
    @Order(6)
    void testEnable() throws Exception {
        // 请求成功
        this.requestGet(ENABLE, addCustomerContact.getId());

        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        Assertions.assertEquals(customerContact.getDisableReason(), StringUtils.EMPTY);
        Assertions.assertTrue(customerContact.getEnable());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_UPDATE, ENABLE, addCustomerContact.getId());
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addCustomerContact.getId());
        CustomerContact customerContact = customerContactMapper.selectByPrimaryKey(addCustomerContact.getId());
        Assertions.assertNull(customerContact);
        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_CONTACT_DELETE, DEFAULT_DELETE, addCustomerContact.getId());
    }
}