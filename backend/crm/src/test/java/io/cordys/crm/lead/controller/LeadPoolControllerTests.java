package io.cordys.crm.lead.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.lead.domain.LeadPool;
import io.cordys.crm.lead.domain.LeadPoolRelation;
import io.cordys.crm.lead.dto.LeadPoolDTO;
import io.cordys.crm.lead.dto.request.LeadPoolAddRequest;
import io.cordys.crm.lead.dto.request.LeadPoolPickRuleSaveRequest;
import io.cordys.crm.lead.dto.request.LeadPoolRecycleRuleSaveRequest;
import io.cordys.crm.lead.dto.request.LeadPoolUpdateRequest;
import io.cordys.crm.system.dto.RuleConditionDTO;
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
		LeadPoolAddRequest request = new LeadPoolAddRequest();
		BeanUtils.copyBean(request, leadPool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		LeadPoolPickRuleSaveRequest pickRule = LeadPoolPickRuleSaveRequest.builder()
				.pickNumber(1).limitOnNumber(true).pickIntervalDays(1).limitPreOwner(true).build();
		request.setPickRule(pickRule);
		RuleConditionDTO condition = new RuleConditionDTO();
		condition.setColumn("name");
		condition.setOperator("=");
		condition.setValue("cc");
		LeadPoolRecycleRuleSaveRequest recycleRule = LeadPoolRecycleRuleSaveRequest.builder().expireNotice(true).noticeDays(10).conditions(List.of(condition)).build();
		request.setRecycleRule(recycleRule);
		this.requestPostWithOk("/lead-pool/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		BasePageRequest request = createPageRequest();
		request.setSort(Map.of("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/lead-pool/page", request);
		Pager<List<LeadPoolDTO>> result = getPageResult(mvcResult, LeadPoolDTO.class);
		assert result.getList().size() == 1;
		testLeadPool = result.getList().getFirst();
	}

	@Test
	@Order(4)
	void update() throws Exception {
		LeadPool leadPool = createLeadPool();
		leadPool.setId(testLeadPool.getId());
		LeadPoolUpdateRequest request = new LeadPoolUpdateRequest();
		BeanUtils.copyBean(request, leadPool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		LeadPoolPickRuleSaveRequest pickRule = LeadPoolPickRuleSaveRequest.builder()
				.pickNumber(1).limitOnNumber(true).pickIntervalDays(1).limitPreOwner(true).build();
		request.setPickRule(pickRule);
		LeadPoolRecycleRuleSaveRequest recycleRule = LeadPoolRecycleRuleSaveRequest.builder().expireNotice(true).noticeDays(10).build();
		request.setRecycleRule(recycleRule);
		MvcResult mvcResult = this.requestPost("/lead-pool/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("lead_pool_access_fail"));
		// update owner id by sql
		leadPool.setOwnerId(JSON.toJSONString(List.of("admin")));
		leadPoolMapper.updateById(leadPool);
		request.setOwnerIds(List.of("admin"));
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
		MvcResult mvcResult1 = this.requestGet("/lead-pool/check-pick/" + testLeadPool.getId()).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult1.getResponse().getContentAsString().contains(Translator.get("lead_pool_related"));
		// pick lead, delete the pool
		leadPoolRelationMapper.deleteByPrimaryKey(leadPoolRelation.getId());
		this.requestGetWithOk("/lead-pool/delete/" + testLeadPool.getId());
	}

	private BasePageRequest createPageRequest() {
		BasePageRequest request = new BasePageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		return request;
	}

	private LeadPool createLeadPool() {
		LeadPool leadPool = new LeadPool();
		leadPool.setName("default-lead-pool");
		leadPool.setScopeId(JSON.toJSONString(List.of("default-dp")));
		leadPool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		leadPool.setEnable(true);
		leadPool.setAuto(true);
		return leadPool;
	}

	private LeadPoolRelation createLeadPoolRelation() {
		LeadPoolRelation leadPoolRelation = new LeadPoolRelation();
		leadPoolRelation.setId("default-lead-pool-relation");
		leadPoolRelation.setLeadId("default-lead");
		leadPoolRelation.setPicked(false);
		leadPoolRelation.setLastPickUserId("default-user");
		leadPoolRelation.setLastPickTime(System.currentTimeMillis());
		leadPoolRelation.setCreateTime(System.currentTimeMillis());
		leadPoolRelation.setCreateUser("default-user");
		leadPoolRelation.setUpdateTime(System.currentTimeMillis());
		leadPoolRelation.setUpdateUser("default-user");
		return leadPoolRelation;
	}
}
