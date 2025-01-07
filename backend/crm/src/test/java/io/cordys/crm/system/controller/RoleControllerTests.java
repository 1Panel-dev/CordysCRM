package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.dto.request.RoleAddRequest;
import io.cordys.crm.system.dto.request.RoleUpdateRequest;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleControllerTests extends BaseTest {
    private static final String BASE_PATH = "/role/";

    protected static final String LIST = "list/{0}";
    private static final String PERMISSION_SETTING = "permission/setting/{0}";

    /**
     * 记录创建的角色
     */
    private static Role addRole;

    @Resource
    private BaseMapper<Role> roleMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testListEmpty() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOk(LIST, DEFAULT_ORGANIZATION_ID)
                .andReturn();
        List<RoleListResponse> roleList = getResultDataArray(mvcResult, RoleListResponse.class);

        Assertions.assertTrue(CollectionUtils.isEmpty(roleList));

        // @@校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, LIST, DEFAULT_ORGANIZATION_ID);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // @@请求成功
        RoleAddRequest request = new RoleAddRequest();
        request.setName("test");
        request.setDescription("test desc");
        request.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Role resultData = getResultData(mvcResult, Role.class);
        Role role = roleMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addRole = role;
        Assertions.assertEquals(request.getName(), role.getName());
        Assertions.assertEquals(request.getDescription(), role.getDescription());

        // @@校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // @@请求成功
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setId(addRole.getId());
        request.setName("test update");
        request.setDescription("test desc !!!!");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验请求成功数据
        Role userRoleResult = roleMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), userRoleResult.getName());
        Assertions.assertEquals(request.getDescription(), userRoleResult.getDescription());

        // 不修改信息
        RoleUpdateRequest emptyRequest = new RoleUpdateRequest();
        emptyRequest.setId(addRole.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // @@校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(3)
    void testList() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOk(LIST, DEFAULT_ORGANIZATION_ID)
                .andReturn();
        List<RoleListResponse> userRoles = getResultDataArray(mvcResult, RoleListResponse.class);


        // @@校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, LIST, DEFAULT_ORGANIZATION_ID);
    }

    @Test
    @Order(4)
    void getPermissionSetting() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(PERMISSION_SETTING, addRole.getId());
        List<PermissionDefinitionItem> permissionDefinition = getResultDataArray(mvcResult, PermissionDefinitionItem.class);
        // todo
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, PERMISSION_SETTING, addRole.getId());
    }

    @Test
    @Order(5)
    void delete() throws Exception {
        // @@请求成功
        this.requestGetWithOk(DEFAULT_DELETE, addRole.getId());
        // todo
        // @@校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_DELETE, DEFAULT_DELETE, addRole.getId());
    }
}