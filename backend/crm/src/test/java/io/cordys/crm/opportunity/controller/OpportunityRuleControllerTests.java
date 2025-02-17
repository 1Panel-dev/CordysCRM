package io.cordys.crm.opportunity.controller;

import io.cordys.common.pager.Pager;
import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.opportunity.domain.OpportunityRule;
import io.cordys.crm.opportunity.dto.OpportunityRuleDTO;
import io.cordys.crm.opportunity.dto.request.OpportunityRuleSaveRequest;
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
public class OpportunityRuleControllerTests extends BaseTest {

	private static OpportunityRuleDTO editRule;
	@Resource
	private BaseMapper<OpportunityRule> opportunityRuleMapper;

	@Test
	@Order(1)
	void emptyPage() throws Exception {
		BasePageRequest request = new BasePageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/opportunity-rule/page", request);
		Pager<List<OpportunityRuleDTO>> result = getPageResult(mvcResult, OpportunityRuleDTO.class);
		assert result.getList().isEmpty();
	}

	@Test
	@Order(2)
	void add() throws Exception {
		OpportunityRuleSaveRequest request = OpportunityRuleSaveRequest.builder()
				.name("rule").ownerId("cc").scopeId("admin")
				.enable(true).auto(false).expireNotice(false).build();
		this.requestPostWithOk("/opportunity-rule/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		BasePageRequest request = new BasePageRequest();
		request.setSort(Map.of("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/opportunity-rule/page", request);
		Pager<List<OpportunityRuleDTO>> result = getPageResult(mvcResult, OpportunityRuleDTO.class);
		assert result.getList().size() == 1;
		editRule = result.getList().getFirst();
	}


	@Test
	@Order(4)
	void update() throws Exception {
		OpportunityRuleSaveRequest request = new OpportunityRuleSaveRequest();
		BeanUtils.copyBean(request, editRule);
		MvcResult mvcResult = this.requestPost("/opportunity-rule/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("opportunity.access_fail"));
		// update owner id by sql
		editRule.setOwnerId("admin");
		opportunityRuleMapper.updateById(editRule);
		request.setOwnerId("admin");
		this.requestPostWithOk("/opportunity-rule/update", request);
	}

	@Test
	@Order(5)
	void switchStatus() throws Exception {
		MvcResult mvcResult = this.requestGet("/opportunity-rule/switch/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("opportunity.rule.not_exist"));
		this.requestGetWithOk("/opportunity-rule/switch/" + editRule.getId());
	}

	@Test
	@Order(6)
	void delete() throws Exception {
		MvcResult mvcResult = this.requestGet("/opportunity-rule/delete/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("opportunity.rule.not_exist"));
		this.requestGetWithOk("/opportunity-rule/delete/" + editRule.getId());
	}
}
