package cn.cordys.crm.form.controller;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.domain.BaseModel;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.system.domain.OrganizationUser;
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
        request.setRoleId(roleId);
        request.setUserIds(List.of("cf-role-direct-user"));
        request.setDeptIds(List.of("cf-role-test-dept"));
        request.setRoleIds(List.of("cf-role-test-system-role"));

        this.requestPostWithOk("role/user/add", request);

        assertRoleUsers(roleId, "cf-role-direct-user", "cf-role-dept-user", "cf-role-system-role-user");
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
        OrganizationUser organizationUser = new OrganizationUser();
        organizationUser.setId("cf-role-test-org-user");
        organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        organizationUser.setDepartmentId("cf-role-test-dept");
        organizationUser.setUserId("cf-role-dept-user");
        organizationUser.setEnable(true);
        setAuditFields(organizationUser);
        organizationUserMapper.insert(organizationUser);

        UserRole userRole = new UserRole();
        userRole.setId("cf-role-test-user-role");
        userRole.setRoleId("cf-role-test-system-role");
        userRole.setUserId("cf-role-system-role-user");
        setAuditFields(userRole);
        userRoleMapper.insert(userRole);
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
        Set<String> actualUserIds = new HashSet<>(customFormRoleUserMapper.selectListByLambda(wrapper)
                .stream()
                .map(CustomFormRoleUser::getUserId)
                .toList());
        assertEquals(Set.of(expectedUserIds), actualUserIds);
    }
}
