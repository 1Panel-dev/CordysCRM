package cn.cordys.crm.form.controller;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import cn.cordys.crm.system.domain.Department;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.Role;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.domain.UserRole;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomFormControllerTests extends BaseTest {

    private static final String BASE_PATH = "/custom-form/";
    private static final String OPTION = "option";
    private static final String ADMIN_GET = "admin/get/{0}";
    private static final String ROLE_USERS = "role/users";
    private static String createdFormId;

    @Resource
    private BaseMapper<CustomFormAdmin> customFormAdminMapper;
    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<UserRole> userRoleMapper;
    @Resource
    private BaseMapper<User> userMapper;
    @Resource
    private BaseMapper<Department> departmentMapper;
    @Resource
    private BaseMapper<Role> roleMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(1)
    void testList() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_LIST);
        List<CustomFormListResponse> list = getResultDataArray(mvcResult, CustomFormListResponse.class);
        assertNotNull(list);
    }

    @Test
    @Order(2)
    void testCreate() throws Exception {
        CustomFormAddRequest request = new CustomFormAddRequest();
        request.setName("测试自定义表单");
        request.setEnable(true);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomForm form = getResultData(mvcResult, CustomForm.class);
        assertNotNull(form);
        assertNotNull(form.getId());
        assertEquals("测试自定义表单", form.getName());

        createdFormId = form.getId();
    }

    @Test
    @Order(3)
    void testEnabledOptions() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(OPTION);
        List<OptionDTO> list = getResultDataArray(mvcResult, OptionDTO.class);
        assertNotNull(list);
        assertTrue(list.stream().anyMatch(o -> createdFormId.equals(o.getId())),
                "已开启的表单应出现在选项列表中");
    }

    @Test
    @Order(4)
    void testGet() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, createdFormId);
        CustomFormGetResponse response = getResultData(mvcResult, CustomFormGetResponse.class);
        assertNotNull(response);
        assertEquals(createdFormId, response.getId());
        assertEquals("测试自定义表单", response.getName());
    }

    @Test
    @Order(5)
    void testUpdate() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        CustomFormUpdateRequest request = new CustomFormUpdateRequest();
        request.setId(createdFormId);
        request.setName("更新后的表单名称");
        request.setEnable(false);
        request.setFormProp(new FormProp());

        this.requestPostWithOk(DEFAULT_UPDATE, request);

        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, createdFormId);
        CustomFormGetResponse response = getResultData(mvcResult, CustomFormGetResponse.class);
        assertEquals("更新后的表单名称", response.getName());
        assertEquals(false, response.getEnable());
    }

    @Test
    @Order(6)
    void testSetAdmins() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        MvcResult adminResult = this.requestGetWithOkAndReturn(ADMIN_GET, createdFormId);
        List<OptionDTO> admins = getResultDataArray(adminResult, OptionDTO.class);
        assertNotNull(admins);
        assertTrue(admins.stream().anyMatch(admin -> "admin".equals(admin.getId())));

        CustomFormAdminBatchRequest request = new CustomFormAdminBatchRequest();
        request.setCustomFormId(createdFormId);
        request.setUserIds(List.of("test-admin-user", "test-admin-user-2"));

        this.requestPostWithOk("/admin/set", request);
        assertAdminUsers("test-admin-user", "test-admin-user-2");

        request.setUserIds(List.of("test-admin-user-3"));
        this.requestPostWithOk("/admin/set", request);
        assertAdminUsers("test-admin-user-3");
    }

    @Test
    @Order(7)
    void testAddRoleUsersByUserDeptAndRole() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        String roleId = getFirstCustomFormRoleId();
        prepareUserDeptAndRoleData();

        CustomFormRoleUserBatchRequest request = new CustomFormRoleUserBatchRequest();
        request.setCustomFormRoleId(roleId);
        request.setUserIds(List.of("cf-role-direct-user"));
        request.setDeptIds(List.of("cf-role-test-dept"));
        request.setRoleIds(List.of("cf-role-test-system-role"));

        this.requestPostWithOk("role/user/add", request);

        assertRoleUsers(roleId, "cf-role-direct-user", "cf-role-dept-user", "cf-role-system-role-user");
        assertRoleUsersPage(roleId, 1, 3, 3);
    }

    @Test
    @Order(8)
    void testDelete() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        this.requestGetWithOk(DEFAULT_DELETE, createdFormId);
        createdFormId = null;
    }

    private void assertAdminUsers(String... expectedUserIds) {
        LambdaQueryWrapper<CustomFormAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormAdmin::getCustomFormId, createdFormId);
        Set<String> actualUserIds = new HashSet<>(customFormAdminMapper.selectListByLambda(wrapper)
                .stream()
                .map(CustomFormAdmin::getUserId)
                .toList());
        assertEquals(Set.of(expectedUserIds), actualUserIds);
    }

    private String getFirstCustomFormRoleId() {
        LambdaQueryWrapper<CustomFormRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRole::getCustomFormId, createdFormId);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(wrapper);
        assertTrue(!roles.isEmpty(), "表单创建后应自动创建内置角色");
        return roles.getFirst().getId();
    }

    private void prepareUserDeptAndRoleData() {
        insertDepartment();
        insertRole();
        insertUser("cf-role-direct-user", "直接用户");
        insertUser("cf-role-dept-user", "部门用户");
        insertUser("cf-role-system-role-user", "角色用户");
        insertOrganizationUser("cf-role-direct-org-user", "cf-role-direct-user", "销售顾问");
        insertOrganizationUser("cf-role-dept-org-user", "cf-role-dept-user", "部门专员");
        insertOrganizationUser("cf-role-system-role-org-user", "cf-role-system-role-user", "角色专员");
        insertUserRole("cf-role-direct-user-role", "cf-role-direct-user");
        insertUserRole("cf-role-dept-user-role", "cf-role-dept-user");
        insertUserRole("cf-role-test-user-role", "cf-role-system-role-user");
    }

    private void insertDepartment() {
        Department department = new Department();
        department.setId("cf-role-test-dept");
        department.setName("测试部门");
        department.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        department.setParentId("0");
        department.setPos(1L);
        department.setResource("TEST");
        department.setResourceId("cf-role-test-dept-resource");
        setAuditFields(department);
        departmentMapper.insert(department);
    }

    private void insertRole() {
        Role role = new Role();
        role.setId("cf-role-test-system-role");
        role.setName("测试系统角色");
        role.setInternal(false);
        role.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        role.setDataScope(RoleDataScope.ALL.name());
        setAuditFields(role);
        roleMapper.insert(role);
    }

    private void insertOrganizationUser(String id, String userId, String position) {
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId(id);
        organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        organizationUser.setDepartmentId("cf-role-test-dept");
        organizationUser.setUserId(userId);
        organizationUser.setPosition(position);
        organizationUser.setEnable(true);
        setAuditFields(organizationUser);
        organizationUserMapper.insert(organizationUser);
    }

    private void insertUserRole(String id, String userId) {
        UserRole userRole = new UserRole();
        userRole.setId(id);
        userRole.setRoleId("cf-role-test-system-role");
        userRole.setUserId(userId);
        setAuditFields(userRole);
        userRoleMapper.insert(userRole);
    }

    private void insertUser(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setPassword("123456");
        user.setGender(false);
        setAuditFields(user);
        userMapper.insert(user);
    }

    private void setAuditFields(BaseModel model) {
        long now = System.currentTimeMillis();
        model.setCreateUser("admin");
        model.setUpdateUser("admin");
        model.setCreateTime(now);
        model.setUpdateTime(now);
    }

    private void assertRoleUsers(String roleId, String... expectedUserIds) {
        LambdaQueryWrapper<CustomFormRoleUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormRoleUser::getRoleId, roleId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(wrapper);
        Set<String> actualUserIds = new HashSet<>(roleUsers
                .stream()
                .map(CustomFormRoleUser::getUserId)
                .toList());
        assertEquals(Set.of(expectedUserIds), actualUserIds);
    }

    private void assertRoleUsersPage(String roleId, int current, int pageSize, int total) throws Exception {
        CustomFormRoleUserPageRequest request = new CustomFormRoleUserPageRequest();
        request.setRoleId(roleId);
        request.setCurrent(current);
        request.setPageSize(pageSize);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(ROLE_USERS, request);
        Pager<List<CustomFormRoleUserListResponse>> pager = getPageResult(mvcResult, CustomFormRoleUserListResponse.class);
        assertEquals(current, pager.getCurrent());
        assertEquals(pageSize, pager.getPageSize());
        assertEquals(total, pager.getTotal());
        assertEquals(pageSize, pager.getList().size());
        CustomFormRoleUserListResponse first = pager.getList().getFirst();
        assertNotNull(first.getCustomFormRoleUserId());
        assertNotNull(first.getUserId());
        assertNotNull(first.getUsername());
        assertNotNull(first.getCreateTime());
        assertTrue(pager.getList().stream().allMatch(user -> "cf-role-test-dept".equals(user.getDepartmentId())));
        assertTrue(pager.getList().stream().allMatch(user -> "测试部门".equals(user.getDepartmentName())));
        assertTrue(pager.getList().stream().allMatch(user -> user.getPosition() != null));
        assertTrue(pager.getList().stream().allMatch(user -> user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "cf-role-test-system-role".equals(role.getId()))));
    }
}
