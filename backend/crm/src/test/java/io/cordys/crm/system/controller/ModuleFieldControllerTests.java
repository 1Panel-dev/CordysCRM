package io.cordys.crm.system.controller;

import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.dto.request.*;
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

	public static String FIELD_ID = "field-test-id";

	public static final String BASE_PATH = "/field";
	public static final String DEPT_TREE = "/dept/tree";
	public static final String USER_DEPT_TREE = "/user/dept/tree";
	public static final String CLUE_SOURCE_DATA = "/source/clue";
	public static final String CUSTOMER_SOURCE_DATA = "/source/customer";
	public static final String CONTACT_SOURCE_DATA = "/source/contact";
	public static final String OPPORTUNITY_SOURCE_DATA = "/source/opportunity";
	public static final String PRODUCT_SOURCE_DATA = "/source/product";


	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void initData() {
		ModuleField field = new ModuleField();
		field.setId(FIELD_ID);
		field.setFormId("form-test-1");
		field.setName("dep-test-1");
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
		request.setFieldId(FIELD_ID);
		this.requestPostWithOk(DEPT_TREE, request);
	}

	@Test
	@Order(3)
	void testGetUserDeptTree() throws Exception {
		ModuleFieldRequest request = new ModuleFieldRequest();
		request.setFieldId(FIELD_ID);
		MvcResult r1 = this.requestPost(USER_DEPT_TREE, request).andReturn();
		assert r1.getResponse().getContentAsString().contains(Translator.get("module.field.not_match_type"));
		request.setFieldId("dep-not-exit");
		MvcResult r2 = this.requestPost(USER_DEPT_TREE, request).andReturn();
		assert r2.getResponse().getContentAsString().contains(Translator.get("module.field.not_exist"));
	}

	@Test
	@Order(4)
	void resetFieldData() {
		ModuleField field = new ModuleField();
		field.setId(FIELD_ID);
		field.setType(FieldType.DATA_SOURCE.name());
		moduleFieldMapper.updateById(field);
	}

	@Test
	@Order(5)
	void testListClueSourceData() throws Exception {
		SourceCluePageRequest cluePageRequest = new SourceCluePageRequest();
		cluePageRequest.setFieldId(FIELD_ID);
		cluePageRequest.setCurrent(1);
		cluePageRequest.setPageSize(10);
		this.requestPostWithOk(CLUE_SOURCE_DATA, cluePageRequest);
	}

	@Test
	@Order(6)
	void testListCustomerSourceData() throws Exception {
		SourceCustomerPageRequest customerPageRequest = new SourceCustomerPageRequest();
		customerPageRequest.setFieldId(FIELD_ID);
		customerPageRequest.setCurrent(1);
		customerPageRequest.setPageSize(10);
		this.requestPostWithOk(CUSTOMER_SOURCE_DATA, customerPageRequest);
	}

	@Test
	@Order(7)
	void testListContactSourceData() throws Exception {
		SourceContactPageRequest contactPageRequest = new SourceContactPageRequest();
		contactPageRequest.setFieldId(FIELD_ID);
		contactPageRequest.setCurrent(1);
		contactPageRequest.setPageSize(10);
		this.requestPostWithOk(CONTACT_SOURCE_DATA, contactPageRequest);
	}

	@Test
	@Order(8)
	void testListOpportunitySourceData() throws Exception {
		SourceOpportunityPageRequest opportunityPageRequest = new SourceOpportunityPageRequest();
		opportunityPageRequest.setFieldId(FIELD_ID);
		opportunityPageRequest.setCurrent(1);
		opportunityPageRequest.setPageSize(10);
		this.requestPostWithOk(OPPORTUNITY_SOURCE_DATA, opportunityPageRequest);
	}

	@Test
	@Order(9)
	void testListProductSourceData() throws Exception {
		SourceProductPageRequest productPageRequest = new SourceProductPageRequest();
		productPageRequest.setFieldId(FIELD_ID);
		productPageRequest.setCurrent(1);
		productPageRequest.setPageSize(10);
		this.requestPostWithOk(PRODUCT_SOURCE_DATA, productPageRequest);
	}
}
