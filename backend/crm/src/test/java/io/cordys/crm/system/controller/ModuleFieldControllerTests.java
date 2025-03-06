package io.cordys.crm.system.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.FieldSourceType;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.dto.request.ModuleFieldRequest;
import io.cordys.crm.system.dto.request.ModuleSourceDataRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ModuleFieldControllerTests extends BaseTest {

	@Resource
	private BaseMapper<ModuleField> moduleFieldMapper;

	public static final String BASE_PATH = "/field";
	public static final String DEPT_TREE = "/dept/tree";
	public static final String USER_DEPT_TREE = "/user/dept/tree";
	public static final String SOURCE_DATA = "/source/data";


	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void initData() {
		ModuleField field = new ModuleField();
		field.setId("dep-test-1");
		field.setFormId("form-test-1");
		field.setType(FieldType.DEPARTMENT.name());
		field.setPos(1L);
		field.setCreateUser("admin");
		field.setCreateTime(System.currentTimeMillis());
		field.setUpdateUser("admin");
		field.setUpdateTime(System.currentTimeMillis());
		moduleFieldMapper.insert(field);
	}

	@Test
	@Order(2)
	void testGetDeptTree() throws Exception {
		ModuleFieldRequest request = new ModuleFieldRequest();
		request.setFieldId("dep-test-1");
		request.setFormKey(FormKey.CUSTOMER.getKey());
		this.requestPostWithOk(DEPT_TREE, request);
	}

	@Test
	@Order(3)
	void testGetUserDeptTree() throws Exception {
		ModuleFieldRequest request = new ModuleFieldRequest();
		request.setFieldId("dep-test-1");
		request.setFormKey(FormKey.CUSTOMER.getKey());
		MvcResult r1 = this.requestPost(USER_DEPT_TREE, request).andReturn();
		assert r1.getResponse().getContentAsString().contains(Translator.get("module.field.not_match_type"));
		request.setFieldId("dep-not-exit");
		MvcResult r2 = this.requestPost(USER_DEPT_TREE, request).andReturn();
		assert r2.getResponse().getContentAsString().contains(Translator.get("module.field.not_exist"));
	}

	@Test
	@Order(4)
	void testGetSourceData() throws Exception {
		ModuleSourceDataRequest sourceDataRequest = new ModuleSourceDataRequest();
		sourceDataRequest.setFieldId("dep-test-1");
		sourceDataRequest.setFormKey(FormKey.CUSTOMER.getKey());
		sourceDataRequest.setCurrent(1);
		sourceDataRequest.setPageSize(10);
		sourceDataRequest.setSourceType(FieldSourceType.CUSTOMER.name());
		this.requestPost(SOURCE_DATA, sourceDataRequest);
	}
}
