package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.uid.IDGenerator;
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
import cn.cordys.crm.system.domain.Role;
import cn.cordys.crm.system.domain.RolePermission;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerRelationControllerTests extends BaseTest {
    protected static final String LIST = "list/{0}";
    protected static final String SAVE = "save/{0}";
    protected static final String ADD = "add/{0}";
    protected static final String UPDATE = "update/{0}";
    protected static final String DELETE = "delete/{0}";
    private static final String BASE_PATH = "/account/relation/";
    private static Customer addCustomer;
    private static CustomerRelation addCustomerRelation;
    private static Customer anotherAddCustomer;

    @Resource
    private CustomerRelationService customerRelationService;

    @Resource
    private CustomerService customerService;

    @Resource
    private BaseMapper<CustomerRelation> customerRelationBaseMapper;

    @Resource
    private BaseMapper<Role> roleMapper;

    @Resource
    private BaseMapper<RolePermission> rolePermissionMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testListEmpty() throws Exception {
        CustomerAddRequest request = new CustomerAddRequest();
        request.setName("empty-relation-test");
        request.setOwner(InternalUser.ADMIN.getValue());
        var customer = customerService.add(request, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);
        MvcResult mvcResult = this.requestGetWithOkAndReturn(LIST, customer.getId());
        List<CustomerRelationListResponse> list = getResultDataArray(mvcResult, CustomerRelationListResponse.class);
        Assertions.assertTrue(CollectionUtils.isEmpty(list));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_READ, LIST, customer.getId());
    }

    @Test
    @Order(1)
    void testSave() throws Exception {
        CustomerAddRequest customerAddRequest = new CustomerAddRequest();
        customerAddRequest.setName(UUID.randomUUID().toString());
        customerAddRequest.setOwner(InternalUser.ADMIN.getValue());
        addCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(), DEFAULT_ORGANIZATION_ID);

        customerAddRequest.setName(UUID.randomUUID().toString());
        anotherAddCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(),
                DEFAULT_ORGANIZATION_ID);

        // 校验权限 放前面，否则会删掉创建的关系（客户需已存在，否则数据权限校验会直接拒绝）
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, SAVE, List.of(), addCustomer.getId());

        List<CustomerRelationSaveRequest> requests = List.of(new CustomerRelationSaveRequest());
        requestPostWithOk(SAVE, requests, addCustomer.getId());

        requests = List.of(
                new CustomerRelationSaveRequest(anotherAddCustomer.getId(), CustomerRelationType.SUBSIDIARY.name()));
        requestPostWithOk(SAVE, requests, addCustomer.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, SAVE, requests, addCustomer.getId());
    }

    @Test
    @Order(2)
    void testSaveRejectsSelfScopeOtherOwner() throws Exception {
        CustomerAddRequest customerAddRequest = new CustomerAddRequest();
        customerAddRequest.setName(UUID.randomUUID().toString());
        customerAddRequest.setOwner(InternalUser.ADMIN.getValue());
        Customer otherOwnerCustomer = customerService.add(customerAddRequest, InternalUser.ADMIN.getValue(),
                DEFAULT_ORGANIZATION_ID);

        customerRelationService.save(otherOwnerCustomer.getId(),
                List.of(new CustomerRelationSaveRequest(anotherAddCustomer.getId(),
                        CustomerRelationType.SUBSIDIARY.name())));
        CustomerRelation example = new CustomerRelation();
        example.setSourceCustomerId(otherOwnerCustomer.getId());
        CustomerRelation existingRelation = customerRelationBaseMapper.selectOne(example);
        Assertions.assertNotNull(existingRelation);

        Role role = roleMapper.selectByPrimaryKey(PERMISSION_USER_NAME);
        String originDataScope = role.getDataScope();
        role.setDataScope(RoleDataScope.SELF.name());
        roleMapper.update(role);

        RolePermission rolePermission = new RolePermission();
        rolePermission.setId(IDGenerator.nextStr());
        rolePermission.setRoleId(PERMISSION_USER_NAME);
        rolePermission.setPermissionId(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE);
        rolePermissionMapper.insert(rolePermission);
        permissionCache.clearCache(PERMISSION_USER_NAME, DEFAULT_ORGANIZATION_ID);

        try {
            // 仅本人数据权限不能整体覆盖其他负责人的客户关系
            int saveStatus = mockMvc
                    .perform(getPermissionPostRequestBuilder(SAVE, List.of(), otherOwnerCustomer.getId()))
                    .andReturn().getResponse().getStatus();
            Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), saveStatus);

            // 仅本人数据权限不能向其他负责人的客户添加关系
            CustomerRelationSaveRequest addRequest = new CustomerRelationSaveRequest(anotherAddCustomer.getId(),
                    CustomerRelationType.SUBSIDIARY.name());
            int addStatus = mockMvc
                    .perform(getPermissionPostRequestBuilder(ADD, addRequest, otherOwnerCustomer.getId()))
                    .andReturn().getResponse().getStatus();
            Assertions.assertEquals(HttpStatus.FORBIDDEN.value(), addStatus);

            Assertions.assertNotNull(customerRelationBaseMapper.selectByPrimaryKey(existingRelation.getId()));
        } finally {
            rolePermissionMapper.deleteByPrimaryKey(rolePermission.getId());
            role.setDataScope(originDataScope);
            roleMapper.update(role);
            permissionCache.clearCache(PERMISSION_USER_NAME, DEFAULT_ORGANIZATION_ID);
        }
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
        CustomerRelationSaveRequest customerRelationSaveRequest = new CustomerRelationSaveRequest(
                anotherAddCustomer.getId(), CustomerRelationType.SUBSIDIARY.name());

        MvcResult mvcResult = requestPostWithOkAndReturn(ADD, customerRelationSaveRequest, addCustomer.getId());
        addCustomerRelation = getResultData(mvcResult, CustomerRelation.class);
        addCustomerRelation = customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId());
        Assertions.assertEquals(addCustomerRelation.getSourceCustomerId(), addCustomer.getId());
        Assertions.assertEquals(addCustomerRelation.getTargetCustomerId(), anotherAddCustomer.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, ADD, customerRelationSaveRequest,
                addCustomer.getId());
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
        requestPostPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, UPDATE, customerRelationUpdateRequest,
                addCustomer.getId());
    }

    @Test
    @Order(6)
    void testAddAndUpdateWithoutRequiredFields() throws Exception {
        CustomerRelation example = new CustomerRelation();
        example.setSourceCustomerId(addCustomer.getId());
        example.setTargetCustomerId(anotherAddCustomer.getId());
        Long relationCount = customerRelationBaseMapper.countByExample(example);

        // 不传关联客户：应返回参数校验失败，而不是落入数据库非空约束导致 500
        CustomerRelationSaveRequest withoutCustomer = new CustomerRelationSaveRequest(null,
                CustomerRelationType.SUBSIDIARY.name());
        MvcResult result = requestPost(ADD, withoutCustomer, addCustomer.getId()).andReturn();
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Map<?, ?> response = parseResponse(result);
        Assertions.assertEquals(CrmHttpResultCode.VALIDATE_FAILED.getCode(), response.get("code"));
        Assertions.assertTrue(((Map<?, ?>) response.get("messageDetail")).containsKey("customerId"));

        // 不传关系类型
        CustomerRelationSaveRequest withoutType = new CustomerRelationSaveRequest(anotherAddCustomer.getId(), null);
        result = requestPost(ADD, withoutType, addCustomer.getId()).andReturn();
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        response = parseResponse(result);
        Assertions.assertEquals(CrmHttpResultCode.VALIDATE_FAILED.getCode(), response.get("code"));
        Assertions.assertTrue(((Map<?, ?>) response.get("messageDetail")).containsKey("relationType"));

        // 更新时同样校验关联客户
        CustomerRelationUpdateRequest updateRequest = new CustomerRelationUpdateRequest();
        updateRequest.setId(addCustomerRelation.getId());
        updateRequest.setRelationType(CustomerRelationType.SUBSIDIARY.name());
        result = requestPost(UPDATE, updateRequest, addCustomer.getId()).andReturn();
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        response = parseResponse(result);
        Assertions.assertEquals(CrmHttpResultCode.VALIDATE_FAILED.getCode(), response.get("code"));
        Assertions.assertTrue(((Map<?, ?>) response.get("messageDetail")).containsKey("customerId"));

        // 校验失败时不应写入关系记录
        Assertions.assertEquals(relationCount, customerRelationBaseMapper.countByExample(example));
    }

    @Test
    @Order(7)
    void testUpdateTargetCustomerAlreadyInGroup() throws Exception {
        // addCustomer 已是 anotherAddCustomer 的子公司，再把关系改为子公司应被业务规则拒绝
        CustomerRelationUpdateRequest request = new CustomerRelationUpdateRequest();
        request.setId(addCustomerRelation.getId());
        request.setCustomerId(anotherAddCustomer.getId());
        request.setRelationType(CustomerRelationType.SUBSIDIARY.name());

        MvcResult result = requestPost(UPDATE, request, addCustomer.getId()).andReturn();
        Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        Map<?, ?> response = parseResponse(result);
        Assertions.assertEquals(CrmHttpResultCode.VALIDATE_FAILED.getCode(), response.get("code"));
        String messageDetail = String.valueOf(response.get("messageDetail"));
        Assertions.assertTrue(messageDetail.contains(anotherAddCustomer.getName()));

        // 关系未被修改
        CustomerRelation relation = customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId());
        Assertions.assertEquals(anotherAddCustomer.getId(), relation.getSourceCustomerId());
        Assertions.assertEquals(addCustomer.getId(), relation.getTargetCustomerId());
    }

    @Test
    @Order(9)
    void delete() throws Exception {
        requestGetPermissionTest(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, DELETE, addCustomerRelation.getId());

        Assertions.assertNull(customerRelationBaseMapper.selectByPrimaryKey(addCustomerRelation.getId()));
    }

    @Test
    @Order(10)
    void batchDelete() {
        customerRelationService.deleteByCustomerId(addCustomer.getId());
        List<CustomerRelationListResponse> list = customerRelationService.list(addCustomer.getId());
        Assertions.assertTrue(list.isEmpty());
    }
}
