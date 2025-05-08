package io.cordys.crm.customer.controller;

import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.customer.constants.CustomerCollaborationType;
import io.cordys.crm.customer.domain.CustomerCollaboration;
import io.cordys.crm.customer.dto.request.CustomerCollaborationAddRequest;
import io.cordys.crm.customer.dto.request.CustomerCollaborationUpdateRequest;
import io.cordys.crm.customer.dto.response.CustomerCollaborationListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerCollaborationControllerTests extends BaseTest {
    private static final String BASE_PATH = "/customer/collaboration/";
    private static final String LIST = "/list/{0}";

    private static CustomerCollaboration addCustomerCollaboration;

    @Resource
    private BaseMapper<CustomerCollaboration> customerCollaborationMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testListEmpty() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, "customerId");
        List<CustomerCollaborationListResponse> customerCollaborations = getResultDataArray(mvcResult, CustomerCollaborationListResponse.class);
        Assertions.assertTrue(CollectionUtils.isEmpty(customerCollaborations));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, "customerId");
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        CustomerCollaborationAddRequest request = new CustomerCollaborationAddRequest();
        request.setCollaborationType(CustomerCollaborationType.COLLABORATION.name());
        request.setCustomerId("customerId");
        request.setUserId(InternalUser.ADMIN.getValue());
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomerCollaboration resultData = getResultData(mvcResult, CustomerCollaboration.class);
        CustomerCollaboration customerCollaboration = customerCollaborationMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addCustomerCollaboration = customerCollaboration;
        Assertions.assertEquals(request.getCustomerId(), "customerId");
        Assertions.assertEquals(request.getCollaborationType(), customerCollaboration.getCollaborationType());
        Assertions.assertEquals(request.getUserId(), customerCollaboration.getUserId());
        
        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        CustomerCollaborationUpdateRequest request = new CustomerCollaborationUpdateRequest();
        request.setId(addCustomerCollaboration.getId());
        request.setCollaborationType(CustomerCollaborationType.READ_ONLY.name());
        this.requestPostWithOk(DEFAULT_UPDATE, request);

        // 校验请求成功数据
        CustomerCollaboration customerCollaboration = customerCollaborationMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getCollaborationType(), customerCollaboration.getCollaborationType());
        Assertions.assertEquals(addCustomerCollaboration.getCustomerId(), "customerId");
        Assertions.assertEquals(addCustomerCollaboration.getUserId(), customerCollaboration.getUserId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, "customerId");
        List<CustomerCollaborationListResponse> list = getResultDataArray(mvcResult, CustomerCollaborationListResponse.class);
        list.forEach(customerCollaborationListResponse -> {
            CustomerCollaboration customerCollaboration = customerCollaborationMapper.selectByPrimaryKey(customerCollaborationListResponse.getId());
            CustomerCollaboration result = BeanUtils.copyBean(new CustomerCollaboration(), customerCollaborationListResponse);
            Assertions.assertEquals(customerCollaboration, result);
            Assertions.assertNotNull(customerCollaborationListResponse.getUpdateUserName());
        });
        
        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, "customerId");
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addCustomerCollaboration.getId());
        CustomerCollaboration customerCollaboration = customerCollaborationMapper.selectByPrimaryKey(addCustomerCollaboration.getId());
        Assertions.assertNull(customerCollaboration);
        // 校验权限
        requestGetPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE),
                DEFAULT_DELETE, addCustomerCollaboration.getId());
    }

    @Test
    @Order(10)
    void batchDelete() throws Exception {
        this.requestPostWithOk(DEFAULT_BATCH_DELETE, List.of(addCustomerCollaboration.getId()));
        CustomerCollaboration customerCollaboration = customerCollaborationMapper.selectByPrimaryKey(addCustomerCollaboration.getId());
        Assertions.assertNull(customerCollaboration);
        // 校验权限
        requestPostPermissionsTest(List.of(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE),
                DEFAULT_BATCH_DELETE, List.of(addCustomerCollaboration.getId()));
    }
}