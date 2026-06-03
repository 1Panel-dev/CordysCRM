package cn.cordys.crm.form.controller;

import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.system.dto.form.FormProp;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomFormControllerTests extends BaseTest {

    private static final String BASE_PATH = "/custom-form/";
    private static String createdFormId;

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
    void testGet() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, createdFormId);
        CustomFormGetResponse response = getResultData(mvcResult, CustomFormGetResponse.class);
        assertNotNull(response);
        assertEquals(createdFormId, response.getId());
        assertEquals("测试自定义表单", response.getName());
    }

    @Test
    @Order(4)
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
    @Order(5)
    void testAddAdmins() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        CustomFormAdminBatchRequest request = new CustomFormAdminBatchRequest();
        request.setCustomFormId(createdFormId);
        request.setUserIds(List.of("test-admin-user"));

        this.requestPostWithOk("/admin/add", request);
    }

    @Test
    @Order(6)
    void testRemoveAdmins() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        CustomFormAdminBatchRequest request = new CustomFormAdminBatchRequest();
        request.setCustomFormId(createdFormId);
        request.setUserIds(List.of("test-admin-user"));

        this.requestPostWithOk("/admin/remove", request);
    }

    @Test
    @Order(7)
    void testDelete() throws Exception {
        assertNotNull(createdFormId, "表单应已创建");

        this.requestGetWithOk(DEFAULT_DELETE, createdFormId);
        createdFormId = null;
    }
}
