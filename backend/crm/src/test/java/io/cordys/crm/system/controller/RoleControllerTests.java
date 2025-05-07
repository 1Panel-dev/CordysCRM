package io.cordys.crm.system.controller;

import io.cordys.common.constants.InternalRole;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.constants.RoleDataScope;
import io.cordys.common.dto.DeptUserTreeNode;
import io.cordys.common.dto.RoleUserTreeNode;
import io.cordys.common.permission.Permission;
import io.cordys.common.permission.PermissionDefinitionItem;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.*;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.RoleGetResponse;
import io.cordys.crm.system.dto.response.RoleListResponse;
import io.cordys.crm.system.dto.response.RoleUserListResponse;
import io.cordys.crm.system.dto.response.RoleUserOptionResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.cordys.crm.system.constants.SystemResultCode.INTERNAL_ROLE_PERMISSION;
import static io.cordys.crm.system.constants.SystemResultCode.ROLE_EXIST;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoleControllerTests extends BaseTest {
    private static final String BASE_PATH = "/role/";
    private static final String PERMISSION_SETTING = "permission/setting";
    private static final String USER_PAGE = "user/page";
    private static final String DEPT_TREE = "dept/tree";
    private static final String USER_DEPT_TREE = "user/dept/tree/{roleId}";
    private static final String USER_ROLE_TREE = "user/role/tree/{roleId}";
    private static final String USER_OPTION = "user/option/{roleId}";
    private static final String USER_RELATE = "user/relate";
    private static final String USER_DELETE = "user/delete/{id}";
    private static final String USER_BATCH_DELETE = "user/batch/delete";

    /**
     * 记录创建的角色
     */
    private static Role addRole;
    private static Role anotherUserRole;

    @Resource
    private BaseMapper<Role> roleMapper;
    @Resource
    private BaseMapper<RolePermission> rolePermissionMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private BaseMapper<RoleScopeDept> roleScopeDeptMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;

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

        // 校验重名异常
        assertErrorCode(this.requestPost(DEFAULT_ADD, request), ROLE_EXIST);

        // 添加另一条数据，配置权限
        request.setName("other name");
        request.setDataScope(RoleDataScope.DEPT_CUSTOM.name());
        request.setDeptIds(List.of("deptId"));
        request.setPermissions(new ArrayList<>() {{
            PermissionUpdateRequest permission1
                    = new PermissionUpdateRequest();
            permission1.setEnable(true);
            permission1.setId(PermissionConstants.SYSTEM_ROLE_READ);
            add(permission1);
            PermissionUpdateRequest permission2
                    = new PermissionUpdateRequest();
            permission2.setEnable(false);
            permission2.setId(PermissionConstants.SYSTEM_ROLE_UPDATE);
            add(permission2);
        }});
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        anotherUserRole = roleMapper.selectByPrimaryKey(getResultData(mvcResult, Role.class).getId());
        // 校验组织
        RoleScopeDept example = new RoleScopeDept();
        example.setDepartmentId("deptId");
        example.setRoleId(anotherUserRole.getId());
        Assertions.assertTrue(roleScopeDeptMapper.select(example).size() > 0);

        // 获取该用户组拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(anotherUserRole.getId());
        Set<String> requestPermissionIds = request.getPermissions().stream()
                .filter(PermissionUpdateRequest::getEnable)
                .map(PermissionUpdateRequest::getId)
                .collect(Collectors.toSet());
        // 校验请求成功数据
        Assertions.assertEquals(requestPermissionIds, permissionIds);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(2)
    void testUpdate() throws Exception {
        // 请求成功
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setId(addRole.getId());
        request.setName("test update");
        request.setDescription("test desc !!!!");
        this.requestPostWithOk(DEFAULT_UPDATE, request);

        // 校验请求成功数据
        Role userRoleResult = roleMapper.selectByPrimaryKey(request.getId());
        Assertions.assertEquals(request.getName(), userRoleResult.getName());
        Assertions.assertEquals(request.getDescription(), userRoleResult.getDescription());

        // 修改权限
        request.setPermissions(new ArrayList<>() {{
            PermissionUpdateRequest permission1
                    = new PermissionUpdateRequest();
            permission1.setEnable(true);
            permission1.setId(PermissionConstants.SYSTEM_ROLE_READ);
            add(permission1);
            PermissionUpdateRequest permission2
                    = new PermissionUpdateRequest();
            permission2.setEnable(false);
            permission2.setId(PermissionConstants.SYSTEM_ROLE_UPDATE);
            add(permission2);
        }});
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 获取该用户组拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(request.getId());
        Set<String> requestPermissionIds = request.getPermissions().stream()
                .filter(PermissionUpdateRequest::getEnable)
                .map(PermissionUpdateRequest::getId)
                .collect(Collectors.toSet());
        // 校验请求成功数据
        Assertions.assertEquals(requestPermissionIds, permissionIds);

        // 不修改信息
        RoleUpdateRequest emptyRequest = new RoleUpdateRequest();
        emptyRequest.setId(addRole.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, emptyRequest);

        // 校验重名异常
        request.setId(addRole.getId());
        request.setName(anotherUserRole.getName());
        assertErrorCode(this.requestPost(DEFAULT_UPDATE, request), ROLE_EXIST);

        // 校验内置不能修改名称
        request.setId(InternalRole.ORG_ADMIN.getValue());
        request.setName("test");
        this.requestPost(DEFAULT_UPDATE, request);
        Assertions.assertEquals(roleMapper.selectByPrimaryKey(InternalRole.ORG_ADMIN.getValue()).getName(), InternalRole.ORG_ADMIN.getValue());

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
    void testGetPermissionSetting() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(PERMISSION_SETTING);
        List<PermissionDefinitionItem> permissionDefinition = getResultDataArray(mvcResult, PermissionDefinitionItem.class);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(permissionDefinition));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, PERMISSION_SETTING);
    }

    @Test
    @Order(4)
    void testGet() throws Exception {
        // @@请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addRole.getId());
        RoleGetResponse getResponse = getResultData(mvcResult, RoleGetResponse.class);
        List<PermissionDefinitionItem> permissionDefinition = getResponse.getPermissions();

        Role role = roleMapper.selectByPrimaryKey(addRole.getId());
        Assertions.assertEquals(role, BeanUtils.copyBean(new Role(), getResponse));

        // 获取该用户组拥有的权限
        Set<String> permissionIds = getPermissionIdSetByRoleId(addRole.getId());
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
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, DEFAULT_GET, addRole.getId());
    }

    @Test
    @Order(6)
    void testUserPage() throws Exception {
        RoleUserPageRequest request = new RoleUserPageRequest();
        request.setRoleId(addRole.getId());
        request.setCurrent(1);
        request.setPageSize(500);
        // 请求成功
        MvcResult mvcResult = this.requestPostWithOkAndReturn(USER_PAGE, request);
        List<RoleUserListResponse> pageResult = getPageResult(mvcResult, RoleUserListResponse.class).getList();

        // 校验数据
        UserRole example = new UserRole();
        example.setRoleId(addRole.getId());
        List<UserRole> userRoles = userRoleMapper.select(example);
        Set<String> userIdSet = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
        Assertions.assertEquals(pageResult.size(), userRoles.size());
        pageResult.forEach(user -> {
            Assertions.assertTrue(userIdSet.contains(user.getUserId()));
        });

        // @@校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, USER_PAGE, request);
    }

    @Test
    @Order(7)
    void testUserRelate() throws Exception {
        // 请求成功
        RoleUserRelateRequest request = new RoleUserRelateRequest();
        request.setRoleId(addRole.getId());
        request.setUserIds(List.of(InternalUser.ADMIN.getValue()));
        this.requestPostWithOk(USER_RELATE, request);

        // 校验数据
        UserRole userRole = new UserRole();
        userRole.setRoleId(addRole.getId());
        userRole.setUserId(InternalUser.ADMIN.getValue());
        Assertions.assertFalse(CollectionUtils.isEmpty(userRoleMapper.select(userRole)));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD_USER, USER_RELATE, request);
    }

    @Test
    @Order(8)
    void testUserDeptTree() throws Exception {
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(USER_DEPT_TREE, addRole.getId());
        List<DeptUserTreeNode> deptUserTreeNodes = getResultDataArray(mvcResult, DeptUserTreeNode.class);

        // 校验数据
        deptUserTreeNodes.forEach(deptUserTreeNode -> {
            if (StringUtils.equals(deptUserTreeNode.getNodeType(), "ORG")) {
                OrganizationUser organizationUser = new OrganizationUser();
                organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
                organizationUser.setDepartmentId(deptUserTreeNode.getId());
                List<OrganizationUser> organizationUsers = organizationUserMapper.select(organizationUser);
                organizationUsers.forEach(user -> {
                    Assertions.assertTrue(deptUserTreeNode.getChildren().stream()
                            .anyMatch(child -> StringUtils.equals(child.getId(), user.getUserId())));
                });
            }
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD_USER, USER_DEPT_TREE, addRole.getId());
    }

    @Test
    @Order(8)
    void testDeptTree() throws Exception {
        // 请求成功
        this.requestGetWithOkAndReturn(DEPT_TREE);
        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_READ, DEPT_TREE);
    }

    @Test
    @Order(9)
    void testUserRoleTree() throws Exception {
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(USER_ROLE_TREE, addRole.getId());
        List<RoleUserTreeNode> deptUserTreeNodes = getResultDataArray(mvcResult, RoleUserTreeNode.class);

        // 校验数据
        deptUserTreeNodes.forEach(roleNode -> {
            if (StringUtils.equals(roleNode.getNodeType(), "ROLE")) {
                UserRole userRole = new UserRole();
                userRole.setRoleId(roleNode.getId());
                if (userRole.getRoleId().equals(PERMISSION_USER_NAME)) {
                    return;
                }
                if (StringUtils.equalsAny(roleNode.getId(),
                        InternalRole.ORG_ADMIN.getValue(),
                        InternalRole.SALES_MANAGER.getValue(),
                        InternalRole.SALES_STAFF.getValue())) {
                    Assertions.assertTrue(roleNode.getInternal());
                }
                List<UserRole> userRoles = userRoleMapper.select(userRole);
                userRoles.forEach(user -> {
                    Assertions.assertTrue(roleNode.getChildren().stream()
                            .anyMatch(child -> StringUtils.equals(child.getId(), user.getUserId())));
                });
            }
        });

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD_USER, USER_ROLE_TREE, addRole.getId());
    }

    @Test
    @Order(9)
    void testUserOption() throws Exception {
        // 请求成功
        MvcResult mvcResult = this.requestGetWithOkAndReturn(USER_OPTION, addRole.getId());
        List<RoleUserOptionResponse> userOptionResponses = getResultDataArray(mvcResult, RoleUserOptionResponse.class);

        OrganizationUser example = new OrganizationUser();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setEnable(true);
        List<OrganizationUser> orgUsers = organizationUserMapper.select(example);
        // 校验数据
        Assertions.assertTrue(!orgUsers.isEmpty() && !userOptionResponses.isEmpty());

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_ADD_USER, USER_ROLE_TREE, addRole.getId());
    }

    @Test
    @Order(10)
    void testRoleUserDelete() throws Exception {
        UserRole example = new UserRole();
        example.setRoleId(addRole.getId());
        String id = userRoleMapper.select(example).getFirst().getId();

        // 请求成功
        this.requestGetWithOk(USER_DELETE, id);

        // 校验请求成功数据
        Assertions.assertNull(userRoleMapper.selectByPrimaryKey(id));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_REMOVE_USER, USER_DELETE, id);
    }

    @Test
    @Order(11)
    void testRoleUserBatchDelete() throws Exception {
        // 先关联，再删除
        RoleUserRelateRequest request = new RoleUserRelateRequest();
        request.setRoleId(addRole.getId());
        request.setUserIds(List.of(InternalUser.ADMIN.getValue()));
        this.requestPostWithOk(USER_RELATE, request);

        UserRole example = new UserRole();
        example.setRoleId(addRole.getId());
        List<String> ids = userRoleMapper.select(example)
                .stream()
                .map(UserRole::getId)
                .toList();

        // 请求成功
        this.requestPostWithOk(USER_BATCH_DELETE, ids);

        // 校验请求成功数据
        ids.forEach(id -> Assertions.assertNull(userRoleMapper.selectByPrimaryKey(id)));

        // 校验权限
        requestPostPermissionTest(PermissionConstants.SYSTEM_ROLE_REMOVE_USER, USER_BATCH_DELETE, ids);
    }

    @Test
    @Order(20)
    void testDelete() throws Exception {
        // 请求成功
        this.requestGetWithOk(DEFAULT_DELETE, addRole.getId());

        // 校验请求成功数据
        Assertions.assertNull(roleMapper.selectByPrimaryKey(addRole.getId()));

        // 校验角色与权限的关联关系是否删除
        Assertions.assertTrue(CollectionUtils.isEmpty(getByRoleId(addRole.getId())));

        // 校验角色与部门的关联关系是否删除
        Assertions.assertTrue(CollectionUtils.isEmpty(getRoleScopeDeptByRoleId(addRole.getId())));

        // 操作内置角色异常
        assertErrorCode(this.requestGet(DEFAULT_DELETE, InternalRole.SALES_MANAGER.getValue()), INTERNAL_ROLE_PERMISSION);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.SYSTEM_ROLE_DELETE, DEFAULT_DELETE, addRole.getId());
    }

    public List<RolePermission> getByRoleId(String roleId) {
        RolePermission example = new RolePermission();
        example.setRoleId(roleId);
        return rolePermissionMapper.select(example);
    }

    public List<RoleScopeDept> getRoleScopeDeptByRoleId(String roleId) {
        RoleScopeDept example = new RoleScopeDept();
        example.setRoleId(roleId);
        return roleScopeDeptMapper.select(example);
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