package cn.cordys.crm.form.controller;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.domain.ModuleFormBlob;
import cn.cordys.common.pager.Pager;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomFormDataControllerTests extends BaseTest {

    private static final String BASE_PATH = "/custom-form/data/";
    private static String createdDataId;
    private static String testFormId;

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

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    private void ensureFormExists() {
        if (testFormId == null) {
            testFormId = IDGenerator.nextStr();
        }
        if (customFormMapper.selectByPrimaryKey(testFormId) != null) {
            return;
        }

        String formId = testFormId;
        long now = System.currentTimeMillis();

        CustomForm form = new CustomForm();
        form.setId(formId);
        form.setName(UUID.randomUUID().toString());
        form.setEnable(true);
        form.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        form.setCreateTime(now);
        form.setUpdateTime(now);
        form.setCreateUser(InternalUser.ADMIN.getValue());
        form.setUpdateUser(InternalUser.ADMIN.getValue());
        customFormMapper.insert(form);

        ModuleForm moduleForm = new ModuleForm();
        moduleForm.setId(formId);
        moduleForm.setFormKey(formId);
        moduleForm.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        moduleForm.setCreateTime(now);
        moduleForm.setUpdateTime(now);
        moduleForm.setCreateUser(InternalUser.ADMIN.getValue());
        moduleForm.setUpdateUser(InternalUser.ADMIN.getValue());
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
                    role.setCreateUser(InternalUser.ADMIN.getValue());
                    role.setUpdateUser(InternalUser.ADMIN.getValue());
                    return role;
                }).toList();
        customFormRoleMapper.batchInsert(roles);
    }

    @Test
    @Order(1)
    void testAdd() throws Exception {
        ensureFormExists();

        CustomFormDataAddRequest request = new CustomFormDataAddRequest();
        request.setCustomFormId(testFormId);
        request.setName("测试表单数据");

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        CustomFormData data = getResultData(mvcResult, CustomFormData.class);
        assertNotNull(data);
        assertNotNull(data.getId());
        assertEquals("测试表单数据", data.getName());
        assertEquals(testFormId, data.getCustomFormId());

        createdDataId = data.getId();
    }

    @Test
    @Order(2)
    void testGet() throws Exception {
        assertNotNull(createdDataId, "数据应已创建");

        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, createdDataId);
        CustomFormDataGetResponse response = getResultData(mvcResult, CustomFormDataGetResponse.class);
        assertNotNull(response);
        assertEquals(createdDataId, response.getId());
        assertEquals("测试表单数据", response.getName());
    }

    @Test
    @Order(3)
    void testPage() throws Exception {
        ensureFormExists();

        CustomFormDataPageRequest request = new CustomFormDataPageRequest();
        request.setCustomFormId(testFormId);
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomFormDataListResponse>> pager = getPageResult(mvcResult, CustomFormDataListResponse.class);
        assertNotNull(pager);
        assertNotNull(pager.getList());
    }

    @Test
    @Order(4)
    void testPageWithKeyword() throws Exception {
        ensureFormExists();

        CustomFormDataPageRequest request = new CustomFormDataPageRequest();
        request.setCustomFormId(testFormId);
        request.setCurrent(1);
        request.setPageSize(10);
        request.setKeyword("测试");

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<CustomFormDataListResponse>> pager = getPageResult(mvcResult, CustomFormDataListResponse.class);
        assertNotNull(pager);
    }

    @Test
    @Order(5)
    void testUpdate() throws Exception {
        assertNotNull(createdDataId, "数据应已创建");

        CustomFormDataUpdateRequest request = new CustomFormDataUpdateRequest();
        request.setId(createdDataId);
        request.setCustomFormId(testFormId);
        request.setName("更新后的表单数据");

        this.requestPostWithOk(DEFAULT_UPDATE, request);

        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, createdDataId);
        CustomFormDataGetResponse response = getResultData(mvcResult, CustomFormDataGetResponse.class);
        assertEquals("更新后的表单数据", response.getName());
    }

    @Test
    @Order(7)
    void testBatchDelete() throws Exception {
        assertNotNull(createdDataId, "数据应已创建");

        this.requestPostWithOk(DEFAULT_BATCH_DELETE, List.of(createdDataId));

        // 验证数据已被删除
        CustomFormData deleted = customFormDataMapper.selectByPrimaryKey(createdDataId);
        assertNull(deleted, "数据应已被删除");

        createdDataId = null;
    }

    @Test
    @Order(8)
    void testDelete() throws Exception {
        ensureFormExists();

        CustomFormDataAddRequest addRequest = new CustomFormDataAddRequest();
        addRequest.setCustomFormId(testFormId);
        addRequest.setName("待删除数据");

        MvcResult addResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, addRequest);
        CustomFormData data = getResultData(addResult, CustomFormData.class);
        assertNotNull(data);

        this.requestGetWithOk(DEFAULT_DELETE, data.getId());

        CustomFormData deleted = customFormDataMapper.selectByPrimaryKey(data.getId());
        assertNull(deleted, "数据应已被删除");
    }
}
