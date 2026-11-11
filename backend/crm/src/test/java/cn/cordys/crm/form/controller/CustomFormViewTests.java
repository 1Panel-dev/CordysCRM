package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.InternalUserView;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.crm.system.domain.OrganizationUser;
import cn.cordys.crm.system.domain.UserView;
import cn.cordys.crm.system.dto.response.UserViewListResponse;
import cn.cordys.crm.system.dto.response.UserViewResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomFormViewTests extends BaseTest {

    private static final String DATA_BASE_PATH = "/custom-form/data/";
    private static final String VIEW_BASE_PATH = "/custom-form/view/";

    private static String formIdA;
    private static String formIdB;
    private static String viewIdA;

    @Resource
    private BaseMapper<CustomForm> customFormMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private BaseMapper<ModuleFormBlob> moduleFormBlobMapper;
    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormData> customFormDataMapper;
    @Resource
    private BaseMapper<OrganizationUser> organizationUserMapper;
    @Resource
    private BaseMapper<UserView> userViewMapper;

    @Override
    protected String getBasePath() {
        return "/";
    }

    private void ensureFormsExist() {
        if (formIdA == null) {
            formIdA = IDGenerator.nextStr();
            formIdB = IDGenerator.nextStr();
        }
        createFormIfAbsent(formIdA);
        createFormIfAbsent(formIdB);
    }

    private void createFormIfAbsent(String formId) {
        if (customFormMapper.selectByPrimaryKey(formId) != null) {
            return;
        }
        long now = System.currentTimeMillis();
        String admin = InternalUser.ADMIN.getValue();

        CustomForm form = new CustomForm();
        form.setId(formId);
        form.setName(UUID.randomUUID().toString());
        form.setEnable(true);
        form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        form.setCreateTime(now);
        form.setUpdateTime(now);
        form.setCreateUser(admin);
        form.setUpdateUser(admin);
        customFormMapper.insert(form);

        ModuleForm moduleForm = new ModuleForm();
        moduleForm.setId(formId);
        moduleForm.setFormKey(formId);
        moduleForm.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        moduleForm.setCreateTime(now);
        moduleForm.setUpdateTime(now);
        moduleForm.setCreateUser(admin);
        moduleForm.setUpdateUser(admin);
        moduleFormMapper.insert(moduleForm);

        ModuleFormBlob formBlob = new ModuleFormBlob();
        formBlob.setId(formId);
        formBlob.setProp("{}");
        moduleFormBlobMapper.insert(formBlob);

        List<CustomFormRole> roles = Arrays.stream(CustomFormRoleKey.values())
                .map(key -> {
                    CustomFormRole role = new CustomFormRole();
                    role.setId(IDGenerator.nextStr());
                    role.setName(key.getKey());
                    role.setCustomFormId(formId);
                    role.setInternalKey(key.getKey());
                    role.setCreateTime(now);
                    role.setUpdateTime(now);
                    role.setCreateUser(admin);
                    role.setUpdateUser(admin);
                    return role;
                }).toList();
        customFormRoleMapper.batchInsert(roles);
    }

    private void ensureAdminOrganizationUser() {
        String adminId = InternalUser.ADMIN.getValue();
        LambdaQueryWrapper<OrganizationUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrganizationUser::getUserId, adminId)
                .eq(OrganizationUser::getOrganizationId, DEFAULT_ORGANIZATION_ID);
        if (organizationUserMapper.selectListByLambda(wrapper).isEmpty()) {
            OrganizationUser organizationUser = new OrganizationUser();
            organizationUser.setId(IDGenerator.nextStr());
            organizationUser.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            organizationUser.setUserId(adminId);
            organizationUser.setEnable(true);
            organizationUser.setCreateTime(System.currentTimeMillis());
            organizationUser.setUpdateTime(System.currentTimeMillis());
            organizationUser.setCreateUser(adminId);
            organizationUser.setUpdateUser(adminId);
            organizationUserMapper.insert(organizationUser);
        }
    }

    private void createData(String formId, String name, String owner) {
        long now = System.currentTimeMillis();
        CustomFormData data = new CustomFormData();
        data.setId(IDGenerator.nextStr());
        data.setCustomFormId(formId);
        data.setName(name);
        data.setOwner(owner);
        data.setCreateTime(now);
        data.setUpdateTime(now);
        data.setCreateUser(owner);
        data.setUpdateUser(owner);
        customFormDataMapper.insert(data);
    }

    private Pager<List<CustomFormDataListResponse>> page(String formId, String viewId) throws Exception {
        CustomFormDataPageRequest request = new CustomFormDataPageRequest();
        request.setCustomFormId(formId);
        request.setCurrent(1);
        request.setPageSize(100);
        request.setViewId(viewId);
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DATA_BASE_PATH + DEFAULT_PAGE, request);
        return getPageResult(mvcResult, CustomFormDataListResponse.class);
    }

    @Test
    @Order(1)
    void testDataScope() throws Exception {
        ensureFormsExist();

        MvcResult mvcResult = this.requestGetWithOkAndReturn("/custom-form/data-scope/{0}", formIdA);
        String dataScope = getResultData(mvcResult, String.class);
        assertEquals(CustomFormRoleKey.MANAGE_ALL.getKey(), dataScope);
    }

    @Test
    @Order(2)
    void testAddListView() throws Exception {
        ensureFormsExist();

        Map<String, Object> request = Map.of(
                "name", "我的视图",
                "customFormId", formIdA,
                "searchMode", "AND");

        MvcResult mvcResult = this.requestPostWithOkAndReturn(VIEW_BASE_PATH + DEFAULT_ADD, request);
        UserView userView = getResultData(mvcResult, UserView.class);
        assertNotNull(userView);
        assertEquals(formIdA, userView.getResourceType());
        viewIdA = userView.getId();

        MvcResult listResult = this.requestGetWithOkAndReturn(VIEW_BASE_PATH + "list?customFormId=" + formIdA);
        List<UserViewListResponse> views = getResultDataArray(listResult, UserViewListResponse.class);
        assertTrue(views.stream().anyMatch(v -> viewIdA.equals(v.getId())));

        // 同一用户另一个表单的视图列表不可见
        MvcResult otherListResult = this.requestGetWithOkAndReturn(VIEW_BASE_PATH + "list?customFormId=" + formIdB);
        List<UserViewListResponse> otherViews = getResultDataArray(otherListResult, UserViewListResponse.class);
        assertTrue(otherViews.stream().noneMatch(v -> viewIdA.equals(v.getId())));
    }

    @Test
    @Order(3)
    void testViewDetailAndUpdate() throws Exception {
        assertNotNull(viewIdA);

        MvcResult detailResult = this.requestGetWithOkAndReturn(
                VIEW_BASE_PATH + "detail/{0}?customFormId={1}", viewIdA, formIdA);
        assertEquals("我的视图", getResultData(detailResult, UserViewResponse.class).getName());

        Map<String, Object> updateRequest = Map.of(
                "id", viewIdA,
                "name", "改名后的视图",
                "customFormId", formIdA,
                "searchMode", "AND");
        this.requestPostWithOk(VIEW_BASE_PATH + DEFAULT_UPDATE, updateRequest);

        MvcResult updatedResult = this.requestGetWithOkAndReturn(
                VIEW_BASE_PATH + "detail/{0}?customFormId={1}", viewIdA, formIdA);
        assertEquals("改名后的视图", getResultData(updatedResult, UserViewResponse.class).getName());
    }

    @Test
    @Order(4)
    void testCrossFormViewAccessDenied() throws Exception {
        assertNotNull(viewIdA);

        // 使用其他表单的 customFormId 操作该视图 -> 404
        this.requestGet(VIEW_BASE_PATH + "detail/{0}?customFormId={1}", viewIdA, formIdB)
                .andExpect(status().isNotFound());
        this.requestGet(VIEW_BASE_PATH + "fixed/{0}?customFormId={1}", viewIdA, formIdB)
                .andExpect(status().isNotFound());
        this.requestGet(VIEW_BASE_PATH + "delete/{0}?customFormId={1}", viewIdA, formIdB)
                .andExpect(status().isNotFound());

        // 视图仍在
        assertNotNull(userViewMapper.selectByPrimaryKey(viewIdA));
    }

    @Test
    @Order(5)
    void testPageWithOtherFormViewDenied() throws Exception {
        ensureFormsExist();
        assertNotNull(viewIdA);

        // 用表单B的分页请求携带表单A的视图 -> 404
        this.requestPost(DATA_BASE_PATH + DEFAULT_PAGE, Map.of(
                "customFormId", formIdB,
                "current", 1,
                "pageSize", 10,
                "viewId", viewIdA))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void testPageWithSelfView() throws Exception {
        ensureFormsExist();
        ensureAdminOrganizationUser();

        String adminId = InternalUser.ADMIN.getValue();
        createData(formIdA, "管理员数据", adminId);
        createData(formIdA, "他人数据", "other-user-view-test");

        Pager<List<CustomFormDataListResponse>> pager = page(formIdA, InternalUserView.SELF.name());
        assertFalse(pager.getList().isEmpty());
        assertTrue(pager.getList().stream().allMatch(d -> adminId.equals(d.getOwner())));
    }

    @Test
    @Order(7)
    void testPageWithAllView() throws Exception {
        ensureFormsExist();
        ensureAdminOrganizationUser();

        Pager<List<CustomFormDataListResponse>> pager = page(formIdA, InternalUserView.ALL.name());
        assertTrue(pager.getList().stream().anyMatch(d -> "other-user-view-test".equals(d.getOwner())));
    }

    @Test
    @Order(8)
    void testPageWithDepartmentView() throws Exception {
        ensureFormsExist();
        ensureAdminOrganizationUser();

        // 管理员未分配部门时部门视图应返回空数据, 而不是退化为全部数据
        Pager<List<CustomFormDataListResponse>> pager = page(formIdA, InternalUserView.DEPARTMENT.name());
        assertNotNull(pager.getList());
        assertTrue(pager.getList().isEmpty());
    }

    @Test
    @Order(9)
    void testPageWithOtherUserCustomView() throws Exception {
        ensureFormsExist();

        // 构造他人创建的视图, 当前用户不可使用
        long now = System.currentTimeMillis();
        UserView otherView = new UserView();
        otherView.setId(IDGenerator.nextStr());
        otherView.setName("他人视图");
        otherView.setUserId("other-user-view-test");
        otherView.setResourceType(formIdA);
        otherView.setFixed(false);
        otherView.setEnable(true);
        otherView.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        otherView.setPos(0L);
        otherView.setSearchMode("AND");
        otherView.setCreateTime(now);
        otherView.setUpdateTime(now);
        otherView.setCreateUser("other-user-view-test");
        otherView.setUpdateUser("other-user-view-test");
        userViewMapper.insert(otherView);

        this.requestPost(DATA_BASE_PATH + DEFAULT_PAGE, Map.of(
                "customFormId", formIdA,
                "current", 1,
                "pageSize", 10,
                "viewId", otherView.getId()))
                .andExpect(status().isNotFound());

        this.requestGet(VIEW_BASE_PATH + "detail/{0}?customFormId={1}", otherView.getId(), formIdA)
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    void testDeleteFormCleanupViews() throws Exception {
        ensureFormsExist();

        Map<String, Object> request = Map.of(
                "name", "待清理视图",
                "customFormId", formIdB,
                "searchMode", "AND");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(VIEW_BASE_PATH + DEFAULT_ADD, request);
        String viewId = getResultData(mvcResult, UserView.class).getId();
        assertNotNull(userViewMapper.selectByPrimaryKey(viewId));

        this.requestGetWithOk("/custom-form/delete/{0}", formIdB);

        assertNull(userViewMapper.selectByPrimaryKey(viewId), "删除表单后其视图应被清理");
        assertNull(customFormMapper.selectByPrimaryKey(formIdB));
        formIdB = IDGenerator.nextStr();
    }

    @Test
    @Order(11)
    void testFixedEnableDelete() throws Exception {
        assertNotNull(viewIdA);

        this.requestGetWithOk(VIEW_BASE_PATH + "fixed/{0}?customFormId={1}", viewIdA, formIdA);
        assertTrue(userViewMapper.selectByPrimaryKey(viewIdA).getFixed());

        this.requestGetWithOk(VIEW_BASE_PATH + "enable/{0}?customFormId={1}", viewIdA, formIdA);
        assertFalse(userViewMapper.selectByPrimaryKey(viewIdA).getEnable());

        this.requestGetWithOk(VIEW_BASE_PATH + "delete/{0}?customFormId={1}", viewIdA, formIdA);
        assertNull(userViewMapper.selectByPrimaryKey(viewIdA));
    }
}
