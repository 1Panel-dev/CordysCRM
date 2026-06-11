package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.RoleDataScope;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.DeptUserTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.RoleUserTreeNode;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.*;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import cn.cordys.crm.system.domain.*;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomFormControllerTests extends BaseTest {

    private static final String BASE_PATH = "/custom-form/";
    private static final String OPTION = "option";
    private static final String ADMIN_GET = "admin/get/{0}";
    private static final String ROLE_USERS = "role/users";

    private static final String ROLE_DEPT_TREE = "role/user/dept/tree";
    private static final String ROLE_ROLE_TREE = "role/user/role/tree";
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
        CustomFormAddRequest request = JSON.parseObject("{\"name\":\"未命名表单sdfsdf\",\"enable\":true,\"fields\":[{\"id\":\"178090868410300000\",\"type\":\"DIVIDER\",\"icon\":\"iconicon_dividing_line\",\"name\":\"基本信息\",\"fieldWidth\":1,\"showLabel\":true,\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[],\"dividerClass\":\"divider--normal\",\"dividerColor\":\"#edf0f1\",\"titleColor\":\"#323535\",\"placeholder\":\"\",\"sumColumns\":[]},{\"id\":\"178090868410300001\",\"type\":\"INPUT\",\"name\":\"名称\",\"icon\":\"iconicon_single_line_text\",\"fieldWidth\":1,\"showLabel\":true,\"defaultValue\":\"\",\"defaultValueType\":\"custom\",\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[{\"key\":\"required\",\"required\":true,\"message\":\"common.notNull\",\"label\":\"common.required\",\"trigger\":[\"change\",\"blur\"]}],\"formula\":\"\",\"businessKey\":\"name\",\"disabledProps\":[\"readable\",\"mobile\",\"rules.required\"],\"internalKey\":\"customFormDataName\",\"placeholder\":\"\",\"sumColumns\":[]},{\"id\":\"178090868410300002\",\"type\":\"INPUT\",\"name\":\"自定义字段1\",\"icon\":\"iconicon_single_line_text\",\"fieldWidth\":1,\"showLabel\":true,\"defaultValue\":\"\",\"defaultValueType\":\"custom\",\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[],\"formula\":\"\",\"placeholder\":\"\",\"sumColumns\":[]},{\"id\":\"178090868410300003\",\"type\":\"INPUT\",\"name\":\"自定义字段2\",\"icon\":\"iconicon_single_line_text\",\"fieldWidth\":1,\"showLabel\":true,\"defaultValue\":\"\",\"defaultValueType\":\"custom\",\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[],\"formula\":\"\",\"placeholder\":\"\",\"sumColumns\":[]},{\"id\":\"178090868410300004\",\"type\":\"DIVIDER\",\"icon\":\"iconicon_dividing_line\",\"name\":\"负责人信息\",\"fieldWidth\":1,\"showLabel\":true,\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[],\"dividerClass\":\"divider--normal\",\"dividerColor\":\"#edf0f1\",\"titleColor\":\"#323535\",\"placeholder\":\"\",\"sumColumns\":[]},{\"id\":\"178090868410300005\",\"type\":\"MEMBER\",\"icon\":\"iconicon_member_single_choice\",\"name\":\"负责人\",\"fieldWidth\":1,\"showLabel\":true,\"description\":\"\",\"readable\":true,\"editable\":true,\"mobile\":true,\"rules\":[],\"defaultValue\":\"admin\",\"initialOptions\":[{\"id\":\"admin\",\"name\":\"Administrator\"}],\"hasCurrentUser\":false,\"multiple\":false,\"businessKey\":\"owner\",\"disabledProps\":[],\"internalKey\":\"customFormDataNOwner\",\"placeholder\":\"\",\"sumColumns\":[]}],\"formProp\":{\"layout\":1,\"labelPos\":\"top\",\"inputWidth\":\"custom\",\"optBtnContent\":[{\"text\":\"保存\",\"enable\":true},{\"text\":\"保存并继续添加\",\"enable\":false},{\"text\":\"取消\",\"enable\":true}],\"optBtnPos\":\"flex-row\"}}", CustomFormAddRequest.class);
        request.setName("测试自定义表单-X");
        request.setEnable(true);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomForm form = getResultData(mvcResult, CustomForm.class);
        assertNotNull(form);
        assertNotNull(form.getId());
        assertEquals("测试自定义表单-X", form.getName());

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
        assertEquals("测试自定义表单-X", response.getName());
        assertNotNull(response.getCreator());
        assertEquals("admin", response.getCreator().getId());
        assertEquals("Administrator", response.getCreator().getName());
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
        request.setUserIds(List.of("cf-role-direct-user", "cf-role-disabled-direct-user"));
        request.setDeptIds(List.of("cf-role-test-dept"));
        request.setRoleIds(List.of("cf-role-test-system-role"));

        this.requestPostWithOk("role/user/add", request);

        assertRoleUsers(roleId, "cf-role-direct-user", "cf-role-dept-user", "cf-role-system-role-user");
        assertRoleUsersPage(roleId, 1, 3, 3);
    }

    @Test
    @Order(8)
    void testRoleUserTrees() throws Exception {
        MvcResult deptTreeResult = this.requestGetWithOkAndReturn(ROLE_DEPT_TREE);
        List<DeptUserTreeNode> deptTree = getResultDataArray(deptTreeResult, DeptUserTreeNode.class);
        assertTrue(containsTreeNode(deptTree, "cf-role-test-dept"));
        assertTrue(containsTreeNode(deptTree, "cf-role-dept-user"));

        MvcResult roleTreeResult = this.requestGetWithOkAndReturn(ROLE_ROLE_TREE);
        List<RoleUserTreeNode> roleTree = getResultDataArray(roleTreeResult, RoleUserTreeNode.class);
        assertTrue(containsTreeNode(roleTree, "cf-role-test-system-role"));
        assertTrue(containsTreeNode(roleTree, "cf-role-system-role-user"));
    }

    @Test
    @Order(9)
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

    private boolean containsTreeNode(List<? extends BaseTreeNode> treeNodes, String id) {
        return treeNodes.stream().anyMatch(node -> containsTreeNode(node, id));
    }

    private boolean containsTreeNode(BaseTreeNode node, String id) {
        if (id.equals(node.getId())) {
            return true;
        }
        return node.getChildren().stream().anyMatch(child -> containsTreeNode(child, id));
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
        insertUser("cf-role-disabled-direct-user", "禁用直接用户");
        insertUser("cf-role-disabled-dept-user", "禁用部门用户");
        insertUser("cf-role-disabled-role-user", "禁用角色用户");
        insertOrganizationUser("cf-role-direct-org-user", "cf-role-direct-user", "销售顾问");
        insertOrganizationUser("cf-role-dept-org-user", "cf-role-dept-user", "部门专员");
        insertOrganizationUser("cf-role-system-role-org-user", "cf-role-system-role-user", "角色专员");
        insertOrganizationUser("cf-role-disabled-direct-org-user", "cf-role-disabled-direct-user", "禁用直接用户", false);
        insertOrganizationUser("cf-role-disabled-dept-org-user", "cf-role-disabled-dept-user", "禁用部门用户", false);
        insertOrganizationUser("cf-role-disabled-role-org-user", "cf-role-disabled-role-user", "禁用角色用户", false);
        insertUserRole("cf-role-direct-user-role", "cf-role-direct-user");
        insertUserRole("cf-role-dept-user-role", "cf-role-dept-user");
        insertUserRole("cf-role-test-user-role", "cf-role-system-role-user");
        insertUserRole("cf-role-disabled-role-user-role", "cf-role-disabled-role-user");
    }

    private void insertDepartment() {
        Department department = new Department();
        department.setId("cf-role-test-dept");
        department.setName("自定义表单测试部门");
        department.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        department.setParentId("NONE");
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
        insertOrganizationUser(id, userId, position, true);
    }

    private void insertOrganizationUser(String id, String userId, String position, boolean enable) {
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId(id);
        organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        organizationUser.setDepartmentId("cf-role-test-dept");
        organizationUser.setUserId(userId);
        organizationUser.setPosition(position);
        organizationUser.setEnable(enable);
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
        request.setCustomFormRoleId(roleId);
        request.setCurrent(current);
        request.setPageSize(pageSize);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(ROLE_USERS, request);
        Pager<List<CustomFormRoleUserListResponse>> pager = getPageResult(mvcResult, CustomFormRoleUserListResponse.class);
        assertEquals(current, pager.getCurrent());
        assertEquals(pageSize, pager.getPageSize());
        assertEquals(total, pager.getTotal());
        assertEquals(pageSize, pager.getList().size());
        CustomFormRoleUserListResponse first = pager.getList().getFirst();
        assertNotNull(first.getId());
        assertNotNull(first.getUserId());
        assertNotNull(first.getUsername());
        assertNotNull(first.getCreateTime());
        assertTrue(pager.getList().stream().allMatch(user -> "cf-role-test-dept".equals(user.getDepartmentId())));
        assertTrue(pager.getList().stream().allMatch(user -> "自定义表单测试部门".equals(user.getDepartmentName())));
        assertTrue(pager.getList().stream().allMatch(user -> user.getPosition() != null));
        assertTrue(pager.getList().stream().allMatch(user -> user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "cf-role-test-system-role".equals(role.getId()))));
    }
}
