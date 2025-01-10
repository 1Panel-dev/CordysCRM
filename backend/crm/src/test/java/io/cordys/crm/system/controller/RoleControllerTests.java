package io.cordys.crm.system.controller;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.permission.Permission;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.Role;
import io.cordys.crm.system.domain.RolePermission;
import io.cordys.crm.system.dto.request.PermissionSettingUpdateRequest;
import io.cordys.crm.system.dto.request.RoleAddRequest;
import io.cordys.crm.system.dto.request.RoleUpdateRequest;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleControllerTests extends BaseTest {
    private static final String BASE_PATH = "/role/";
    private static final String PERMISSION_SETTING = "permission/setting/{0}";
    private static final String PERMISSION_UPDATE = "permission/update";

    /**
     * 记录创建的角色
     */
    private static Role addRole;

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
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOk(DEFAULT_LIST)
                .andReturn();
        List<RoleListResponse> roleList = getResultDataArray(mvcResult, RoleListResponse.class);

        Map<String, RoleListResponse> roleMap = roleList.stream()
                .collect(Collectors.toMap(RoleListResponse::getId, Function.identity()));

        // 校验内置用户
        assertInternalRole(roleMap.get(InternalRole.ORG_ADMIN.getValue()));
        assertInternalRole(roleMap.get(InternalRole.SALES_STAFF.getValue()));
        assertInternalRole(roleMap.get(InternalRole.SALES_MANAGER.getValue()));

        // 校验组织ID
        roleList.forEach(role -> Assertions.assertEquals(role.getOrganizationId(), DEFAULT_ORGANIZATION_ID));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, DEFAULT_LIST);
    }

    private static void assertInternalRole(RoleListResponse role) {
        Assertions.assertEquals(role.getName(), Translator.get("role." + role.getId()));
        Assertions.assertEquals(role.getCreateUserName(), "Administrator");
        Assertions.assertEquals(role.getUpdateUserName(), "Administrator");
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        // 请求成功
        RoleAddRequest request = new RoleAddRequest();
        request.setName("test");
        request.setDescription("test desc");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Role resultData = getResultData(mvcResult, Role.class);
        Role role = roleMapper.selectByPrimaryKey(resultData.getId());

        // 校验请求成功数据
        this.addRole = role;
        Assertions.assertEquals(request.getName(), role.getName());
        Assertions.assertEquals(request.getDescription(), role.getDescription());
        Assertions.assertEquals(role.getOrganizationId(), DEFAULT_ORGANIZATION_ID);

        // 校验权限
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
        MvcResult mvcResult = this.requestGetWithOk(DEFAULT_LIST)
                .andReturn();
        List<RoleListResponse> userRoles = getResultDataArray(mvcResult, RoleListResponse.class);

        // 校验组织ID
        userRoles.forEach(role -> Assertions.assertEquals(role.getOrganizationId(), DEFAULT_ORGANIZATION_ID));

        // @@校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, DEFAULT_LIST);
    }

    @Test
    @Order(4)
    void getPermissionSetting() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(PERMISSION_SETTING, addRole.getId());
        List<PermissionDefinitionItem> permissionDefinition = getResultDataArray(mvcResult, PermissionDefinitionItem.class);

        // 获取该用户组拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(InternalRole.ORG_ADMIN.getValue());
        // 设置勾选项
        permissionDefinition.forEach(firstLevel -> {
            List<PermissionDefinitionItem> children = firstLevel.getChildren();
            boolean allCheck = true;
            for (PermissionDefinitionItem secondLevel : children) {
                List<Permission> permissions = secondLevel.getPermissions();
                if (org.apache.commons.collections.CollectionUtils.isEmpty(permissions)) {
                    continue;
                }
                boolean secondAllCheck = true;
                for (Permission p : permissions) {
                    if (permissionIds.contains(p.getId())) {
                        // 如果有权限这里校验开启
                        Assertions.assertTrue(p.getEnable());
                        // 使用完移除
                        permissionIds.remove(p.getId());
                    } else {
                        // 如果没有权限校验关闭
                        secondAllCheck = false;
                    }
                }
                if (!secondAllCheck) {
                    // 如果二级菜单有未勾选，则一级菜单设置为未勾选
                    allCheck = false;
                }
            }
            Assertions.assertEquals(allCheck, firstLevel.getEnable());
        });
        // 校验是不是获取的数据中包含了该用户组所有的权限
        Assertions.assertTrue(CollectionUtils.isEmpty(permissionIds));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, PERMISSION_SETTING, addRole.getId());
    }

    @Test
    @Order(5)
    void updatePermissionSetting() throws Exception {
        PermissionSettingUpdateRequest request = new PermissionSettingUpdateRequest();
        request.setPermissions(new ArrayList<>() {{
            PermissionSettingUpdateRequest.PermissionUpdateRequest permission1
                    = new PermissionSettingUpdateRequest.PermissionUpdateRequest();
            permission1.setEnable(true);
            permission1.setId(PermissionConstants.SYSTEM_ROLE_READ);
            add(permission1);
            PermissionSettingUpdateRequest.PermissionUpdateRequest permission2
                    = new PermissionSettingUpdateRequest.PermissionUpdateRequest();
            permission2.setEnable(false);
            permission2.setId(PermissionConstants.SYSTEM_ROLE_UPDATE);
            add(permission2);
        }});

        // 请求成功
        request.setRoleId(addRole.getId());
        this.requestPostWithOk(PERMISSION_UPDATE, request);
        // 获取该用户组拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(request.getRoleId());
        Set<String> requestPermissionIds = request.getPermissions().stream()
                .filter(PermissionSettingUpdateRequest.PermissionUpdateRequest::getEnable)
                .map(PermissionSettingUpdateRequest.PermissionUpdateRequest::getId)
                .collect(Collectors.toSet());
        // 校验请求成功数据
        Assertions.assertEquals(requestPermissionIds, permissionIds);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_UPDATE, PERMISSION_UPDATE, request);
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        // 请求成功
        this.requestGetWithOk(DEFAULT_DELETE, addRole.getId());

        // 校验请求成功数据
        Assertions.assertNull(roleMapper.selectByPrimaryKey(addRole.getId()));
        // 校验角色与权限的关联关系是否删除
        Assertions.assertTrue(CollectionUtils.isEmpty(getByRoleId(addRole.getId())));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_DELETE, DEFAULT_DELETE, addRole.getId());
    }

    public List<RolePermission> getByRoleId(String roleId) {
        RolePermission example = new RolePermission();
        example.setRoleId(roleId);
        return rolePermissionMapper.select(example);
    }

    /**
     * 查询角色对应的权限ID
     *
     * @param roleId
     * @return
     */
    public Set<String> getPermissionIdSetByRoleId(String roleId) {
        return getByRoleId(roleId).stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }
}