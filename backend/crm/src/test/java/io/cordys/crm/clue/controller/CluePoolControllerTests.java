package io.cordys.crm.clue.controller;

import io.cordys.common.dto.SortRequest;
import io.cordys.common.pager.Pager;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.clue.domain.CluePool;
import io.cordys.crm.clue.domain.CluePoolRelation;
import io.cordys.crm.clue.dto.CluePoolDTO;
import io.cordys.crm.clue.dto.CluePoolPickRuleDTO;
import io.cordys.crm.clue.dto.CluePoolRecycleRuleDTO;
import io.cordys.crm.clue.dto.request.CluePoolAddRequest;
import io.cordys.crm.clue.dto.request.CluePoolUpdateRequest;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CluePoolControllerTests extends BaseTest {

	private static CluePoolDTO testCluePool;

	@Resource
	private BaseMapper<CluePool> cluePoolMapper;
	@Resource
	private BaseMapper<CluePoolRelation> cluePoolRelationMapper;

	@Test
	@Order(1)
	void emptyPage() throws Exception {
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/clue-pool/page", createPageRequest());
		Pager<List<CluePoolDTO>> result = getPageResult(mvcResult, CluePoolDTO.class);
		assert result.getList().isEmpty();
	}

	@Test
	@Order(2)
	void add() throws Exception {
		CluePool cluePool = createCluePool();
		CluePoolAddRequest request = new CluePoolAddRequest();
		BeanUtils.copyBean(request, cluePool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		CluePoolPickRuleDTO pickRule = CluePoolPickRuleDTO.builder()
				.pickNumber(1).limitOnNumber(true).pickIntervalDays(1).limitPreOwner(true).build();
		request.setPickRule(pickRule);
		RuleConditionDTO condition = new RuleConditionDTO();
		condition.setColumn("name");
		condition.setOperator("=");
		condition.setValue("cc");
		CluePoolRecycleRuleDTO recycleRule = CluePoolRecycleRuleDTO.builder().expireNotice(true).noticeDays(10).conditions(List.of(condition)).build();
		request.setRecycleRule(recycleRule);
		this.requestPostWithOk("/clue-pool/add", request);
	}

	@Test
	@Order(3)
	void page() throws Exception {
		BasePageRequest request = createPageRequest();
		request.setSort(new SortRequest("name", "desc"));
		MvcResult mvcResult = this.requestPostWithOkAndReturn("/clue-pool/page", request);
		Pager<List<CluePoolDTO>> result = getPageResult(mvcResult, CluePoolDTO.class);
		assert result.getList().size() == 1;
		testCluePool = result.getList().getFirst();
	}

	@Test
	@Order(4)
	void update() throws Exception {
		CluePool cluePool = createCluePool();
		cluePool.setId(testCluePool.getId());
		CluePoolUpdateRequest request = new CluePoolUpdateRequest();
		BeanUtils.copyBean(request, cluePool);
		request.setOwnerIds(List.of("cc"));
		request.setScopeIds(List.of("cc"));
		CluePoolPickRuleDTO pickRule = CluePoolPickRuleDTO.builder()
				.pickNumber(1).limitOnNumber(true).pickIntervalDays(1).limitPreOwner(true).build();
		request.setPickRule(pickRule);
		CluePoolRecycleRuleDTO recycleRule = CluePoolRecycleRuleDTO.builder().expireNotice(true).noticeDays(10).build();
		request.setRecycleRule(recycleRule);
		MvcResult mvcResult = this.requestPost("/clue-pool/update", request).andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("clue_pool_access_fail"));
		// update owner id by sql
		cluePool.setOwnerId(JSON.toJSONString(List.of("admin")));
		cluePoolMapper.updateById(cluePool);
		request.setOwnerIds(List.of("admin"));
		this.requestPostWithOk("/clue-pool/update", request);
	}

	@Test
	@Order(5)
	void switchStatus() throws Exception {
		MvcResult mvcResult = this.requestGet("/clue-pool/switch/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("clue_pool_not_exist"));
		this.requestGetWithOk("/clue-pool/switch/" + testCluePool.getId());
	}

	@Test
	@Order(6)
	void delete() throws Exception {
		MvcResult mvcResult = this.requestGet("/clue-pool/delete/default-pool").andExpect(status().is5xxServerError()).andReturn();
		assert mvcResult.getResponse().getContentAsString().contains(Translator.get("clue_pool_not_exist"));
		// insert free clue on the pool, then delete it
		CluePoolRelation cluePoolRelation = createCluePoolRelation();
		cluePoolRelation.setPoolId(testCluePool.getId());
		cluePoolRelationMapper.insert(cluePoolRelation);
		this.requestGet("/clue-pool/check-pick/" + testCluePool.getId());
		// pick clue, delete the pool
		cluePoolRelationMapper.deleteByPrimaryKey(cluePoolRelation.getId());
		this.requestGetWithOk("/clue-pool/delete/" + testCluePool.getId());
	}

	private BasePageRequest createPageRequest() {
		BasePageRequest request = new BasePageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		return request;
	}

	private CluePool createCluePool() {
		CluePool cluePool = new CluePool();
		cluePool.setName("default-clue-pool");
		cluePool.setScopeId(JSON.toJSONString(List.of("default-dp")));
		cluePool.setOrganizationId(DEFAULT_ORGANIZATION_ID);
		cluePool.setEnable(true);
		cluePool.setAuto(true);
		return cluePool;
	}

	private CluePoolRelation createCluePoolRelation() {
		CluePoolRelation cluePoolRelation = new CluePoolRelation();
		cluePoolRelation.setId("default-clue-pool-relation");
		cluePoolRelation.setClueId("default-clue");
		cluePoolRelation.setPicked(false);
		cluePoolRelation.setLastPickUserId("default-user");
		cluePoolRelation.setLastPickTime(System.currentTimeMillis());
		cluePoolRelation.setCreateTime(System.currentTimeMillis());
		cluePoolRelation.setCreateUser("default-user");
		cluePoolRelation.setUpdateTime(System.currentTimeMillis());
		cluePoolRelation.setUpdateUser("default-user");
		return cluePoolRelation;
	}
}
