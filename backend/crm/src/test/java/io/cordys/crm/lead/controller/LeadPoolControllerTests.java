package io.cordys.crm.lead.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.domain.LeadPoolPickRule;
import io.cordys.crm.lead.domain.LeadPoolRecycleRule;
import io.cordys.crm.lead.domain.LeadPoolRelation;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolPageRequest;
import io.cordys.crm.lead.dto.request.LeadPoolSaveRequest;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LeadPoolControllerTests extends BaseTest {

	private static LeadPoolDTO testLeadPool;

	@Resource
	private BaseMapper<LeadPool> leadPoolMapper;
	@Resource
	private BaseMapper<LeadPoolRelation> leadPoolRelationMapper;

	@Test
	@Order(1)
	void emptyPage() throws Exception {
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/lead-pool/page", createPageRequest());
		Pager<List<LeadPoolDTO>> result = getPageResult(mvcResult, LeadPoolDTO.class);
		assert result.getList().isEmpty();
	}

	@Test
	@Order(2)
	void add() throws Exception {
		LeadPool leadPool = createLeadPool();
		LeadPoolSaveRequest request = new LeadPoolSaveRequest();
		BeanUtils.copyBean(request, leadPool);
		request.setPickRule(createPickRule());
		request.setRecycleRule(createRecycleRule());
		this.requestPostWithOk("/lead-pool/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		LeadPoolPageRequest pageRequest = createPageRequest();
		pageRequest.setSort(Map.of("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/lead-pool/page", pageRequest);
		Pager<List<LeadPoolDTO>> result = getPageResult(mvcResult, LeadPoolDTO.class);
		assert result.getList().size() == 1;
		testLeadPool = result.getList().getFirst();
	}

	@Test
	@Order(4)
	void update() throws Exception {
		LeadPool leadPool = createLeadPool();
		leadPool.setId(testLeadPool.getId());
		LeadPoolSaveRequest request = new LeadPoolSaveRequest();
		BeanUtils.copyBean(request, leadPool);
		LeadPoolPickRule pickRule = createPickRule();
		pickRule.setId("default-pick-rule");
		request.setPickRule(pickRule);
		LeadPoolRecycleRule recycleRule = createRecycleRule();
		recycleRule.setId("default-recycle-rule");
		request.setRecycleRule(recycleRule);
		MvcResult mvcResult = this.requestPost("/lead-pool/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("lead_pool_access_fail"));
		// update owner id by sql
		leadPool.setOwnerId("admin");
		leadPoolMapper.updateById(leadPool);
		request.setOwnerId("admin");
		this.requestPostWithOk("/lead-pool/update", request);
	}

	@Test
	@Order(5)
	void switchStatus() throws Exception {
		MvcResult mvcResult = this.requestGet("/lead-pool/switch/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("lead_pool_not_exist"));
		this.requestGetWithOk("/lead-pool/switch/" + testLeadPool.getId());
	}

	@Test
	@Order(6)
	void delete() throws Exception {
		MvcResult mvcResult = this.requestGet("/lead-pool/delete/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("lead_pool_not_exist"));
		// insert free lead on the pool, then delete it
		LeadPoolRelation leadPoolRelation = createLeadPoolRelation();
		leadPoolRelation.setPoolId(testLeadPool.getId());
		leadPoolRelationMapper.insert(leadPoolRelation);
		MvcResult mvcResult1 = this.requestGet("/lead-pool/delete/" + testLeadPool.getId()).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("lead_pool_related"));
		// pick lead, delete the pool
		leadPoolRelationMapper.deleteByPrimaryKey(leadPoolRelation.getId());
		this.requestGetWithOk("/lead-pool/delete/" + testLeadPool.getId());
	}

	private LeadPoolPageRequest createPageRequest() {
		LeadPoolPageRequest request = new LeadPoolPageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		return request;
	}

	private LeadPool createLeadPool() {
		LeadPool leadPool = new LeadPool();
		leadPool.setName("default-lead-pool");
		leadPool.setScopeId("default-dp");
		leadPool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		leadPool.setOwnerId("default-owner");
		leadPool.setEnable(true);
		return leadPool;
	}

	private LeadPoolPickRule createPickRule() {
		LeadPoolPickRule pickRule = new LeadPoolPickRule();
		pickRule.setPickNumber(1);
		pickRule.setLimitOnNumber(true);
		pickRule.setLimitPreOwner(true);
		pickRule.setPickIntervalDays(1);
		return pickRule;
	}

	private LeadPoolRecycleRule createRecycleRule() {
		LeadPoolRecycleRule recycleRule = new LeadPoolRecycleRule();
		recycleRule.setExpireNotice(true);
		recycleRule.setNoticeDays(10);
		recycleRule.setAuto(true);
		return recycleRule;
	}

	private LeadPoolRelation createLeadPoolRelation() {
		LeadPoolRelation leadPoolRelation = new LeadPoolRelation();
		leadPoolRelation.setId("default-lead-pool-relation");
		leadPoolRelation.setLeadId("default-lead");
		leadPoolRelation.setCreateTime(System.currentTimeMillis());
		leadPoolRelation.setCreateUser("default-user");
		leadPoolRelation.setUpdateTime(System.currentTimeMillis());
		leadPoolRelation.setUpdateUser("default-user");
		return leadPoolRelation;
	}
}
